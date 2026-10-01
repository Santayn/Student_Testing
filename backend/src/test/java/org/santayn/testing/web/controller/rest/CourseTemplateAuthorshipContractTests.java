package org.santayn.testing.web.controller.rest;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.course.CourseTemplate;
import org.santayn.testing.service.CourseService;
import org.santayn.testing.service.CurrentUserAccessService;
import org.springframework.security.core.Authentication;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseTemplateAuthorshipContractTests {

    @Test
    void createTemplateUsesAuthenticatedActorAndDoesNotAcceptDelegatedOwner() {
        CourseService courseService = mock(CourseService.class);
        CurrentUserAccessService accessService = mock(CurrentUserAccessService.class);
        Authentication authentication = mock(Authentication.class);
        CourseRestController controller = new CourseRestController(courseService, accessService);

        when(accessService.currentPersonId(authentication)).thenReturn(42);
        CourseTemplate created = new CourseTemplate();
        created.setId(7);
        created.setSubjectId(3);
        created.setAuthorPersonId(42);
        created.setName("Template");
        created.setPublicVisible(false);
        when(courseService.createTemplate(3, 42, "Template", false)).thenReturn(created);

        controller.createTemplate(
                new CourseRestController.CourseTemplateRequest(3, "Template", false),
                authentication
        );

        verify(accessService).requireSubjectOwner(authentication, 3);
        verify(courseService).createTemplate(3, 42, "Template", false);

        assertThat(Arrays.stream(CourseRestController.CourseTemplateRequest.class.getRecordComponents())
                .map(component -> component.getName())
                .toList())
                .containsExactly("subjectId", "name", "publicVisible");
    }
}
