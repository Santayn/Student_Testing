package org.santayn.testing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DemoApplicationTests {

	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	@Test
	void contextLoads() {
	}

	@Test
	void exposesNewApiWithoutLegacyCompatibilityRoutes() {
		Set<String> routes = requestMappingHandlerMapping.getHandlerMethods()
				.keySet()
				.stream()
				.flatMap(mappingInfo -> mappingInfo.getPatternValues().stream())
				.collect(Collectors.toSet());

		assertThat(routes)
				.allMatch(route -> !route.equals("/api")
						&& (!route.startsWith("/api/") || route.startsWith("/api/v1/")));

		assertThat(routes)
				.contains(
						"/api/v1/status/readiness",
						"/api/v1/courses/templates",
						"/api/v1/memberships/subjects",
						"/api/v1/teaching/assignments",
						"/api/v1/public/learning/subjects/{subjectId}",
						"/api/v1/results/teacher/data",
						"/api/v1/public/learning/test-assignments/{assignmentId}/attempts/start"
				)
				.doesNotContain(
						"/api/v1/public/learning/tests/{testId}/submit",
						"/api/v1/teacher-subjects/me",
						"/api/v1/group-students/{groupId}",
						"/api/v1/faculty-subjects/{facultyId}",
						"/api/v1/teacher/results/data",
						"/api/v1/subjects/{subjectId}/topics"
				);
	}
}
