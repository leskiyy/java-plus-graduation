# Explore With Me — Платформа микросервисов

Этот репозиторий содержит пример платформы микросервисов, используемой в проекте "Explore With Me". Демонстрируется небольшая экосистема Spring Boot-сервисов с централизованной конфигурацией, сервис-дискавери, API-шлюзом, обработкой событий через Kafka и частью межсервисного взаимодействия по gRPC.

В этом README описаны модули инфраструктуры, сервисы и их зоны ответственности, конфигурации (используемые в config-server) и базовые шаги для сборки и запуска системы локально.

Содержание
- Структура проекта
- Описания сервисов
- API (маршрутизация через Gateway)
- Конфигурация (настройки config-server)
- Необходимая инфраструктура для локального запуска
- Порядок сборки и запуска
- Примечания и отладка

---

Структура проекта (infra)
Ключевые модули в `infra/`:

- `discovery-server` — сервер Eureka для регистрации сервисов.
  - Порт по умолчанию: `8761`
- `config-server` — Spring Cloud Config Server (читает YAML из classpath).
  - Использует профиль `native` и загружает конфиги из `classpath:config/{scope}/{application}`.
- `gateway-server` — Spring Cloud Gateway, использует сервис-дискавери и config-server.
- Дополнительные pom-файлы для мульти-модульной сборки.

Файлы конфигурации сервисов включены в:
- `infra/config-server/src/main/resources/config/core/...`
- `infra/config-server/src/main/resources/config/stats/...`
- `infra/config-server/src/main/resources/config/infra/...`

Эти YAML-файлы содержат значения по умолчанию для подключений и runtime-настроек.

---

Сервисы и их ответственность

Core-сервисы (в каталоге `core/`, конфигурации находятся в config-server):
- event-service
  - Основной сервис для событий (публичные эндпойнты и админ-операции).
  - JDBC datasource: `jdbc:postgresql://localhost:5433/core_event_service_db`
  - gRPC-клиенты: `analyzer` и `collector` (адреса через discovery).
- comment-service
  - Управление комментариями к событиям.
  - JDBC datasource: `jdbc:postgresql://localhost:5433/core_comment_service_db`
- request-service
  - Управление запросами на участие в событиях.
  - JDBC datasource: `jdbc:postgresql://localhost:5433/core_request_service_db`
  - gRPC-клиент для `collector`.
- user-service
  - Пользователи и административное управление пользователями.
  - JDBC datasource: `jdbc:postgresql://localhost:5433/core_user_service_db`

Сервисы статистики / аналитики (конфиги в `stats/`):
- stats-collector
  - Сбирает действия пользователей и публикует их в Kafka-топики.
  - Kafka bootstrap по умолчанию: `localhost:9092`
  - Запускает gRPC-сервер (порт задаётся динамически в конфиге `server.port: 0`)
  - Топик: `stats.user-actions.v1`
- stats-aggregator
  - Потребляет user-actions и генерирует агрегированные данные / пишет в Kafka.
  - Конфигурация consumer group в YAML.
  - Топики:
    - `stats.user-actions.v1`
    - `stats.events-similarity.v1`
- stats-analyzer
  - Поддерживает рекомендации / вычисление сходства событий.
  - JDBC datasource: `jdbc:postgresql://localhost:5433/recommendations_analyzer_db`
  - Потребляет топики и предоставляет gRPC-сервер для lookup-рекомендаций.
  - В конфиге задаются веса действий (like/register/view).

Примечание: `stat-service` используется в gateway для маршрутов `/hit/**` и `/stats/**`. Функциональность статистики может быть распределена между collector/aggregator/analyzer.

---

API (через Gateway)

Gateway конфигурируется через config-server и маршрутизирует HTTP-запросы в бекэнд-сервисы. Gateway использует discovery locator и URI вида `lb://{service-name}`. Определённые маршруты:

- Comment Service
  - Приватные комментарии к событиям: `/users/*/events/*/comments/**` -> `comment-service`
  - Приватные комментарии пользователя: `/users/*/comments/**` -> `comment-service`
  - Админ управление комментариями: `/admin/comments/**` -> `comment-service`

- Event Service
  - Приватные события пользователя: `/users/*/events/**` -> `event-service`
  - Админ категории: `/admin/categories/**` -> `event-service`
  - Админ события: `/admin/events/**` -> `event-service`
  - Админ подборки (compilations): `/admin/compilations/**` -> `event-service`
  - Публичные категории: `/categories/**` -> `event-service`
  - Публичные подборки: `/compilations/**` -> `event-service`
  - Публичные события: `/events/**` -> `event-service`

- Request Service
  - Приватные запросы: `/users/*/requests/**` -> `request-service`

- User Service
  - Админ пользователи: `/admin/users/**` -> `user-service`

- Stats Service
  - Хиты и статистика: `/hit/**` и `/stats/**` -> `stat-service`

Примеры:
- Получить публичные события: `GET http://localhost:{gateway-port}/events`
- Создать категорию (админ): `POST http://localhost:{gateway-port}/admin/categories`
- Создать комментарий к событию: `POST http://localhost:{gateway-port}/users/{userId}/events/{eventId}/comments`

Замечание: многие сервисы используют `server.port: 0` в конфигурациях — это заставляет сервис регистрировать динамический порт в Eureka, а gateway маршрутизирует по имени сервиса.

---

Конфигурации по умолчанию (из config-server)

Важные значения из YAML-файлов:

- Базы данных (Postgres на localhost:5433)
  - core_event_service_db
  - core_comment_service_db
  - core_request_service_db
  - core_user_service_db
  - recommendations_analyzer_db (для stats-analyzer)
  - Логин/пароль в YAML: username `postgres`, password `postgres`

- Kafka
  - bootstrap-server: `localhost:9092`
  - топики:
    - `stats.user-actions.v1`
    - `stats.events-similarity.v1`

- gRPC
  - gRPC-клиенты используют адреса вида `discovery:///analyzer` и `discovery:///collector`
  - Некоторые stats-сервисы запускают gRPC-серверы с `port: 0` (динамически)

- Сервис-дискавери
  - Eureka server по умолчанию: `http://localhost:8761/eureka/` (discovery-server)

- Поисковые локации config-server (native / classpath)
  - `classpath:config/core/{application}`
  - `classpath:config/stats/{application}`
  - `classpath:config/infra/{application}`
