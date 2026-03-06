#!/bin/bash
# Демонстрация обработки ошибки аутентификации при выполнении SSH-команды

echo "1. Отправка запроса на выполнение 'whoami' на тестовом SSH-сервере с НЕВЕРНЫМ паролем"

RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/tasks \
-H "Content-Type: application/json" \
-d '{
  "type": "ssh_command",
  "userId": 1,
  "parameters": {
    "host": "host.docker.internal",
    "port": 2222,
    "username": "root",
    "password": "wrongpassword",
    "command": "whoami"
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

echo "3. Получение финального статуса задачи $TASK_ID (ожидаем FAILED с ошибкой аутентификации):"
curl -s http://localhost:8080/api/v1/tasks/"$TASK_ID" | jq .

echo "------"
