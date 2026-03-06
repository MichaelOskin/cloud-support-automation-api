# Cloud Support Automation API

Платформа для автоматизации рутинных операций в облачной инфраструктуре. API может подойти для асинхронного 
выполнения задач, например в облачной среде AWS.
Что поддерживает:
1. Выполнение команд по SSH: Автоматизация задач администрирования на удаленных Linux-серверах 
   с поддержкой аутентификации по паролю и по приватному ключу.
2. Управление EC2-инстансами: Программное создание и управление ресурсами в AWS.
3. Поддержка аудита: Просмотр по времени | Задача | Параметр | Статус |

## Примеры сценариев

### Сценарий: Диагностика веб-сервера по SSH

У клиента не работает сайт. Инженер вручную connect to ssh -> `service nginx status` -> copy/paste и заполнение тикета вручную.
Решение: Инженер отправляет один API-запрос.

```bash
curl -X POST "http://localhost:8080/api/v1/tasks" 
-H "Content-Type: application/json" 
-d '{
  "type": "ssh_command",
  "userId": 1,
  "parameters": {
    "host": "IP_ТЕСТОВОГО_СЕРВЕРА",
    "port": 2222,
    "username": "root",
    "password": "root",
    "command": "service nginx status"
  }
}'
```
Результат
```json
{
  "id": 16,
  "type": "ssh_command",
  "status": "SUCCESS",
  "result": {
    "stderr": "",
    "stdout": "* nginx is running",
    "exitCode": 0
  },
  "errorMessage": null,
  "createdAt": "2026-03-05 19:50:47",
  "userId": 1
}
```

### Сценарий: Создание EC2-инстанса "по запросу"
Разработчик создал заявку на получение aws и в настоящий момент в режиме ожидания.
Решение через запрос API

```bash
curl -X POST "http://localhost:8080/api/v1/tasks" 
-H "Content-Type: application/json" 
-d '{
  "type": "create_ec2",
  "userId": 1,
  "parameters": {
    "imageId": "ami-0d5323a080e2e5b40",
    "instanceType": "t3.micro",
    "keyName": "my-demo-key",
    "securityGroupIds": ["sg-032d70dd5735cfb8c"],
    "subnetId": "subnet-09da52aeb4a1d5985"
  }
}'
```

Результат
```json
{
  "id": 25,
  "type": "create_ec2",
  "status": "SUCCESS",
  "result": {
    "publicIp": "13.53.36.217",
    "privateIp": "172.31.28.212",
    "instanceId": "i-04b32db6b2bb92ab8"
  },
  "errorMessage": null,
  "createdAt": "2026-03-06 04:06:08",
  "userId": 1
}
```
---

## 3. Установка и запуск

### Шаг 1: Конфигурация окружения

```shell
copy .env.example .env
```

Заполните его по следующему шаблону, подставив ваши данные, полученные из консоли AWS:
```env
# --- AWS Credentials and Region ---
# Ключи доступа для IAM-пользователя с правами на управление EC2
export AWS_ACCESS_KEY_ID="AKIA..."
export AWS_SECRET_ACCESS_KEY="ВАШ_СЕКРЕТНЫЙ_КЛЮЧ"
export AWS_REGION="eu-north-1" # Ваш регион AWS

# --- EC2 Parameters ---
# Параметры для создания EC2-инстанса
export EC2_KEY_NAME="my-demo-key"
export EC2_SECURITY_GROUP_IDS="sg-..."
export EC2_SUBNET_ID="subnet-..."
export EC2_AMI_ID="ami-..."

# --- SSH Private Key ---
# Содержимое .pem файла для подключения к создаваемому EC2
export AWS_PRIVATE_KEY_CONTENT="-----BEGIN RSA PRIVATE KEY-----
...
-----END RSA PRIVATE KEY-----"
```

Настроить под нагрузку системы
```properties
# Эти свойства конфигурируют пользовательский пул потоков для @Async задач.
# Эти значения должны быть настроены на основе ресурсов production-среды и ожидаемой нагрузки.
spring.task.execution.pool.core-size=10
spring.task.execution.pool.max-size=20
spring.task.execution.pool.queue-capacity=50
spring.task.execution.thread-name-prefix=Prod-AsyncTask-
```

### Шаг 2: Запуск

Для полного запуска проекта (основное приложение + демонстрационная среда) выполните следующие команды:

1.  **Запустите основное приложение и базу данных:**
    *(Из корневой директории проекта)*
    ```bash
    docker-compose up --build -d
    ```

### Демонстрация
Пример использования API тут : `usecases/api-requests/DEMONSTRATION.md`

Пример nginx :
```shell
cd usecases
# if needed
docker compose up --build -d

# настройка переменных окружения
source .env
chmod +x
./usecases/nginx_ssh_vm/demo_nginx_status.sh
```
Подробнее в `usecases/.md`.

---

## 4. Документация 
Посмотреть UI документацию 
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---
Диаграмма взаимодействия

```mermaid
sequenceDiagram
    participant Client as Клиент (оператор/скрипт)
    participant API as REST Controller
    participant DB as База данных (PostgreSQL)
    participant Executor as Async Task Executor
    participant AWS as AWS EC2 (внешний сервис)
    participant SSH as SSH Server (удалённый хост)

    Client->>API: POST /tasks (create_ec2)
    API->>DB: Сохранить задачу (PENDING)
    API-->>Client: 201 Created (taskId)
    API->>Executor: Запустить обработку (taskId)

    Executor->>DB: Получить задачу
    Executor->>Executor: Обновить статус RUNNING
    Executor->>AWS: ec2.runInstances()
    AWS-->>Executor: instanceId, IP
    Executor->>DB: Обновить статус SUCCESS + результат
    Executor-->>API: (завершено)
```

Для SSH-команд аналогично, вместо AWS – JSch.

#### Описание компонентов (сервисов) системы:

- Клиент – внешний пользователь или автоматизированная система (например, скрипт, интеграция с тикет-системой), отправляющая HTTP-запросы к API.
- REST Controller – слой представления Spring MVC, принимающий запросы, валидирующий входные данные, создающий запись задачи в БД и инициирующий асинхронную обработку.
- БД Postgres – хранилище задач и аудита. Используется для сохранения параметров, статусов и результатов выполнения.
- Async Task Executor – компонент Spring (`@Async` с настроенным пулом потоков), отвечающий за фоновое выполнение задач. 
  Он извлекает задачу из БД, обновляет статус, вызывает соответствующий процессор и сохраняет результат.
- AWS EC2 (внешний сервис) – облачный API AWS, используемый для создания виртуальных машин.
- SSH Server - Linux-сервер, на котором выполняется команда. Доступ через библиотеку JSch.