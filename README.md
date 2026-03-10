# Currency homework project

Проект состоит из двух сервисов:
- `currency-rate-provider` — provider (JSON-RPC endpoint `/rpc`)
- `rate-printer` — consumer (периодически запрашивает курс и печатает его)

Инфраструктура:
- `ZooKeeper` для service discovery;
- `Pact Broker` для хранения consumer-контрактов.

## Что реализовано

- В `rate-printer` добавлен consumer contract test на Pact (`RateProviderConsumerPactTest`).
- В `currency-rate-provider` добавлен provider verification test на Pact (`RateProviderPactVerificationTest`).
- При сборке `currency-rate-provider` test-фаза берёт контракты из `Pact Broker` и проверяет API provider.
- Добавлен `docker-compose.yml` для запуска `ZooKeeper + Pact Broker + PostgreSQL`.

## Запуск инфраструктуры

```bash
docker compose up -d
```

Сервисы будут доступны:
- ZooKeeper: `localhost:2181`
- Pact Broker: `http://localhost:9292`

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

## Запуск сервисов

1. Собрать проект:
   ```bash
   mvn -DskipTests package
   ```

2. Запустить 2 экземпляра provider на разных портах:
   ```bash
   java -jar currency-rate-provider/target/currency-rate-provider-1.0.0.jar --server.port=8080
   java -jar currency-rate-provider/target/currency-rate-provider-1.0.0.jar --server.port=8082
   ```

3. Запустить consumer:
   ```bash
   java -jar rate-printer/target/rate-printer-1.0.0.jar
   ```
