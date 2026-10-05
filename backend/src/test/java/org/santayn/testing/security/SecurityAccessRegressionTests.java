package org.santayn.testing.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

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

    @Autowired
    private MockMvc mockMvc;

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
    void studentCannotCreateDatabaseBackup() throws Exception {
        assertStudentForbidden(HttpMethod.POST, "/api/v1/admin/database-backups");
    }

    @Test
    void studentCannotRestoreDatabaseBackup() throws Exception {
        assertStudentForbidden(HttpMethod.POST, "/api/v1/admin/database-backups/restore");
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
}
