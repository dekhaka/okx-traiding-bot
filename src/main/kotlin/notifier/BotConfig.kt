package notifier

import kotlinx.serialization.Serializable
import modul.json
import java.io.File

@Serializable
data class Config(
    val symbols: List<String>,
    val riskPerTrade: Double,
    val dailyLossLimitPercent: Double,
    val commissionRate: Double,
    val leverage: Double,
    val atrFactorSL: Double,
    val atrFactorTP: Double,
    val strategies: List<String>,
    val atrTrailingFactor: Double = 1.0
)

fun loadConfig(path: String = "config.json"): Config {
    val jsonText = File(path).readText()
    return json.decodeFromString(Config.serializer(), jsonText)
}