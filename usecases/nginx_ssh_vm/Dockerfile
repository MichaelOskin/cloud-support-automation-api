# usecases/nginx_ssh_vm/Dockerfile
# Базовый образ: простой SSH-сервер на Ubuntu
FROM rastasheep/ubuntu-sshd

# Устанавливаем Nginx и необходимые утилиты
# 'procps' для 'ps' команды, 'curl' для проверки http-доступа
RUN apt-get update && \
    apt-get install -y nginx curl procps && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Копируем базовую конфигурацию Nginx
# Nginx будет использовать эту конфигурацию, когда мы его запустим через SSH
COPY default.conf /etc/nginx/sites-available/default
RUN ln -sf /etc/nginx/sites-available/default /etc/nginx/sites-enabled/default

# Устанавливаем права на запуск Nginx без пароля для root, если SSH-пользователь не root
# В данном случае SSH-пользователь root, так что это не строго необходимо, но хорошая практика.
# USERMOD_ENABLE_SUDO=true в образе rastasheep/ubuntu-sshd уже делает пользователя root без пароля.
# Так что 'sudo' должен работать.
