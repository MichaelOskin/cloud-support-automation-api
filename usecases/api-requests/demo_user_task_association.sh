#!/bin/bash
# Демонстрация создания пользователя и привязки задачи к нему
# Требует запущенного основного приложения (Cloud Support Automation API) и сервиса test_ssh_server
# Для красивого вывода JSON требуется 'jq'

echo "--- Демонстрация создания пользователя и привязки задачи ---"

# 1. Создание пользователя
echo "1. Создание тестового пользователя..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/users \
-H "Content-Type: application/json" \
-d '{
  "name": "Demo User",
  "email": "demo.user.new@example.com",
  "role": "CUSTOMER"
}')

USER_ID=$(echo "$RESPONSE" | jq -r '.id')

if [ "$USER_ID" == "null" ]; then
  echo "Ошибка: Не удалось создать пользователя. Ответ API:"
  echo "$RESPONSE" | jq .
  exit 1
fi

echo "Пользователь 'Demo User' успешно создан. ID пользователя: $USER_ID"

# 2. Создание задачи, ассоциированной с этим пользователем
echo "2. Отправка запроса на выполнение команды для пользователя ID: $USER_ID"

TASK_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/tasks \
-H "Content-Type: application/json" \
-d '{
  "type": "ssh_command",
  "userId": '$USER_ID',
  "parameters": {
    "host": "host.docker.internal",
    "port": 2222,
    "username": "root",
    "password": "root",
    "command": "echo \"Task for user '$USER_ID' executed!\""
  }
}')

TASK_ID=$(echo "$TASK_RESPONSE" | jq -r '.id')

if [ "$TASK_ID" == "null" ]; then
  echo "Ошибка: Не удалось создать задачу. Ответ API:"
  echo "$TASK_RESPONSE" | jq .
  exit 1
fi

echo "Задача успешно создана. ID задачи: $TASK_ID. Статус: PENDING"
echo "3. Ожидание завершения задачи (3 секунды)..."
sleep 3

# 4. Получение финального статуса задачи
echo "4. Получение финального статуса задачи $TASK_ID (ожидаем SUCCESS и 'userId: '$USER_ID'):"
curl -s http://localhost:8080/api/v1/tasks/"$TASK_ID" | jq .

echo "--- Демонстрация завершена ---"
