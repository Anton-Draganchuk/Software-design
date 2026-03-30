# Currency homework project

Проект состоит из двух Spring Boot сервисов и инфраструктуры для discovery, контрактного тестирования и наблюдаемости:

- `currency-rate-provider` - provider с JSON-RPC endpoint `/rpc`
- `rate-printer` - client, который периодически вызывает provider
- `ZooKeeper` - service discovery
- `Pact Broker` - хранение consumer-контрактов
- `Prometheus + Grafana` - сбор и визуализация метрик

## Что добавлено

- Spring Boot Actuator и Prometheus registry в оба приложения
- логирование входящего запроса и ответа на provider
- логирование исходящего запроса и ответа на client
- лог версии приложения при старте
- кастомные серверные метрики:
  - `rpc_server_requests_seconds_*` для RPS и времени обработки
  - `rpc_server_errors_total` для количества `500`
- готовый `Grafana` dashboard для `client`, `service1`, `service2` и `zookeeper`
- экспорт метрик ZooKeeper через `JMX Exporter`

## Метрики

На provider считаются метрики из задания:

- количество запросов в секунду с разбивкой по клиенту (`client` tag из header `X-Client-Name`)
- количество ответов `500`
- время обработки запроса:
  - среднее
  - медиана `p50`
  - `p95`

Все Spring-сервисы также публикуют стандартные JVM метрики Micrometer через `/actuator/prometheus`.

## Запуск полного стенда

Полный стенд поднимается одной командой:

```bash
docker compose up --build -d
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

## Actuator endpoints

У каждого Spring-сервиса включены:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

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

1. Поднимите только инфраструктуру:
   ```bash
   docker compose up -d zookeeper pact-broker-db pact-broker prometheus grafana zookeeper-jmx-exporter
   ```

2. Соберите проект:
   ```bash
   mvn -DskipTests package
   ```

3. Запустите provider instances:
   ```bash
   java -jar currency-rate-provider/target/currency-rate-provider-1.0.0.jar --spring.application.name=service1 --server.port=8080
   java -jar currency-rate-provider/target/currency-rate-provider-1.0.0.jar --spring.application.name=service2 --server.port=8082
   ```

4. Запустите client:
   ```bash
   java -jar rate-printer/target/rate-printer-1.0.0.jar --spring.application.name=client --server.port=8081
   ```

## Замечания по логированию

- provider логирует request/response для всех рабочих endpoint'ов, кроме `/actuator/**`
- client добавляет header `X-Client-Name`, чтобы серверные метрики можно было строить по вызывающему клиенту
- версия приложения логируется при старте через `build-info`
