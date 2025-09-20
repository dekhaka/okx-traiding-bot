
# OKX Trading Bot (Demo)

Этот проект демонстрирует мои навыки разработки на Kotlin.

## Возможности
- Подключение к OKX через WebSocket
- Обработка рыночных данных (RSI, EMA, ATR)
- Расчёт размера позиции
- Уведомления в Telegram
- Логирование сделок

## Стек технологий
- Kotlin
- Ktor
- Coroutines
- Jackson (JSON)
- OKX WebSocket API
- Telegram Bot API
- Google Sheets API (опционально)

## Как запустить
1. Установите JDK 17 и Gradle.
2. Создайте файл .env с параметрами:


OKX_API_KEY=ваш_ключ
OKX_SECRET=ваш_секрет
TELEGRAM_TOKEN=токен
TELEGRAM_CHAT_ID=чат_id
