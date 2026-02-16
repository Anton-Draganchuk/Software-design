# Currency homework project

Проект состоит из двух сервисов:
- `currency-rate-provider` — producer (JSON-RPC endpoint `/rpc`)
- `rate-printer` — consumer (периодически запрашивает курс и печатает его)

В проект добавлен `ZooKeeper` как service registry:
- каждый экземпляр `currency-rate-provider` при старте автоматически регистрируется в `ZooKeeper`;
- `rate-printer` автоматически отслеживает список экземпляров и делает `round-robin` балансировку между ними.

## Запуск

1. Запустить ZooKeeper на `localhost:2181`.
2. Собрать проект:
   ```bash
   mvn -DskipTests package
   ```
3. Запустить 2 экземпляра provider на разных портах:
   ```bash
   java -jar currency-rate-provider/target/currency-rate-provider-1.0.0.jar --server.port=8080
   java -jar currency-rate-provider/target/currency-rate-provider-1.0.0.jar --server.port=8082
   ```
4. Запустить consumer:
   ```bash
   java -jar rate-printer/target/rate-printer-1.0.0.jar
   ```

В логах `rate-printer` будет видно, что запросы идут к разным provider-инстансам.
