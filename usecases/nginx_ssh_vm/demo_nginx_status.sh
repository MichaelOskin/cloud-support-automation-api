#!/bin/bash
# Демонстрация SSH-запроса для проверки статуса Nginx на тестовой VM
# Запускать из директории usecases/nginx_ssh_vm/

# Требует:
# 1. Основное приложение (Cloud Support Automation API) запущено (docker-compose up -d app из корня проекта).
# 2. Тестовая VM с Nginx запущена (docker-compose up -d --build из usecases/).
# 3. Для красивого вывода JSON требуется 'jq' (sudo apt install jq / brew install jq).

echo "--- Демонстрация проверки статуса Nginx на тестовой VM через SSH ---"

# 0. Убедимся, что Nginx запущен на тестовой VM
echo "0. Отправка запроса на запуск Nginx на тестовой VM, если он не запущен..."
START_NGINX_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/tasks \
-H "Content-Type: application/json" \
-d '{
  "type": "ssh_command",
  "userId": 1,
  "parameters": {
    "host": "host.docker.internal",
    "port": 2222,
    "username": "root",
    "password": "root",
    "command": "service nginx start"
  }
}')

START_NGINX_TASK_ID=$(echo "$START_NGINX_RESPONSE" | jq -r '.id')
echo "Задача запуска Nginx создана. ID: $START_NGINX_TASK_ID. Ожидание 3 сек..."
sleep 3
START_NGINX_STATUS=$(curl -s http://localhost:8080/api/v1/tasks/"$START_NGINX_TASK_ID" | jq -r '.status')
if [ "$START_NGINX_STATUS" != "SUCCESS" ]; then
    echo "Ошибка: Не удалось запустить Nginx на тестовой VM. Статус задачи: $START_NGINX_STATUS"
    curl -s http://localhost:8080/api/v1/tasks/"$START_NGINX_TASK_ID" | jq .
    exit 1
fi
echo "Nginx успешно запущен на тестовой VM."

sleep 2 # Небольшая пауза после запуска Nginx

# 1. Отправка запроса на проверку статуса Nginx
echo "1. Отправка запроса на выполнение 'service nginx status' на тестовой VM"

RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/tasks \
-H "Content-Type: application/json" \
-d '{
  "type": "ssh_command",
  "userId": 1,
  "parameters": {
    "host": "host.docker.internal",
    "port": 2222,
    "username": "root",
    "password": "root",
    "command": "service nginx status"
  }
}')

TASK_ID=$(echo "$RESPONSE" | jq -r '.id')

if [ "$TASK_ID" == "null" ]; then
  echo "Ошибка: Не удалось создать задачу. Ответ API:"
  echo "$RESPONSE" | jq .
  exit 1
fi

echo "Задача успешно создана. ID задачи: $TASK_ID. Статус: PENDING"
echo "2. Ожидание завершения задачи (3 секунды)..."
sleep 3

echo "3. Получение финального статуса задачи $TASK_ID:"
curl -s http://localhost:8080/api/v1/tasks/"$TASK_ID" | jq .

echo "--- Демонстрация завершена ---"
