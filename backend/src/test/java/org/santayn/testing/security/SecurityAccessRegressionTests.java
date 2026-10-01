package org.santayn.testing.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessRegressionTests {

    private static final List<SimpleGrantedAuthority> STANDARD_TEACHER_AUTHORITIES = List.of(
            new SimpleGrantedAuthority("ROLE_TEACHER"),
            new SimpleGrantedAuthority("TEACHER"),
            new SimpleGrantedAuthority("people.read"),
            new SimpleGrantedAuthority("PEOPLE.READ"),
            new SimpleGrantedAuthority("courses.manage"),
            new SimpleGrantedAuthority("COURSES.MANAGE"),
            new SimpleGrantedAuthority("teaching.manage"),
            new SimpleGrantedAuthority("TEACHING.MANAGE"),
            new SimpleGrantedAuthority("tests.manage"),
            new SimpleGrantedAuthority("TESTS.MANAGE"),
            new SimpleGrantedAuthority("questions.manage"),
            new SimpleGrantedAuthority("QUESTIONS.MANAGE"),
            new SimpleGrantedAuthority("lectures.read"),
            new SimpleGrantedAuthority("LECTURES.READ")
    );

    private static final List<SimpleGrantedAuthority> ROLE_ONLY_TEACHER_AUTHORITIES = List.of(
            new SimpleGrantedAuthority("ROLE_TEACHER"),
            new SimpleGrantedAuthority("TEACHER")
    );

    private static final List<SimpleGrantedAuthority> ROLE_ONLY_ADMIN_AUTHORITIES = List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ADMIN")
    );

    private static final List<SimpleGrantedAuthority> ROLES_MANAGE_ONLY_AUTHORITIES = List.of(
            new SimpleGrantedAuthority("roles.manage")
    );

    private static final List<SimpleGrantedAuthority> MULTI_ROLE_WITHOUT_FUNCTION_PERMISSION = List.of(
            new SimpleGrantedAuthority("ROLE_TEACHER"),
            new SimpleGrantedAuthority("TEACHER"),
            new SimpleGrantedAuthority("ROLE_STUDENT"),
            new SimpleGrantedAuthority("STUDENT")
    );

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicAuthConfigurationIsAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v1/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicRegistrationEnabled").value(false));
    }

    @Test
    void revokeDoesNotRequireBearerButStillRequiresCookieCsrf() throws Exception {
        mockMvc.perform(request(HttpMethod.POST, "/api/v1/auth/revoke"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unauthenticatedApiErrorUsesStructuredContract() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required."))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void studentCannotAccessQuestionBank() throws Exception {
        assertStudentForbidden(HttpMethod.GET, "/api/v1/questions");
        assertStudentForbidden(HttpMethod.POST, "/api/v1/questions");
    }

    @Test
    void studentCannotAccessRawAttemptsOrCompleteThem() throws Exception {
        assertStudentForbidden(HttpMethod.GET, "/api/v1/tests/attempts");
        assertStudentForbidden(HttpMethod.GET, "/api/v1/tests/attempts/1/responses");
        assertStudentForbidden(HttpMethod.POST, "/api/v1/tests/attempts/1/complete");
    }

    @Test
    void studentCannotUseTopicCrud() throws Exception {
        assertStudentForbidden(HttpMethod.GET, "/api/v1/topics");
        assertStudentForbidden(HttpMethod.POST, "/api/v1/topics");
        assertStudentForbidden(HttpMethod.PUT, "/api/v1/topics/1");
        assertStudentForbidden(HttpMethod.DELETE, "/api/v1/topics/1");
    }

    @Test
    void studentCannotUseAdministrativeTestApi() throws Exception {
        assertStudentForbidden(HttpMethod.GET, "/api/v1/tests");
        assertStudentForbidden(HttpMethod.POST, "/api/v1/tests");
        assertStudentForbidden(HttpMethod.PUT, "/api/v1/tests/1");
        assertStudentForbidden(HttpMethod.DELETE, "/api/v1/tests/1");
    }

    @Test
    void studentCannotUseGenericSubjectCatalogEndpoints() throws Exception {
        assertStudentForbidden(HttpMethod.GET, "/api/v1/subjects");
        assertStudentForbidden(HttpMethod.GET, "/api/v1/subjects/1");
        assertStudentForbidden(HttpMethod.GET, "/api/v1/subjects/1/faculties");
    }


    @Test
    void studentCannotEnumerateGenericAcademicCatalog() throws Exception {
        assertStudentForbidden(HttpMethod.GET, "/api/v1/faculties");
        assertStudentForbidden(HttpMethod.GET, "/api/v1/faculties/1");
        assertStudentForbidden(HttpMethod.GET, "/api/v1/groups");
        assertStudentForbidden(HttpMethod.GET, "/api/v1/groups/1");
    }

    @Test
    void standardTeacherUsesScopedTeachingAggregatesInsteadOfGenericAcademicCatalog() throws Exception {
        assertTeacherForbidden(HttpMethod.GET, "/api/v1/faculties");
        assertTeacherForbidden(HttpMethod.GET, "/api/v1/groups");
    }

    @Test
    void academicManagePermissionCanReadGenericAcademicCatalog() throws Exception {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("academic.manage")
        );

        mockMvc.perform(request(HttpMethod.GET, "/api/v1/faculties")
                        .with(user("academic-manager").authorities(authorities)))
                .andExpect(status().isOk());
        mockMvc.perform(request(HttpMethod.GET, "/api/v1/groups")
                        .with(user("academic-manager").authorities(authorities)))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotCreateDatabaseBackup() throws Exception {
        assertStudentForbidden(HttpMethod.POST, "/api/v1/admin/database-backups");
    }

    @Test
    void studentCannotRestoreDatabaseBackup() throws Exception {
        assertStudentForbidden(HttpMethod.POST, "/api/v1/admin/database-backups/restore");
    }

    @Test
    void teacherCannotChangePeople() throws Exception {
        assertTeacherForbidden(HttpMethod.POST, "/api/v1/users/people");
        assertTeacherForbidden(HttpMethod.PUT, "/api/v1/users/people/1");
    }

    @Test
    void teacherCannotChangeAcademicCatalog() throws Exception {
        assertTeacherForbidden(HttpMethod.POST, "/api/v1/faculties");
        assertTeacherForbidden(HttpMethod.PUT, "/api/v1/faculties/1");
        assertTeacherForbidden(HttpMethod.DELETE, "/api/v1/faculties/1");

        assertTeacherForbidden(HttpMethod.POST, "/api/v1/groups");
        assertTeacherForbidden(HttpMethod.PUT, "/api/v1/groups/1");
        assertTeacherForbidden(HttpMethod.DELETE, "/api/v1/groups/1");

        assertTeacherForbidden(HttpMethod.POST, "/api/v1/subjects");
        assertTeacherForbidden(HttpMethod.PUT, "/api/v1/subjects/1");
        assertTeacherForbidden(HttpMethod.DELETE, "/api/v1/subjects/1");

        assertTeacherForbidden(HttpMethod.POST, "/api/v1/teaching/assignments");
        assertTeacherForbidden(HttpMethod.POST, "/api/v1/teaching/load-types");
    }

    @Test
    void teacherRoleAloneDoesNotGrantFunctionalManagementPermissions() throws Exception {
        assertRoleOnlyTeacherForbidden(HttpMethod.GET, "/api/v1/users/people", "{}");
        assertRoleOnlyTeacherForbidden(
                HttpMethod.POST,
                "/api/v1/courses/templates",
                """
                {"subjectId":1,"name":"Role only template","publicVisible":false}
                """
        );
        assertRoleOnlyTeacherForbidden(
                HttpMethod.POST,
                "/api/v1/lectures",
                """
                {"subjectMembershipId":1,"ordinal":1,"title":"Role only lecture","contentFolderKey":"role-only"}
                """
        );
        assertRoleOnlyTeacherForbidden(
                HttpMethod.POST,
                "/api/v1/tests",
                """
                {"title":"Role only test","duration":"00:10:00","attemptsAllowed":1,"questionCount":1,"selectionRules":[]}
                """
        );
        assertRoleOnlyTeacherForbidden(
                HttpMethod.POST,
                "/api/v1/questions",
                """
                {"topicId":1,"type":1,"question":"Role only question","points":1,"ordinal":1,"correctAnswer":"A"}
                """
        );
        assertRoleOnlyTeacherForbidden(
                HttpMethod.POST,
                "/api/v1/teaching/subject-memberships/1/load-types",
                """
                {"teachingLoadTypeId":1,"notes":"role only"}
                """
        );
    }

    @Test
    void adminRoleAloneDoesNotBypassFunctionalPermissions() throws Exception {
        assertForbiddenWithAuthorities(HttpMethod.GET, "/api/v1/users", ROLE_ONLY_ADMIN_AUTHORITIES, "{}");
        assertForbiddenWithAuthorities(HttpMethod.POST, "/api/v1/faculties", ROLE_ONLY_ADMIN_AUTHORITIES, "{}");
        assertForbiddenWithAuthorities(HttpMethod.POST, "/api/v1/tests", ROLE_ONLY_ADMIN_AUTHORITIES, "{}");
        assertForbiddenWithAuthorities(HttpMethod.GET, "/api/v1/questions", ROLE_ONLY_ADMIN_AUTHORITIES, "{}");
        assertForbiddenWithAuthorities(HttpMethod.POST, "/api/v1/teaching/assignments", ROLE_ONLY_ADMIN_AUTHORITIES, "{}");
    }

    @Test
    void adminRoleAloneDoesNotBypassReadPermissions() throws Exception {
        assertForbiddenWithAuthorities(HttpMethod.GET, "/api/v1/teaching/assignments", ROLE_ONLY_ADMIN_AUTHORITIES, "{}");
        assertForbiddenWithAuthorities(HttpMethod.GET, "/api/v1/teaching/load-types", ROLE_ONLY_ADMIN_AUTHORITIES, "{}");
        assertForbiddenWithAuthorities(HttpMethod.GET, "/api/v1/teaching/lecture-assignments", ROLE_ONLY_ADMIN_AUTHORITIES, "{}");
        assertForbiddenWithAuthorities(HttpMethod.GET, "/api/v1/faculties", ROLE_ONLY_ADMIN_AUTHORITIES, "{}");
        assertForbiddenWithAuthorities(HttpMethod.GET, "/api/v1/groups", ROLE_ONLY_ADMIN_AUTHORITIES, "{}");
    }

    @Test
    void teacherRoleAloneDoesNotOpenTeacherProfileAggregate() throws Exception {
        assertRoleOnlyTeacherForbidden(HttpMethod.GET, "/api/v1/teaching/profile-context", "{}");
    }

    @Test
    void multipleGlobalRolesStillDoNotReplaceFunctionPermission() throws Exception {
        assertForbiddenWithAuthorities(
                HttpMethod.POST,
                "/api/v1/tests",
                MULTI_ROLE_WITHOUT_FUNCTION_PERMISSION,
                "{}"
        );
    }

    @Test
    void rolesManagePermissionDoesNotActAsUniversalAdminPermission() throws Exception {
        assertForbiddenWithAuthorities(HttpMethod.GET, "/api/v1/tests/attempts", ROLES_MANAGE_ONLY_AUTHORITIES, "{}");
        assertForbiddenWithAuthorities(HttpMethod.POST, "/api/v1/memberships/groups/1", ROLES_MANAGE_ONLY_AUTHORITIES, "{}");
        assertForbiddenWithAuthorities(HttpMethod.POST, "/api/v1/teaching/assignments", ROLES_MANAGE_ONLY_AUTHORITIES, "{}");
        assertForbiddenWithAuthorities(HttpMethod.PUT, "/api/v1/users/1/active", ROLES_MANAGE_ONLY_AUTHORITIES, "{\"active\":false}");
    }

    @Test
    void granularPermissionWorksWithoutGlobalRoleAuthority() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v1/roles")
                        .with(user("permission-only-role-manager").authorities(ROLES_MANAGE_ONLY_AUTHORITIES)))
                .andExpect(status().isOk());

        mockMvc.perform(request(HttpMethod.GET, "/api/v1/subjects")
                        .with(user("permission-only-course-manager")
                                .authorities(new SimpleGrantedAuthority("courses.manage"))))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCreateDatabaseBackup() throws Exception {
        MvcResult result = mockMvc.perform(request(HttpMethod.POST, "/api/v1/admin/database-backups")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("student-test-database-backup-")
                ))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/sql")))
                .andExpect(content().string(containsString("CREATE")));
    }

    @Test
    void adminCanRestoreDatabaseBackup() throws Exception {
        MvcResult backupResult = mockMvc.perform(request(HttpMethod.POST, "/api/v1/admin/database-backups")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted())
                .andReturn();
        MvcResult completedBackup = mockMvc.perform(asyncDispatch(backupResult))
                .andExpect(status().isOk())
                .andReturn();

        MockMultipartFile backupFile = new MockMultipartFile(
                "file",
                "backup.sql",
                "application/sql",
                completedBackup.getResponse().getContentAsByteArray()
        );

        mockMvc.perform(multipart("/api/v1/admin/database-backups/restore")
                        .file(backupFile)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("restored"))
                .andExpect(jsonPath("$.fileName").value("backup.sql"));
    }

    @Test
    void readinessIsPublicAndChecksDatabase() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v1/status/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"))
                .andExpect(jsonPath("$.database").value("up"));
    }

    private void assertStudentForbidden(HttpMethod method, String path) throws Exception {
        mockMvc.perform(request(method, path)
                        .with(user("student").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private void assertTeacherForbidden(HttpMethod method, String path) throws Exception {
        mockMvc.perform(request(method, path)
                        .with(user("teacher").authorities(STANDARD_TEACHER_AUTHORITIES))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private void assertRoleOnlyTeacherForbidden(HttpMethod method, String path, String content) throws Exception {
        mockMvc.perform(request(method, path)
                        .with(user("role-only-teacher").authorities(ROLE_ONLY_TEACHER_AUTHORITIES))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isForbidden());
    }

    private void assertForbiddenWithAuthorities(HttpMethod method,
                                                String path,
                                                List<SimpleGrantedAuthority> authorities,
                                                String content) throws Exception {
        mockMvc.perform(request(method, path)
                        .with(user("authorization-matrix-user").authorities(authorities))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isForbidden());
    }
}
