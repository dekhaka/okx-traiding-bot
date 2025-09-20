package strategy

import modul.Candle
import utils.MarketPhase

class ReversalShortStrategy(override val type: StrategyType) : TradeStrategy {
    override val name: String = "Reversal Short"
    override val allowedPhase = listOf(MarketPhase.FLAT, MarketPhase.DOWN_TREND)

    override fun direction(): TradeDirection = TradeDirection.SHORT

    override fun shouldEnterTrade(
        candles: List<Candle>,
        rsi: Double?,
        ema200: Double?,
        atr: Double?,
        marketPhase: MarketPhase?
    ): Boolean {
        return checkReversalShort(candles, rsi)
    }
}