# Currency homework project

Проект состоит из двух Spring Boot сервисов и инфраструктуры для discovery, контрактного тестирования и наблюдаемости:

- `currency-rate-provider` - provider с JSON-RPC endpoint `/rpc`
- `rate-printer` - client, который периодически вызывает provider
- `ZooKeeper` - service discovery
- `Pact Broker` - хранение consumer-контрактов
- `Prometheus + Grafana` - сбор и визуализация метрик

## Что реализовано

- Actuator и Prometheus registry в обоих приложениях
- логирование request/response на provider и client только в `stdout`
- лог версии приложения при старте
- кастомные серверные метрики:
  - `rpc_server_requests_seconds_*`
  - `rpc_server_errors_total`
- Grafana dashboard для `client`, `service1`, `service2` и `zookeeper`
- ZooKeeper metrics через `JMX Exporter`
- разделение `build -> release -> run`
- `dev` и `prod` parity через одни и те же артефакты и разные env-файлы
- graceful shutdown для Spring-сервисов и Docker-контейнеров

## 12-factor изменения

### V. Build, release, run

Стадии разделены:

- `scripts/build.sh` - собирает jar-артефакты
- `scripts/release.sh <env-file>` - собирает runtime-образы из уже готовых jar
- `scripts/run.sh <env-file>` - запускает готовый release

Dockerfile больше не компилирует исходники. Он только упаковывает уже собранный jar в runtime-образ.

### IX. Disposability

- включён `server.shutdown=graceful`
- настроен `spring.lifecycle.timeout-per-shutdown-phase=20s`
- для `rate-printer` включено ожидание завершения scheduler-задач при остановке
- в `docker-compose.yml` добавлен `stop_grace_period: 20s`

### X. Dev/prod parity

- один и тот же jar и один и тот же Dockerfile используются и в `dev`, и в `prod`
- различия между окружениями вынесены в env-файлы:
  - `env/dev.env`
  - `env/prod.env`
- профиль Spring выбирается через `SPRING_PROFILES_ACTIVE`

### XI. Logs

- оба приложения логируют только в `stdout`
- файловых appender'ов нет
- для этого добавлены явные `logback-spring.xml`

## Build / Release / Run

### 1. Build

Сборка jar-артефактов:

```bash
./scripts/build.sh
```

Или вручную:

```bash
mvn -DskipTests clean package
```

### 2. Release

Сборка runtime-образов для выбранного окружения:

```bash
./scripts/release.sh env/dev.env
```

или

```bash
./scripts/release.sh env/prod.env
```

### 3. Run

Запуск уже собранного release:

```bash
./scripts/run.sh env/dev.env
```

или

```bash
./scripts/run.sh env/prod.env
```

Если нужен старый краткий сценарий, он тоже работает после build-шага:

```bash
docker compose --env-file env/dev.env up -d
```

## Конфигурация окружений

Шаблон переменных лежит в:

```bash
.env.example
```

Основные env-файлы:

- `env/dev.env`
- `env/prod.env`

Сейчас различие минимальное и контролируемое:

- `dev` использует профиль `dev`
- `prod` использует профиль `prod`

Остальные адреса, порты и runtime-параметры также настраиваются только через env.

## Метрики

На provider считаются метрики из задания:

- количество запросов в секунду с разбивкой по клиенту
- количество ответов `500`
- время обработки запроса:
  - среднее
  - медиана `p50`
  - `p95`

Все Spring-сервисы также публикуют стандартные JVM метрики через `/actuator/prometheus`.

## Actuator endpoints

У каждого Spring-сервиса включены:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

## Запуск полного стенда

Рекомендуемый сценарий:

```bash
./scripts/build.sh
./scripts/release.sh env/dev.env
./scripts/run.sh env/dev.env
```

После старта будут доступны:

- provider `service1`: `http://localhost:8080`
- provider `service2`: `http://localhost:8082`
- client actuator: `http://localhost:8081/actuator`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Pact Broker: `http://localhost:9292`
- ZooKeeper: `localhost:2181`

Логин Grafana по умолчанию: `admin/admin`.

## Grafana dashboard

Dashboard provisioning уже настроен. После запуска откройте:

`Grafana -> Dashboards -> Currency Homework -> Currency Homework Observability`

На дашборде есть панели для:

- доступности всех monitored targets
- RPS provider по клиентам
- количества `500`
- среднего времени ответа
- `p50` и `p95`
- JVM heap и threads для `client`, `service1`, `service2`
- базовых ZooKeeper и ZooKeeper JVM метрик

## Контрактный цикл

1. Сгенерировать consumer pact:
   ```bash
   mvn -pl rate-printer test
   ```

2. Опубликовать pact в broker:
   ```bash
   ./scripts/publish-rate-printer-pact.sh
   ```

3. Проверить provider по контрактам из broker:
   ```bash
   mvn -pl currency-rate-provider test
   ```

## Локальный запуск без Docker

1. Поднимите инфраструктуру:
   ```bash
   docker compose --env-file env/dev.env up -d zookeeper pact-broker-db pact-broker prometheus grafana zookeeper-jmx-exporter
   ```

2. Соберите jar:
   ```bash
   ./scripts/build.sh
   ```

3. Запустите provider instances:
   ```bash
   java -jar currency-rate-provider/target/currency-rate-provider-1.0.0.jar --spring.profiles.active=dev --spring.application.name=service1 --server.port=8080
   java -jar currency-rate-provider/target/currency-rate-provider-1.0.0.jar --spring.profiles.active=dev --spring.application.name=service2 --server.port=8082
   ```

4. Запустите client:
   ```bash
   java -jar rate-printer/target/rate-printer-1.0.0.jar --spring.profiles.active=dev --spring.application.name=client --server.port=8081
   ```
