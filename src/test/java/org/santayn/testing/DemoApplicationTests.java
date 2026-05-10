package org.santayn.testing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"app.data-loader.enabled=false",
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.sql.init.mode=never"
})
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
				.contains(
						"/api/v1/courses/templates",
						"/api/v1/memberships/subjects",
						"/api/v1/teaching/assignments",
						"/api/v1/public/learning/subjects/{subjectId}",
						"/api/v1/results/teacher/data"
				)
				.doesNotContain(
						"/api/v1/teacher-subjects/me",
						"/api/v1/group-students/{groupId}",
						"/api/v1/faculty-subjects/{facultyId}",
						"/api/v1/teacher/results/data",
						"/api/v1/subjects/{subjectId}/topics"
				);
	}
}
