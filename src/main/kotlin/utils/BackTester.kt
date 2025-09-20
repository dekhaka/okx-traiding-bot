package utils

import modul.Candle
import notifier.BotConfig
import stats.AccountBalance
import stats.TradeResult
import stats.TradeStats
import strategy.Position
import strategy.StrategyType
import modul.calculateRSI
import notifier.TelegramNotifier
import notifier.closePosition
import stats.RiskManager
import stats.calculateATR
import stats.isHighVolume
import stats.isVolatilityHigh
import strategy.StrategyManager
import strategy.TradeDirection
import strategy.openPosition
import utils.Indicators.calculateEMA
import utils.Indicators.detectMarketPhase
import java.io.File


suspend fun runBacktest(
    strategyManager: StrategyManager,
    tradeStats: TradeStats,
    riskManager: RiskManager,
    accountBalance: AccountBalance,
    activePositions: MutableMap<String, MutableMap<StrategyType, Position>>,
    closedTrades: MutableList<TradeResult>,
    candleBuffers: MutableMap<String, MutableList<Candle>>,
    config: BotConfig,
    telegramNotifier: TelegramNotifier
) {

    val symbol = config.symbols.firstOrNull() ?: run {
        println("⚠️ Символ не указан в конфиге")
        return
    }

    val useApi = false  // переключатель между API и CSV

    val historicalCandles = if (useApi) {
        val startTime = 1719100000000
        val endTime = 1719704800000
        fetchHistoricalCandles(
            instrumentId = symbol,
            startTime = startTime,
            endTime = endTime
        )
    } else {
        loadCandlesFromCsv("data/$symbol.csv")
    }
    val equityCurve = mutableListOf<Pair<Long, Double>>()
    val buffer = candleBuffers.getOrPut(symbol) { mutableListOf() }

    for (candle in historicalCandles) {
        buffer.add(candle)
        if (buffer.size > 200) buffer.removeAt(0)

        val rsi = calculateRSI(buffer)
        val atr = calculateATR(buffer)
        val ema200 = calculateEMA(buffer, 50)
        val marketPhase = detectMarketPhase(buffer)

        val selectedStrategy = strategyManager.selectStrategy(buffer, rsi, atr, ema200, marketPhase)

        val priceAboveEma = ema200?.let { candle.close > it } ?: true
        val priceBelowEma = ema200?.let { candle.close < it } ?: true

        if (
            selectedStrategy != null &&
            isHighVolume(buffer) &&
            isVolatilityHigh(buffer) &&
            (
                    (selectedStrategy.direction() == TradeDirection.LONG && priceAboveEma) ||
                            (selectedStrategy.direction() == TradeDirection.SHORT && priceBelowEma)
                    )
        ) {
            val strategyMap = activePositions.getOrPut(symbol) { mutableMapOf() }
            if (strategyMap.containsKey(selectedStrategy.type)) continue

            val stopLossPercent = 0.01
            val riskPercent = riskManager.getRiskPercent()
            val volume = accountBalance.calculateTradeSize(candle.close, stopLossPercent, riskPercent)
            val cost = volume * candle.close

            if (!accountBalance.canEnter(cost)) continue

            val stopLoss = if (selectedStrategy.direction() == TradeDirection.LONG)
                candle.close * 0.98 else candle.close * 1.02
            val takeProfit = if (selectedStrategy.direction() == TradeDirection.LONG)
                candle.close * 1.04 else candle.close * 0.96

            val position = openPosition(
                instrumentId = symbol,
                entryPrice = candle.close,
                direction = selectedStrategy.direction(),
                strategyType = selectedStrategy.type,
                timestamp = candle.timestamp,
                volume = volume,
                stopLoss = stopLoss,
                takeProfit = takeProfit
            )


            if (selectedStrategy.supportsTrailingStop && atr != null) {
                position.dynamicStopLoss = when (selectedStrategy.direction()) {
                    TradeDirection.LONG -> candle.close - atr * config.atrTrailingFactor
                    TradeDirection.SHORT -> candle.close + atr * config.atrTrailingFactor
                }
            }

            strategyMap[selectedStrategy.type] = position
        }

        // 📉 Проверка на закрытие
        val toClose = activePositions[symbol]?.filterValues { position ->
            (position.direction == TradeDirection.LONG &&
                    (candle.low <= position.stopLoss || candle.high >= position.takeProfit)) ||
                    (position.direction == TradeDirection.SHORT &&
                            (candle.high >= position.stopLoss || candle.low <= position.takeProfit))
        } ?: emptyMap()

        toClose.forEach { (strategyType, position) ->
            val closePrice = when {
                position.direction == TradeDirection.LONG && candle.low <= position.stopLoss -> position.stopLoss
                position.direction == TradeDirection.SHORT && candle.high >= position.stopLoss -> position.stopLoss
                else -> position.takeProfit
            }

            val profit = closePosition(
                symbol,
                position,
                closePrice,
                accountBalance,
                closedTrades,
                tradeStats,
                telegramNotifier
            )

            activePositions[symbol]?.remove(strategyType)
        }

        // 📈 Добавление баланса в equity curve
        equityCurve.add(candle.timestamp to accountBalance.balance)
    }

    // 💾 Сохраняем эквитикривую
    File("equity_curve.csv").printWriter().use { out ->
        out.println("timestamp,balance")
        equityCurve.forEach { (ts, bal) ->
            out.println("$ts,$bal")
        }
    }

    // 📊 Выводим сводную статистику
    println("📊 Результаты бэктеста:")
    println(tradeStats.summary())
}