package org.santayn.testing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StaticApiMigrationTests {

    @Test
    void nonAuthStaticPagesLoadSharedNavigationScript() throws IOException {
        Path staticRoot = Path.of("src", "main", "resources", "static");
        List<String> authPages = List.of("login.html", "register.html");

        List<String> pagesWithoutSharedScript;
        try (var files = Files.walk(staticRoot)) {
            pagesWithoutSharedScript = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".html"))
                    .filter(path -> !authPages.contains(path.getFileName().toString()))
                    .filter(path -> !contains(path, "/js/api.js"))
                    .map(path -> staticRoot.relativize(path).toString())
                    .toList();
        }

        assertThat(pagesWithoutSharedScript).isEmpty();
    }

    @Test
    void sharedNavigationScriptOwnsSidebarRendering() throws IOException {
        Path apiScript = Path.of("src", "main", "resources", "static", "js", "api.js");
        String script = Files.readString(apiScript);

        assertThat(script)
                .contains("const SIDEBAR_ITEMS")
                .contains("function populateSidebar")
                .contains("function installSidebarIfMissing")
                .contains("data-sidebar-logout")
                .contains("/profile.html")
                .contains("/subjects.html")
                .contains("/manage-teacher-groups.html")
                .contains("/questions-page.html")
                .contains("/result-list.html");
    }

    @Test
    void adminTeacherGroupPageUsesTeachingAssignmentWorkflow() throws IOException {
        String adminTeacherGroupsPage = Files.readString(Path.of("src", "main", "resources", "static", "manage-teacher-groups.html"));

        assertThat(adminTeacherGroupsPage)
                .contains("facultySelect")
                .contains("studyCourseSelect")
                .contains("subjectTeacherRows")
                .contains("addSubjectTeacherRowBtn")
                .contains("createSubjectTeacherRow")
                .contains("data-row-subject")
                .contains("data-row-teacher")
                .contains("groupChecklist")
                .contains("template-accordion")
                .contains("/faculties/")
                .contains("/groups?facultyId=")
                .contains("/memberships/subjects?activeOnly=false")
                .contains("/teaching/assignments")
                .contains("ensureAdmin");
    }

    @Test
    void staticPagesExposeQuestionTopicSelectionWorkflow() throws IOException {
        String createTestPage = Files.readString(Path.of("src", "main", "resources", "static", "create-test.html"));
        String questionsPage = Files.readString(Path.of("src", "main", "resources", "static", "questions-page.html"));
        String lecturesPage = Files.readString(Path.of("src", "main", "resources", "static", "manage-lectures-subject.html"));
        String schema = Files.readString(Path.of("src", "main", "resources", "schema.sql"));

        assertThat(createTestPage)
                .contains("groupChecklist")
                .contains("topicSelect")
                .contains("topicQuestionList")
                .contains("topicId")
                .contains("teachingAssignmentId")
                .contains("singleAnswerQuestionCount");

        assertThat(questionsPage)
                .contains("topicSelect")
                .contains("testId: null")
                .contains("subjectMembershipId")
                .contains("/questions?topicId=");

        assertThat(lecturesPage)
                .contains("lectureLinkedTestSelect")
                .contains("subjectMembershipId")
                .contains("/topics?subjectMembershipId=");

        assertThat(schema)
                .contains("\"TeachingAssignmentId\"")
                .contains("\"SubjectMembershipId\"")
                .contains("\"LinkedTestId\"")
                .contains("\"TestQuestionSelectionRules\"")
                .contains("\"TopicId\"")
                .contains("DROP NOT NULL");
    }

    @Test
    void staticPagesDoNotCallLegacyCompatibilityApi() throws IOException {
        Path staticRoot = Path.of("src", "main", "resources", "static");
        List<String> forbiddenFragments = List.of(
                "/teacher-subjects",
                "/teacher-groups",
                "/group-students",
                "/faculty-subjects",
                "/teacher/results",
                "/questions/topics",
                "/legacy/lectures",
                "/public/tests",
                "/public/lectures",
                "/public/subjects",
                "/users/by-role"
        );

        List<String> matches;
        try (var files = Files.walk(staticRoot)) {
            matches = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".html") || path.toString().endsWith(".js"))
                    .flatMap(path -> forbiddenFragments.stream()
                            .filter(fragment -> contains(path, fragment))
                            .map(fragment -> staticRoot.relativize(path) + " contains " + fragment))
                    .toList();
        }

        assertThat(matches).isEmpty();
    }

    private static boolean contains(Path path, String fragment) {
        try {
            return Files.readString(path).contains(fragment);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to read " + path, error);
        }
    }
}
