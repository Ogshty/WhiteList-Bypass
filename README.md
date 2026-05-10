# 🛡️ WL Bypass

[![Android](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Xray](https://img.shields.io/badge/Core-Xray--core-red?style=for-the-badge&logo=xray)](https://github.com/XTLS/Xray-core)

**WL Bypass** — это современное Android-приложение для управления VPN-соединениями с упором на гибкость, скорость и эстетику. Построенное на базе мощного **Xray-core**, оно обеспечивает надежный обход ограничений и безопасность ваших данных.

---

## ✨ Ключевые особенности

- 🚀 **Высокая производительность**: Использование `Xray-core` (Sing-box) для максимальной скорости и стабильности.
- 🎨 **Premium UI**: Полностью нативный интерфейс на Jetpack Compose с динамическими градиентами, плавными анимациями и поддержкой Material You.
- 📝 **Белый список (Whitelist)**: Выбирайте конкретные приложения, которые должны работать через VPN, оставляя остальные в локальной сети.
- 🔄 **Умные подписки**: Поддержка автоматического обновления конфигураций через ссылки (GitHub Gists, репозитории).
- ⚡ **Низкая задержка**: Встроенная проверка пинга (latency check) для выбора самого быстрого сервера.
- 🛡️ **Безопасность**: Функции Kill Switch и автоматическое переподключение для защиты вашего трафика 24/7.
- 📊 **Логирование**: Подробный мониторинг состояния соединения в реальном времени.

---

## 🛠 Технологический стек

- **Язык**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Архитектура**: MVVM + Clean Architecture
- **VPN Core**: [libbox](https://github.com/SagerNet/sing-box) / Xray-core
- **Фоновые задачи**: WorkManager
- **Хранение данных**: DataStore Preferences
- **Сеть**: OkHttp 4.x

---

## 📸 Скриншоты

<p align="center">
  <img src="https://via.placeholder.com/300x600?text=Main+Screen" width="30%" />
  <img src="https://via.placeholder.com/300x600?text=Whitelist" width="30%" />
  <img src="https://via.placeholder.com/300x600?text=Settings" width="30%" />
</p>

---

## 🚀 Начало работы

### Требования
- Android 8.0 (API 26) и выше.
- Android Studio Iguana или новее.

### Сборка проекта
1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/Ogshty/WhiteList-Bypass.git
   ```
2. Откройте проект в Android Studio.
3. Дождитесь завершения Gradle Sync.
4. Соберите и запустите приложение на вашем устройстве.

---

## 📄 Лицензия

Этот проект распространяется под лицензией MIT. Подробности в файле [LICENSE](LICENSE).

---

## 🤝 Контакты

Если у вас есть вопросы или предложения, создавайте **Issue** или свяжитесь с разработчиком.

Разработано с ❤️ от [Ogshty](https://github.com/Ogshty)
