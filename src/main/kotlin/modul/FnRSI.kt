package Modul

import java.time.Period

fun calculateRSI (candles: List<Candle>, period: Int = 14): Double? {
    if (candles.size < period + 1) return null

    var gain = 0.0
    var loss = 0.0

    for (i in 1..period){
        val change = candles[i].close - candles[i-1].close
        if (change > 0) gain += change else loss -= change
    }

    val avgGain = gain / period
    val avgLoss = loss / period

    if (avgLoss == 0.0) return 100.0

    val rs = avgGain/avgLoss
    return 100 - (100/(1+rs))
}