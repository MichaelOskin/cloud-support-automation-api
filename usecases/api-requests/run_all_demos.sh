#!/bin/bash
# Оркестратор для последовательного запуска всех демонстрационных скриптов.
# Требует запущенного основного приложения (Cloud Support Automation API)
# и демонстрационных сервисов (test_ssh_server) из usecases/docker-compose.yml.
# Запускать из директории usecases/

# Функция для запуска одной демонстрации
run_demo() {
    local demo_name="$1"
    local script_path="$2"
    echo -e "
=== Демо: $demo_name ==="
    "$script_path"
    if [ $? -ne 0 ]; then
        echo "Ошибка в '$script_path'. Прерывание."
        exit 1
    fi
    sleep 5
}

echo "--- Запуск всех демонстрационных сценариев Cloud Support Automation API ---"
echo "Убедитесь, что основное приложение и демонстрационные сервисы запущены."
echo "----------------------------------------------------------------------"
sleep 2

run_demo "Успешное выполнение SSH-команды" "./scripts/demo_ssh_success.sh"
run_demo "Ошибка аутентификации SSH" "./scripts/demo_ssh_auth_fail.sh"
run_demo "Ошибка выполнения команды SSH" "./scripts/demo_ssh_command_fail.sh"
run_demo "Создание пользователя и привязка задачи" "./scripts/demo_user_task_association.sh"

echo -e "
--- Все демонстрационные сценарии успешно завершены! ---"
