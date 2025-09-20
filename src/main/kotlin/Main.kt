
import Modul.handleCandlestickMessage
import Modul.sendCandleSubscription
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = HttpClient(CIO) {
        install(WebSockets)
    }

    val instruments = listOf("BTC-USDT")

    client.webSocket("wss://ws.okx.com:8443/ws/v5/business") {
        // Отправляем подписку на свечи
        sendCandleSubscription(instruments)

        println("🌐 Подключение установлено. Ожидаем данные...")

        for (frame in incoming) {
            val text = (frame as? Frame.Text)?.readText() ?: continue
            println("📥 Получено сообщение:\n$text") // Для отладки
            try {
                handleCandlestickMessage(text)
            } catch (e: Exception) {
                println("❗ Ошибка обработки: ${e.message}")
            }
        }
    }
}
