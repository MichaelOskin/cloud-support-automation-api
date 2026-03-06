package org.cloud.automation.api.processor;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloud.automation.api.domain.AuditEvent;
import org.cloud.automation.api.domain.Task;
import org.cloud.automation.api.domain.TaskStatus;
import org.cloud.automation.api.repository.TaskRepository;
import org.cloud.automation.api.service.AuditService;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Обработчик для задач типа "ssh_command".
 * <p>
 * Этот класс реализует логику подключения к удаленному хосту по SSH,
 * выполнения переданной команды и сохранения результата.
 * Он автоматически обнаруживается и регистрируется в {@link org.cloud.automation.api.service.TaskProcessorRegistry}.
 * <p>
 * <b>Требуемые параметры в {@link Task#getParameters()}:</b>
 * <ul>
 *   <li>{@code host} (String) - IP-адрес или доменное имя хоста.</li>
 *   <li>{@code port} (Integer, опционально, по умолчанию 22) - Порт SSH.</li>
 *   <li>{@code username} (String) - Имя пользователя для подключения.</li>
 *   <li>{@code password} (String, опционально) - Пароль для аутентификации. <b>Внимание:</b> не используйте в продакшене, замените на ключи.</li>
 *   <li>{@code privateKey} (String, опционально) - Содержимое приватного SSH-ключа (формат PEM).</li>
 *   <li>{@code command} (String) - Команда для выполнения.</li>
 * </ul>
 * <p>
 * <b>Формат результата в {@link Task#getResult()}:</b>
 * <ul>
 *   <li>{@code exitCode} (Integer) - Код завершения выполненной команды.</li>
 *   <li>{@code stdout} (String) - Стандартный вывод (stdout) команды.</li>
 *   <li>{@code stderr} (String) - Стандартный вывод ошибок (stderr) команды.</li>
 * </ul>
 * <p>
 * <b>Пример использования (клиентский код на Java):</b>
 * <pre>{@code
 * RestTemplate restTemplate = new RestTemplate();
 * String apiUrl = "http://localhost:8080/api/v1/tasks";
 *
 * Map<String, Object> sshParams = new HashMap<>();
 * sshParams.put("host", "192.168.1.100");
 * sshParams.put("username", "user");
 * sshParams.put("password", "secret");
 * sshParams.put("command", "ls -la /var/log");
 *
 * CreateTaskRequest request = CreateTaskRequest.builder()
 *         .type("ssh_command")
 *         .parameters(sshParams)
 *         .build();
 *
 * try {
 *     TaskDto responseDto = restTemplate.postForObject(apiUrl, request, TaskDto.class);
 *     System.out.println("Task created with ID: " + responseDto.getId());
 * } catch (HttpClientErrorException e) {
 *     System.err.println("Error creating task: " + e.getResponseBodyAsString());
 * }
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshCommandProcessor implements TaskProcessor {

    private static final String TYPE = "ssh_command";

    private final TaskRepository taskRepository;
    private final AuditService auditService;

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Выполняет задачу по выполнению SSH-команды.
     * <p>
     * Метод извлекает параметры из задачи, устанавливает SSH-соединение с помощью библиотеки JSch,
     * выполняет команду, собирает stdout, stderr и код выхода.
     * По завершении (успешном или неуспешном), он обновляет статус и результат задачи в базе данных.
     *
     * @param task Задача для выполнения.
     */
    @Override
    public void execute(Task task) {
        log.info("Executing SSH command for Task ID: {}", task.getId());

        Map<String, Object> params = task.getParameters();
        String host = getParam(params, "host");
        int port = Integer.parseInt(getParam(params, "port", "22"));
        String username = getParam(params, "username");
        String password = (String) params.get("password"); // Note: In a real app, use encrypted secrets
        String privateKey = (String) params.get("privateKey"); // New: Private key content
        String command = getParam(params, "command");

        Session session = null;
        ChannelExec channel = null;

        try {
            JSch jsch = new JSch();

            if (privateKey != null && !privateKey.isEmpty()) {
                // Add the private key for authentication. JSch will automatically try it.
                // The 'null' for passphrase assumes the key is not password-protected.
                jsch.addIdentity("dynamic_identity", privateKey.replace("\\n", "\n").getBytes(StandardCharsets.UTF_8), null, null);
            }

            session = jsch.getSession(username, host, port);

            // Set password ONLY if no private key is provided AND password is provided
            // JSch will try identities first, then password if set.
            if ((privateKey == null || privateKey.isEmpty()) && password != null && !password.isEmpty()) {
                session.setPassword(password);
            }

            // ВАЖНО: Отключаем строгую проверку ключа хоста для автоматизации.
            // В реальном приложении здесь следует использовать файл `known_hosts` для предотвращения MitM-атак.
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000); // 30-секундный таймаут на подключение

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            channel.setInputStream(null);
            try (BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                 BufferedReader stderrReader = new BufferedReader(new InputStreamReader(channel.getErrStream()))) {

                channel.connect(10000); // 10-секундный таймаут на выполнение

                String line;
                while ((line = stdoutReader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
                while ((line = stderrReader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            }

            // Ждем завершения канала, чтобы получить код выхода
            while (!channel.isClosed()) {
                //noinspection BusyWait
                Thread.sleep(100);
            }

            int exitCode = channel.getExitStatus();
            log.info("Task {} finished with exit code: {}", task.getId(), exitCode);

            Map<String, Object> result = new HashMap<>();
            result.put("exitCode", exitCode);
            result.put("stdout", stdout.toString().trim());
            result.put("stderr", stderr.toString().trim());

            task.setResult(result);
            task.setStatus(exitCode == 0 ? TaskStatus.SUCCESS : TaskStatus.FAILED);
            if (exitCode != 0) {
                task.setErrorMessage("Command exited with non-zero status: " + exitCode);
            }

        } catch (Exception e) {
            log.error("Failed to execute SSH command for Task ID: {}", task.getId(), e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }

            task.setCompletedAt(LocalDateTime.now());
            Task savedTask = taskRepository.save(task);

            AuditEvent finalEvent = savedTask.getStatus() == TaskStatus.SUCCESS ? AuditEvent.COMPLETED : AuditEvent.FAILED;
            auditService.logEvent(savedTask, finalEvent, Map.of("finalStatus", savedTask.getStatus().toString()));

            log.info("Task {} completed with status: {}", task.getId(), task.getStatus());
        }
    }

    /**
     * Вспомогательный метод для безопасного извлечения обязательного параметра из карты.
     * @param params Карта параметров.
     * @param key Ключ параметра.
     * @return Значение параметра в виде строки.
     * @throws NullPointerException если параметр отсутствует.
     */
    private String getParam(Map<String, Object> params, String key) {
        return Objects.requireNonNull(params.get(key), "Parameter '" + key + "' is missing.").toString();
    }

    /**
     * Вспомогательный метод для безопасного извлечения необязательного параметра из карты.
     * @param params Карта параметров.
     * @param key Ключ параметра.
     * @param defaultValue Значение по умолчанию, если параметр отсутствует.
     * @return Значение параметра или значение по умолчанию.
     */
    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value == null ? defaultValue : value.toString();
    }
}
