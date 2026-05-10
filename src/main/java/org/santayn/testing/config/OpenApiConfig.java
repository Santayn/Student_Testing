package org.santayn.testing.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Student Test Platform API",
                version = "1.0",
                description = "REST API для платформы тестирования студентов. Документация включает публичные сценарии обучения, административное управление учебной структурой, тесты, вопросы, попытки, результаты и JWT-авторизацию через bearerAuth."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    private static final Map<String, String> TAG_DESCRIPTIONS = tagDescriptions();
    private static final Map<OperationKey, OperationDoc> OPERATION_DOCS = operationDocs();

    @Bean
    public OpenApiCustomizer describeOperations() {
        return openApi -> {
            openApi.setTags(TAG_DESCRIPTIONS.entrySet()
                    .stream()
                    .map(entry -> new Tag().name(entry.getKey()).description(entry.getValue()))
                    .toList());

            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap()
                    .forEach((method, operation) -> describeOperation(path, method, operation)));
        };
    }

    private static void describeOperation(String path, PathItem.HttpMethod method, Operation operation) {
        String normalizedPath = normalizePath(path);
        OperationDoc doc = OPERATION_DOCS.get(new OperationKey(method.name(), normalizedPath));
        if (doc == null) {
            String tag = fallbackTag(normalizedPath);
            operation.setSummary(method.name() + " " + normalizedPath);
            operation.setDescription("Операция REST API для пути `" + normalizedPath
                    + "`. Используйте параметры пути, query-параметры и тело запроса согласно схемам Swagger.");
            operation.setTags(List.of(tag));
            describeRequestAndResponses(operation, method.name() + " " + normalizedPath);
            return;
        }

        operation.setSummary(doc.summary());
        operation.setDescription(doc.description());
        operation.setTags(List.of(doc.tag()));
        describeRequestAndResponses(operation, doc.summary());
    }

    private static void describeRequestAndResponses(Operation operation, String operationSummary) {
        if (operation.getRequestBody() != null && isBlank(operation.getRequestBody().getDescription())) {
            operation.getRequestBody().setDescription("Тело запроса для операции: " + operationSummary
                    + ". Заполните поля по схеме ниже; обязательные поля отмечены в Swagger как required.");
        }

        if (operation.getResponses() == null) {
            return;
        }

        operation.getResponses().forEach((status, response) -> {
            String currentDescription = response.getDescription();
            if (!isDefaultResponseDescription(currentDescription)) {
                return;
            }
            if ("204".equals(status)) {
                response.setDescription("Операция выполнена успешно, тело ответа отсутствует.");
                return;
            }
            if (status.startsWith("2")) {
                response.setDescription("Успешный ответ для операции: " + operationSummary
                        + ". Формат данных описан в схеме ответа ниже.");
            }
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isDefaultResponseDescription(String description) {
        return isBlank(description)
                || "OK".equals(description)
                || "Created".equals(description)
                || "Accepted".equals(description)
                || "No Content".equals(description);
    }

    private static String normalizePath(String path) {
        if (path.startsWith("/api/v1/")) {
            return "/api/" + path.substring("/api/v1/".length());
        }
        if (path.equals("/api/v1")) {
            return "/api";
        }
        return path;
    }

    private static String fallbackTag(String path) {
        if (path.startsWith("/api/auth")) {
            return "Аутентификация";
        }
        if (path.startsWith("/api/public/learning")) {
            return "Публичное обучение";
        }
        if (path.startsWith("/api/results")) {
            return "Результаты";
        }
        if (path.startsWith("/api/tests")) {
            return "Тесты";
        }
        if (path.startsWith("/api/questions")) {
            return "Вопросы";
        }
        if (path.startsWith("/api/teaching")) {
            return "Учебная нагрузка";
        }
        if (path.startsWith("/api/memberships")) {
            return "Участники";
        }
        if (path.startsWith("/api/courses")) {
            return "Курсы";
        }
        if (path.startsWith("/api/lectures")) {
            return "Лекции";
        }
        if (path.startsWith("/api/users")) {
            return "Пользователи";
        }
        if (path.startsWith("/api/roles")) {
            return "Роли и права";
        }
        if (path.startsWith("/api/faculties")) {
            return "Факультеты";
        }
        if (path.startsWith("/api/groups")) {
            return "Группы";
        }
        if (path.startsWith("/api/subjects")) {
            return "Предметы";
        }
        return "Система";
    }

    private static Map<String, String> tagDescriptions() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("Система", "Служебные проверки работоспособности backend.");
        tags.put("Аутентификация", "Вход, регистрация, обновление и отзыв JWT-токенов, смена пароля и данные текущего пользователя.");
        tags.put("Факультеты", "CRUD-операции для факультетов: создание, просмотр, изменение и удаление справочника факультетов.");
        tags.put("Группы", "CRUD-операции для учебных групп и их привязки к факультетам.");
        tags.put("Предметы", "CRUD-операции для предметов, которые используются в курсах, нагрузке и результатах.");
        tags.put("Пользователи", "Управление учетными записями, профилями людей, активностью пользователей, ролями и индивидуальными правами.");
        tags.put("Роли и права", "Справочники ролей и permissions, а также настройка прав, входящих в роль.");
        tags.put("Курсы", "Шаблоны курсов, версии курсов, публикация и снятие публикации учебного материала.");
        tags.put("Лекции", "Лекции внутри версии курса: порядок, контент, публичность и базовые CRUD-операции.");
        tags.put("Участники", "Membership-записи людей на факультетах, в группах и по предметам с ролями, статусами и заметками.");
        tags.put("Учебная нагрузка", "Типы нагрузки, преподавательские назначения, enrollments, назначение лекций и прогресс студентов.");
        tags.put("Тесты", "Создание тестов, правила выбора вопросов, назначения, попытки, ответы и завершение попыток.");
        tags.put("Вопросы", "Вопросы тестов, варианты ответов, активность вопросов и импорт вопросов из Word DOCX.");
        tags.put("Публичное обучение", "Публичные учебные сценарии фронтенда: чтение предметов и лекций, старт попытки и отправка ответов студентом.");
        tags.put("Результаты", "Выборки для преподавателя: доступные предметы, лекции, тесты, группы, студенты и статистика ответов.");
        return tags;
    }

    private static Map<OperationKey, OperationDoc> operationDocs() {
        Map<OperationKey, OperationDoc> docs = new LinkedHashMap<>();

        put(docs, "GET", "/api/status", "Система", "Проверить состояние API",
                "Возвращает короткий технический ответ со статусом сервера, именем backend и текущей меткой времени. Используется для smoke-проверок и мониторинга доступности приложения.");

        put(docs, "POST", "/api/auth/login", "Аутентификация", "Войти в систему",
                "Проверяет логин и пароль пользователя, создает пару access/refresh токенов и учитывает выбранный тип времени жизни сессии. IP-адрес и User-Agent сохраняются для аудита refresh-токена.");
        put(docs, "POST", "/api/auth/register", "Аутентификация", "Зарегистрировать пользователя",
                "Создает новую учетную запись, при необходимости связывает ее с существующей записью personId и сразу возвращает JWT-токены. Используется для первичной регистрации пользователя в платформе.");
        put(docs, "POST", "/api/auth/refresh", "Аутентификация", "Обновить токены",
                "Принимает refreshToken, проверяет его актуальность и выдает новую пару токенов. Новый refresh-токен привязывается к текущему IP-адресу и User-Agent клиента.");
        put(docs, "POST", "/api/auth/revoke", "Аутентификация", "Отозвать refresh-токен",
                "Деактивирует переданный refreshToken текущего авторизованного пользователя. После отзыва токен нельзя использовать для продления сессии.");
        put(docs, "POST", "/api/auth/change-password", "Аутентификация", "Сменить пароль",
                "Проверяет текущий пароль авторизованного пользователя и сохраняет новый пароль. Операция требует действующий Bearer JWT в заголовке Authorization.");
        put(docs, "GET", "/api/auth/me", "Аутентификация", "Получить текущего пользователя",
                "Возвращает сведения о текущей авторизованной учетной записи: логин, personId, роли, permissions и связанные данные профиля.");

        put(docs, "GET", "/api/faculties", "Факультеты", "Получить список факультетов",
                "Возвращает полный список факультетов из справочника. Ответ используется на страницах управления группами, студентами и учебной структурой.");
        put(docs, "GET", "/api/faculties/{id}", "Факультеты", "Получить факультет по ID",
                "Возвращает одну запись факультета по идентификатору. Если факультет не найден, API вернет стандартную ошибку обработки запроса.");
        put(docs, "POST", "/api/faculties", "Факультеты", "Создать факультет",
                "Создает факультет с названием, коротким кодом и необязательным описанием. Код используется как компактное обозначение факультета в интерфейсе.");
        put(docs, "PUT", "/api/faculties/{id}", "Факультеты", "Обновить факультет",
                "Изменяет название, код и описание существующего факультета. Идентификатор берется из пути, а новые значения из тела запроса.");
        put(docs, "DELETE", "/api/faculties/{id}", "Факультеты", "Удалить факультет",
                "Удаляет факультет по идентификатору. Перед удалением убедитесь, что на факультет не завязаны активные группы или membership-записи.");

        put(docs, "GET", "/api/groups", "Группы", "Получить список групп",
                "Возвращает учебные группы. Query-параметр facultyId позволяет ограничить список группами конкретного факультета.");
        put(docs, "GET", "/api/groups/{id}", "Группы", "Получить группу по ID",
                "Возвращает карточку учебной группы: идентификатор, название, код и факультет, к которому она относится.");
        put(docs, "POST", "/api/groups", "Группы", "Создать учебную группу",
                "Создает группу с названием, кодом и обязательной привязкой к факультету. Используется перед добавлением студентов в группу.");
        put(docs, "PUT", "/api/groups/{id}", "Группы", "Обновить учебную группу",
                "Обновляет название, код и факультет существующей группы. Идентификатор изменяемой группы передается в пути.");
        put(docs, "DELETE", "/api/groups/{id}", "Группы", "Удалить учебную группу",
                "Удаляет группу по идентификатору. Перед удалением проверьте, что группа не используется в учебной нагрузке или активных memberships.");

        put(docs, "GET", "/api/subjects", "Предметы", "Получить список предметов",
                "Возвращает все предметы, доступные в справочнике платформы. Предметы являются базой для курсов, преподавательской нагрузки и отчетов.");
        put(docs, "GET", "/api/subjects/{id}", "Предметы", "Получить предмет по ID",
                "Возвращает название и описание конкретного предмета по его идентификатору.");
        put(docs, "POST", "/api/subjects", "Предметы", "Создать предмет",
                "Создает новый предмет с названием и необязательным описанием. После создания предмет можно использовать при создании шаблонов курсов.");
        put(docs, "PUT", "/api/subjects/{id}", "Предметы", "Обновить предмет",
                "Изменяет название и описание существующего предмета. Идентификатор предмета берется из URL.");
        put(docs, "DELETE", "/api/subjects/{id}", "Предметы", "Удалить предмет",
                "Удаляет предмет по идентификатору. Операция подходит только для записей, которые больше не используются курсами и назначениями.");

        put(docs, "GET", "/api/users", "Пользователи", "Получить список пользователей",
                "Возвращает учетные записи пользователей вместе с ролями, permissions, активностью и связанной персоной, если она назначена.");
        put(docs, "GET", "/api/users/{id}", "Пользователи", "Получить пользователя по ID",
                "Возвращает подробную карточку пользователя по идентификатору учетной записи.");
        put(docs, "GET", "/api/users/me", "Пользователи", "Получить свой профиль",
                "Возвращает данные текущего авторизованного пользователя на основе JWT-токена. Используется фронтендом для отображения меню и прав доступа.");
        put(docs, "GET", "/api/users/people", "Пользователи", "Получить список персон",
                "Возвращает справочник физических лиц: имя, фамилию, дату рождения, email и телефон. Персоны могут быть связаны с пользователями и membership-записями.");
        put(docs, "GET", "/api/users/people/{personId}", "Пользователи", "Получить персону по ID",
                "Возвращает одну запись person по идентификатору. Используется при редактировании профиля человека или привязке к учетной записи.");
        put(docs, "POST", "/api/users/people", "Пользователи", "Создать персону",
                "Создает запись физического лица с именем, фамилией, email, датой рождения и телефоном. После создания personId можно использовать в регистрации и memberships.");
        put(docs, "PUT", "/api/users/people/{personId}", "Пользователи", "Обновить персону",
                "Обновляет персональные данные существующей записи person. Идентификатор берется из пути, новые значения передаются в JSON-теле.");
        put(docs, "PUT", "/api/users/{id}/active", "Пользователи", "Изменить активность пользователя",
                "Включает или отключает учетную запись пользователя. Неактивный пользователь не должен проходить обычную авторизацию в системе.");
        put(docs, "PUT", "/api/users/{id}/roles", "Пользователи", "Назначить роли пользователю",
                "Полностью заменяет набор ролей пользователя на переданный список roleIds. Используйте для выдачи ролей STUDENT, TEACHER, ADMIN и других настроенных ролей.");
        put(docs, "PUT", "/api/users/{id}/permissions", "Пользователи", "Назначить индивидуальные права",
                "Полностью заменяет индивидуальные permissions пользователя. Эти права дополняют или уточняют доступ, который пользователь получает через роли.");

        put(docs, "GET", "/api/roles", "Роли и права", "Получить список ролей",
                "Возвращает все роли платформы вместе с описаниями и связанными permissions. Используется в административном управлении доступом.");
        put(docs, "POST", "/api/roles", "Роли и права", "Создать роль",
                "Создает новую роль с именем и описанием. После создания к роли можно привязать permissions отдельной операцией.");
        put(docs, "GET", "/api/roles/permissions", "Роли и права", "Получить список permissions",
                "Возвращает справочник доступных permissions, которые можно назначать ролям или конкретным пользователям.");
        put(docs, "POST", "/api/roles/permissions", "Роли и права", "Создать permission",
                "Создает новое право доступа с системным именем и описанием. Используйте для расширения модели доступа без изменения существующих ролей.");
        put(docs, "PUT", "/api/roles/{id}/permissions", "Роли и права", "Настроить права роли",
                "Полностью заменяет список permissions, входящих в указанную роль. Все пользователи с этой ролью получат обновленный набор прав.");

        put(docs, "GET", "/api/courses/templates", "Курсы", "Получить шаблоны курсов",
                "Возвращает шаблоны курсов с фильтрами по subjectId, authorPersonId и признаку publicOnly. Шаблон объединяет версии курса по одному предмету.");
        put(docs, "GET", "/api/courses/templates/{templateId}", "Курсы", "Получить шаблон курса",
                "Возвращает один шаблон курса по идентификатору: предмет, автор, название и признак публичной видимости.");
        put(docs, "POST", "/api/courses/templates", "Курсы", "Создать шаблон курса",
                "Создает шаблон курса для выбранного предмета и автора. Версии курса создаются отдельно внутри этого шаблона.");
        put(docs, "PUT", "/api/courses/templates/{templateId}", "Курсы", "Обновить шаблон курса",
                "Изменяет предмет, автора, название и публичность существующего шаблона курса.");
        put(docs, "DELETE", "/api/courses/templates/{templateId}", "Курсы", "Удалить шаблон курса",
                "Удаляет шаблон курса по идентификатору. Операция должна применяться только к шаблонам, которые не используются активными версиями и назначениями.");
        put(docs, "PUT", "/api/courses/versions/{versionId}/publish", "Курсы", "Опубликовать версию курса",
                "Помечает версию курса как опубликованную и сохраняет идентификатор преподавателя или администратора, выполнившего публикацию.");
        put(docs, "PUT", "/api/courses/versions/{versionId}/unpublish", "Курсы", "Снять публикацию версии курса",
                "Делает версию курса недоступной в публичных учебных сценариях. Сама версия и ее лекции остаются в базе для редактирования.");
        put(docs, "GET", "/api/courses/templates/{templateId}/versions", "Курсы", "Получить версии шаблона",
                "Возвращает все версии курса внутри указанного шаблона. Версии позволяют вести историю изменений и публиковать стабильные материалы.");
        put(docs, "GET", "/api/courses/versions/{versionId}", "Курсы", "Получить версию курса",
                "Возвращает конкретную версию курса: номер версии, заголовок, описание, автора, статус публикации и заметки об изменениях.");
        put(docs, "POST", "/api/courses/templates/{templateId}/versions", "Курсы", "Создать версию курса",
                "Создает новую версию курса внутри шаблона. Можно сразу указать номер версии, заголовок, описание, автора, публикацию и changeNotes.");
        put(docs, "PUT", "/api/courses/versions/{versionId}", "Курсы", "Обновить версию курса",
                "Изменяет номер, заголовок, описание, автора и заметки существующей версии курса без отдельного изменения статуса публикации.");

        put(docs, "GET", "/api/lectures", "Лекции", "Получить список лекций",
                "Возвращает лекции, при необходимости отфильтрованные по courseVersionId. Список отсортирован сервисным слоем согласно логике курса.");
        put(docs, "GET", "/api/lectures/{id}", "Лекции", "Получить лекцию по ID",
                "Возвращает данные лекции: версию курса, порядковый номер, заголовок, описание, ключ папки контента и публичность.");
        put(docs, "POST", "/api/lectures", "Лекции", "Создать лекцию",
                "Создает лекцию внутри версии курса. ordinal задает порядок показа, contentFolderKey указывает на папку или ключ статического контента.");
        put(docs, "PUT", "/api/lectures/{id}", "Лекции", "Обновить лекцию",
                "Изменяет версию курса, порядок, заголовок, описание, ключ контента и публичность существующей лекции.");
        put(docs, "DELETE", "/api/lectures/{id}", "Лекции", "Удалить лекцию",
                "Удаляет лекцию по идентификатору. Перед удалением проверьте связанные тесты, назначения лекций и прогресс студентов.");

        put(docs, "GET", "/api/memberships/faculties", "Участники", "Найти участников факультетов",
                "Возвращает membership-записи факультетов с фильтрами по facultyId, personId, status и activeOnly. Подходит для поиска преподавателей, администраторов и других ролей на факультете.");
        put(docs, "GET", "/api/memberships/faculties/{facultyId}", "Участники", "Получить участников факультета",
                "Возвращает активных участников конкретного факультета. Используется на странице управления составом факультета.");
        put(docs, "GET", "/api/memberships/faculties/memberships/{membershipId}", "Участники", "Получить membership факультета",
                "Возвращает одну membership-запись факультета по идентификатору, включая personId, роль, статус и заметки.");
        put(docs, "POST", "/api/memberships/faculties/{facultyId}", "Участники", "Добавить участника факультета",
                "Создает membership-запись человека на факультете. В теле запроса передаются personId, числовая роль и необязательные заметки.");
        put(docs, "PUT", "/api/memberships/faculties/memberships/{membershipId}/status", "Участники", "Изменить статус участника факультета",
                "Обновляет только статус membership-записи факультета. Используется для активации, деактивации или перевода записи в другой жизненный статус.");
        put(docs, "PUT", "/api/memberships/faculties/memberships/{membershipId}", "Участники", "Обновить membership факультета",
                "Изменяет статус и заметки membership-записи факультета без смены привязанного человека и факультета.");
        put(docs, "GET", "/api/memberships/groups", "Участники", "Найти участников групп",
                "Возвращает membership-записи учебных групп с фильтрами по groupId, personId, status и activeOnly. Обычно используется для списка студентов группы.");
        put(docs, "POST", "/api/memberships/groups/{groupId}", "Участники", "Добавить участника группы",
                "Создает membership-запись человека в учебной группе. Роль определяет, является ли человек студентом или выполняет другую функцию в группе.");
        put(docs, "GET", "/api/memberships/groups/{groupId}", "Участники", "Получить участников группы",
                "Возвращает активных участников указанной группы. Используется для управления студентами и формирования выборок по результатам.");
        put(docs, "GET", "/api/memberships/groups/memberships/{membershipId}", "Участники", "Получить membership группы",
                "Возвращает одну membership-запись группы по идентификатору, включая personId, роль, статус и заметки.");
        put(docs, "PUT", "/api/memberships/groups/memberships/{membershipId}/status", "Участники", "Изменить статус участника группы",
                "Обновляет только статус membership-записи группы. Это позволяет временно отключить или восстановить участие человека в группе.");
        put(docs, "PUT", "/api/memberships/groups/memberships/{membershipId}", "Участники", "Обновить membership группы",
                "Изменяет статус и заметки membership-записи группы без изменения связей groupId и personId.");
        put(docs, "GET", "/api/memberships/subjects", "Участники", "Найти участников предметов",
                "Возвращает membership-записи предметов с фильтрами по subjectId, personId, status и activeOnly. Используется для преподавателей и ответственных по предмету.");
        put(docs, "GET", "/api/memberships/subjects/{subjectId}", "Участники", "Получить участников предмета",
                "Возвращает активных участников выбранного предмета. Эти записи участвуют в настройке учебной нагрузки.");
        put(docs, "GET", "/api/memberships/subjects/memberships/{membershipId}", "Участники", "Получить membership предмета",
                "Возвращает одну membership-запись предмета по идентификатору.");
        put(docs, "POST", "/api/memberships/subjects/{subjectId}", "Участники", "Добавить участника предмета",
                "Создает membership-запись человека по предмету. В теле запроса указываются personId, роль участника и необязательные заметки.");
        put(docs, "PUT", "/api/memberships/subjects/memberships/{membershipId}/status", "Участники", "Изменить статус участника предмета",
                "Обновляет только статус membership-записи предмета. Подходит для активации и деактивации преподавателя по предмету.");
        put(docs, "PUT", "/api/memberships/subjects/memberships/{membershipId}", "Участники", "Обновить membership предмета",
                "Изменяет статус и заметки membership-записи предмета без смены предмета и человека.");

        put(docs, "GET", "/api/teaching/load-types", "Учебная нагрузка", "Получить типы учебной нагрузки",
                "Возвращает справочник типов нагрузки, например лекции, практики или лабораторные. Типы используются при создании преподавательских назначений.");
        put(docs, "GET", "/api/teaching/load-types/{loadTypeId}", "Учебная нагрузка", "Получить тип нагрузки",
                "Возвращает один тип учебной нагрузки по идентификатору.");
        put(docs, "POST", "/api/teaching/load-types", "Учебная нагрузка", "Создать тип нагрузки",
                "Создает новый тип учебной нагрузки с названием и описанием. После создания его можно назначать subject membership-записям.");
        put(docs, "PUT", "/api/teaching/load-types/{loadTypeId}", "Учебная нагрузка", "Обновить тип нагрузки",
                "Изменяет название и описание существующего типа учебной нагрузки.");
        put(docs, "GET", "/api/teaching/subject-load-types", "Учебная нагрузка", "Найти типы нагрузки по предметам",
                "Возвращает связи subjectMembershipId и teachingLoadTypeId. Фильтры позволяют найти, какие виды нагрузки разрешены конкретному преподавателю по предмету.");
        put(docs, "POST", "/api/teaching/subject-memberships/{subjectMembershipId}/load-types", "Учебная нагрузка", "Добавить тип нагрузки преподавателю",
                "Создает связь между membership-записью преподавателя по предмету и типом нагрузки. Используется перед формированием конкретных назначений на группы.");
        put(docs, "PUT", "/api/teaching/subject-load-types/{subjectLoadTypeId}/status", "Учебная нагрузка", "Изменить статус типа нагрузки по предмету",
                "Обновляет статус связи преподавателя с типом нагрузки. Позволяет временно отключить конкретный вид нагрузки без удаления записи.");
        put(docs, "PUT", "/api/teaching/subject-load-types/{subjectLoadTypeId}", "Учебная нагрузка", "Обновить тип нагрузки по предмету",
                "Изменяет статус и заметки связи subjectMembershipId с teachingLoadTypeId.");
        put(docs, "GET", "/api/teaching/assignments", "Учебная нагрузка", "Найти преподавательские назначения",
                "Возвращает назначения преподавателей с фильтрами по группе, subjectMembershipId, версии курса, типу нагрузки и статусу. Это основной список учебной нагрузки.");
        put(docs, "GET", "/api/teaching/assignments/{id}", "Учебная нагрузка", "Получить преподавательское назначение",
                "Возвращает одно преподавательское назначение по идентификатору, включая группу, курс, семестр, учебный год, часы и статус.");
        put(docs, "POST", "/api/teaching/assignments", "Учебная нагрузка", "Создать преподавательское назначение",
                "Создает назначение преподавателя на группу, предмет, тип нагрузки и версию курса. Параметры semester, academicYear и hoursPerWeek описывают учебный период.");
        put(docs, "PUT", "/api/teaching/assignments/{assignmentId}", "Учебная нагрузка", "Обновить преподавательское назначение",
                "Изменяет состав, курс, период, часы, статус и заметки существующего преподавательского назначения.");
        put(docs, "PUT", "/api/teaching/assignments/{assignmentId}/status", "Учебная нагрузка", "Изменить статус назначения",
                "Обновляет только статус преподавательского назначения. Используется для перевода нагрузки между черновиком, активным и закрытым состояниями.");
        put(docs, "GET", "/api/teaching/enrollments", "Учебная нагрузка", "Найти enrollments студентов",
                "Возвращает связи студентов группы с преподавательскими назначениями. Фильтры доступны по assignment, groupMembershipId, groupId и status.");
        put(docs, "POST", "/api/teaching/assignments/{assignmentId}/enrollments", "Учебная нагрузка", "Добавить enrollment студента",
                "Создает связь между преподавательским назначением и membership-записью студента в группе. Через эту связь учитываются лекции, тесты и прогресс.");
        put(docs, "PUT", "/api/teaching/enrollments/{enrollmentId}/status", "Учебная нагрузка", "Изменить статус enrollment",
                "Обновляет статус связи студента с преподавательским назначением. Позволяет закрывать или восстанавливать участие студента в курсе.");
        put(docs, "GET", "/api/teaching/lecture-assignments", "Учебная нагрузка", "Найти назначения лекций",
                "Возвращает связи лекций с преподавательскими назначениями. Можно фильтровать по teachingAssignmentId и courseLectureId.");
        put(docs, "POST", "/api/teaching/assignments/{assignmentId}/lecture-assignments", "Учебная нагрузка", "Назначить лекцию группе",
                "Создает назначение лекции внутри преподавательского assignment. В теле задаются сроки доступности, обязательность, минимальный прогресс и статус.");
        put(docs, "PUT", "/api/teaching/lecture-assignments/{lectureAssignmentId}", "Учебная нагрузка", "Обновить назначение лекции",
                "Изменяет лекцию, сроки, обязательность, минимальный процент прогресса и статус существующего назначения лекции.");
        put(docs, "PUT", "/api/teaching/lecture-assignments/{lectureAssignmentId}/status", "Учебная нагрузка", "Изменить статус назначения лекции",
                "Обновляет только статус назначения лекции. Используйте для публикации, закрытия или отключения лекции для студентов.");
        put(docs, "GET", "/api/teaching/lecture-progress", "Учебная нагрузка", "Получить прогресс по лекциям",
                "Возвращает прогресс студентов по назначенным лекциям. Можно фильтровать по lectureAssignmentId и teachingAssignmentEnrollmentId.");
        put(docs, "POST", "/api/teaching/lecture-assignments/{lectureAssignmentId}/progress", "Учебная нагрузка", "Обновить прогресс студента",
                "Создает или обновляет прогресс студента по лекции: процент прохождения, статус, источник завершения, позицию просмотра и затраченное время.");

        put(docs, "GET", "/api/tests", "Тесты", "Получить список тестов",
                "Возвращает все тесты с названием, описанием, длительностью, лимитом попыток и количеством вопросов.");
        put(docs, "GET", "/api/tests/{id}", "Тесты", "Получить тест по ID",
                "Возвращает карточку теста по идентификатору. Не включает полный список вопросов, для вопросов используется отдельный API.");
        put(docs, "POST", "/api/tests", "Тесты", "Создать тест",
                "Создает тест с параметрами времени, попыток, количества вопросов и необязательными правилами выбора вопросов по лекциям и типам.");
        put(docs, "PUT", "/api/tests/{id}", "Тесты", "Обновить тест",
                "Изменяет параметры теста и при передаче selectionRules заменяет правила тематического выбора вопросов.");
        put(docs, "GET", "/api/tests/{testId}/selection-rules", "Тесты", "Получить правила выбора вопросов",
                "Возвращает правила, по которым тест набирает вопросы из лекций: общее число, текстовые, одиночные и множественные варианты.");
        put(docs, "PUT", "/api/tests/{testId}/selection-rules", "Тесты", "Заменить правила выбора вопросов",
                "Полностью заменяет набор правил выбора вопросов для теста. Сумма лимитов по типам не должна превышать questionCount правила.");
        put(docs, "GET", "/api/tests/assignments", "Тесты", "Найти назначения тестов",
                "Возвращает назначения тестов с фильтрами по testId, courseVersionId и courseLectureId. Назначение определяет доступность теста студентам.");
        put(docs, "GET", "/api/tests/assignments/{assignmentId}", "Тесты", "Получить назначение теста",
                "Возвращает одно назначение теста по идентификатору, включая область назначения, сроки доступности и статус.");
        put(docs, "POST", "/api/tests/{testId}/assignments", "Тесты", "Назначить тест",
                "Создает назначение теста на версию курса или конкретную лекцию. В теле задаются scope, сроки availableFromUtc/availableUntilUtc и статус.");
        put(docs, "PUT", "/api/tests/assignments/{assignmentId}", "Тесты", "Обновить назначение теста",
                "Изменяет область, сроки и статус существующего назначения теста.");
        put(docs, "PUT", "/api/tests/assignments/{assignmentId}/status", "Тесты", "Изменить статус назначения теста",
                "Обновляет только статус назначения теста. Позволяет публиковать, закрывать или отключать доступ к тесту.");
        put(docs, "GET", "/api/tests/attempts", "Тесты", "Найти попытки тестов",
                "Возвращает попытки прохождения с фильтрами по testAssignmentId и personId. Используется для просмотра истории прохождений.");
        put(docs, "GET", "/api/tests/attempts/{attemptId}", "Тесты", "Получить попытку теста",
                "Возвращает одну попытку теста по идентификатору: студент, назначение, статус, баллы и временные метки.");
        put(docs, "POST", "/api/tests/assignments/{assignmentId}/attempts", "Тесты", "Начать попытку теста",
                "Создает попытку прохождения по назначению теста для указанного personId. При необходимости связывает попытку с enrollment по учебной нагрузке.");
        put(docs, "GET", "/api/tests/attempts/{attemptId}/responses", "Тесты", "Получить ответы попытки",
                "Возвращает все ответы, сохраненные в рамках конкретной попытки теста.");
        put(docs, "POST", "/api/tests/attempts/{attemptId}/responses", "Тесты", "Сохранить ответ на вопрос",
                "Сохраняет ответ по одному вопросу в попытке: текстовый ответ, выбранные варианты, начисленные баллы и признак правильности.");
        put(docs, "GET", "/api/tests/responses/{responseId}/selected-options", "Тесты", "Получить выбранные варианты ответа",
                "Возвращает варианты ответа, выбранные пользователем для конкретного responseId. Применяется для вопросов с одним или несколькими вариантами.");
        put(docs, "POST", "/api/tests/attempts/{attemptId}/complete", "Тесты", "Завершить попытку теста",
                "Закрывает попытку теста, сохраняет итоговый балл и статус. Обычно вызывается после отправки всех ответов.");

        put(docs, "GET", "/api/questions", "Вопросы", "Получить вопросы теста",
                "Возвращает вопросы, принадлежащие testId. Используется на странице редактирования теста и при проверке наполнения тематических правил.");
        put(docs, "GET", "/api/questions/{questionId}", "Вопросы", "Получить вопрос по ID",
                "Возвращает один вопрос теста: тип, текст, баллы, порядок, правильный текстовый ответ, активность и привязку к лекции.");
        put(docs, "GET", "/api/questions/{questionId}/options", "Вопросы", "Получить варианты вопроса",
                "Возвращает варианты ответа для вопроса, отсортированные по ordinal. Используется для вопросов с одним или несколькими вариантами.");
        put(docs, "POST", "/api/questions/import", "Вопросы", "Импортировать вопросы из DOCX",
                "Принимает multipart/form-data с файлом .docx, testId и необязательным courseLectureId. Парсер создает вопросы и варианты ответов по поддерживаемому Word-формату.");
        put(docs, "POST", "/api/questions", "Вопросы", "Создать вопрос",
                "Создает вопрос в тесте с типом, текстом, баллами, порядком, правильным ответом и необязательной привязкой к лекции для тематической выборки.");
        put(docs, "PUT", "/api/questions/{questionId}", "Вопросы", "Обновить вопрос",
                "Изменяет тип, текст, баллы, порядок, правильный ответ, активность и привязку вопроса к лекции.");
        put(docs, "GET", "/api/questions/options/{optionId}", "Вопросы", "Получить вариант ответа",
                "Возвращает один вариант ответа по идентификатору, включая текст, порядок и признак правильности.");
        put(docs, "POST", "/api/questions/{questionId}/options", "Вопросы", "Добавить вариант ответа",
                "Создает вариант ответа для вопроса. ordinal определяет порядок отображения, correct указывает, является ли вариант правильным.");
        put(docs, "PUT", "/api/questions/options/{optionId}", "Вопросы", "Обновить вариант ответа",
                "Изменяет текст, порядок и признак правильности существующего варианта ответа.");
        put(docs, "PUT", "/api/questions/{questionId}/active", "Вопросы", "Изменить активность вопроса",
                "Включает или отключает вопрос. Неактивные вопросы не должны попадать в случайную выборку при старте попытки.");

        put(docs, "GET", "/api/public/learning/subjects/{subjectId}", "Публичное обучение", "Получить публичный предмет",
                "Возвращает краткую информацию о предмете для учебного frontend-сценария: идентификатор, название и описание.");
        put(docs, "GET", "/api/public/learning/subjects/{subjectId}/lectures", "Публичное обучение", "Получить публичные лекции предмета",
                "Возвращает опубликованные и публично видимые лекции по всем опубликованным версиям курсов выбранного предмета.");
        put(docs, "GET", "/api/public/learning/lectures/{lectureId}", "Публичное обучение", "Получить публичную лекцию",
                "Возвращает данные лекции только если лекция, версия курса и шаблон курса публично доступны и опубликованы.");
        put(docs, "GET", "/api/public/learning/lectures/{lectureId}/tests", "Публичное обучение", "Получить доступные тесты лекции",
                "Возвращает активные тесты, назначенные на лекцию и доступные в текущий момент времени.");
        put(docs, "GET", "/api/public/learning/tests/{testId}", "Публичное обучение", "Загрузить публичный тест",
                "Возвращает карточку теста и случайный набор вопросов для первичной загрузки. Для полноценного прохождения лучше использовать старт попытки по assignmentId.");
        put(docs, "POST", "/api/public/learning/test-assignments/{assignmentId}/attempts/start", "Публичное обучение", "Начать или продолжить попытку",
                "Создает новую попытку прохождения или возвращает незавершенную попытку текущего пользователя. Вопросы выбираются случайно с учетом правил теста.");
        put(docs, "POST", "/api/public/learning/tests/{testId}/submit", "Публичное обучение", "Отправить ответы по тесту",
                "Создает попытку для активного назначения теста и сразу сохраняет переданные ответы. Подходит для простого сценария без предварительного startAttempt.");
        put(docs, "POST", "/api/public/learning/attempts/{attemptId}/submit", "Публичное обучение", "Отправить ответы попытки",
                "Сохраняет ответы текущего пользователя в уже созданную попытку, рассчитывает правильность и завершает попытку с итоговым баллом.");

        put(docs, "GET", "/api/results/teacher/subjects", "Результаты", "Получить предметы преподавателя",
                "Возвращает предметы, где текущий пользователь является преподавателем. JWT пользователя используется для определения personId.");
        put(docs, "GET", "/api/results/teacher/lectures", "Результаты", "Получить лекции для отчета",
                "Возвращает лекции выбранного subjectId по всем версиям курсов. Используется как следующий фильтр в цепочке отчетов преподавателя.");
        put(docs, "GET", "/api/results/teacher/tests", "Результаты", "Получить тесты по лекции",
                "Возвращает тесты, назначенные на lectureId. Список используется для фильтрации результатов по конкретному тесту.");
        put(docs, "GET", "/api/results/teacher/groups", "Результаты", "Получить группы по тесту",
                "Возвращает группы, которые связаны с выбранным testId через назначения, учебную нагрузку или фактические попытки студентов.");
        put(docs, "GET", "/api/results/teacher/students", "Результаты", "Получить студентов группы",
                "Возвращает студентов активной group membership-записи для выбранной группы. Используется для фильтрации отчета по студенту.");
        put(docs, "GET", "/api/results/teacher/data", "Результаты", "Получить данные отчета",
                "Возвращает статистику и детализацию ответов с фильтрами по subjectId, lectureId, testId, groupId и studentId. В ответ входят процент правильных ответов и список вопросов с ответами.");

        return docs;
    }

    private static void put(Map<OperationKey, OperationDoc> docs,
                            String method,
                            String path,
                            String tag,
                            String summary,
                            String description) {
        docs.put(new OperationKey(method, path), new OperationDoc(tag, summary, description));
    }

    private record OperationKey(String method, String path) {
    }

    private record OperationDoc(String tag, String summary, String description) {
    }
}
