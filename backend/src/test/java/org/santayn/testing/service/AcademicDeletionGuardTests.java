package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.CourseTemplateRepository;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.FacultyMembershipRepository;
import org.santayn.testing.repository.FacultyRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.LectureAssignmentRepository;
import org.santayn.testing.repository.LectureMaterialRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestQuestionSelectionRuleRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.repository.TopicRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicDeletionGuardTests {

    @Mock private FacultyRepository facultyRepository;
    @Mock private FacultyMembershipRepository facultyMembershipRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMembershipRepository groupMembershipRepository;
    @Mock private TeachingAssignmentRepository teachingAssignmentRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private SubjectMembershipRepository subjectMembershipRepository;
    @Mock private CourseTemplateRepository courseTemplateRepository;
    @Mock private LectureRepository lectureRepository;
    @Mock private CourseVersionRepository courseVersionRepository;
    @Mock private ActiveTeacherSubjectMembershipService activeTeacherSubjectMembershipService;
    @Mock private TestRepository testRepository;
    @Mock private LectureTestLinkService lectureTestLinkService;
    @Mock private LectureAssignmentRepository lectureAssignmentRepository;
    @Mock private LectureMaterialRepository lectureMaterialRepository;
    @Mock private TestAssignmentRepository testAssignmentRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private TestQuestionSelectionRuleRepository testQuestionSelectionRuleRepository;
    @Mock private TopicRepository topicRepository;

    @InjectMocks private FacultyService facultyService;
    @InjectMocks private GroupService groupService;
    @InjectMocks private SubjectService subjectService;
    @InjectMocks private LectureService lectureService;
    @InjectMocks private TopicService topicService;

    @Test
    void facultyWithGroupsCannotBePhysicallyDeleted() {
        Faculty faculty = new Faculty();
        faculty.setId(10);
        when(facultyRepository.findById(10)).thenReturn(Optional.of(faculty));
        when(groupRepository.existsByFacultyId(10)).thenReturn(true);

        assertThatThrownBy(() -> facultyService.delete(10))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be deleted");

        verify(facultyRepository, never()).delete(faculty);
    }

    @Test
    void groupWithMembershipHistoryCannotBePhysicallyDeleted() {
        Group group = new Group();
        group.setId(20);
        when(groupRepository.findById(20)).thenReturn(Optional.of(group));
        when(groupMembershipRepository.existsByGroupId(20)).thenReturn(true);

        assertThatThrownBy(() -> groupService.delete(20))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be deleted");

        verify(groupRepository, never()).delete(group);
    }

    @Test
    void subjectWithCourseContentCannotBePhysicallyDeleted() {
        Subject subject = new Subject();
        subject.setId(30);
        when(subjectRepository.findById(30)).thenReturn(Optional.of(subject));
        when(courseTemplateRepository.existsBySubjectId(30)).thenReturn(true);

        assertThatThrownBy(() -> subjectService.delete(30))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be deleted");

        verify(subjectRepository, never()).delete(subject);
    }

    @Test
    void lectureWithStoredMaterialCannotBePhysicallyDeleted() {
        Lecture lecture = new Lecture();
        lecture.setId(40);
        when(lectureRepository.findById(40)).thenReturn(Optional.of(lecture));
        when(lectureMaterialRepository.existsByCourseLectureId(40)).thenReturn(true);

        assertThatThrownBy(() -> lectureService.delete(40))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be deleted");

        verify(lectureRepository, never()).delete(lecture);
    }

    @Test
    void topicReferencedByQuestionCannotBePhysicallyDeleted() {
        Topic topic = new Topic();
        topic.setId(50);
        when(topicRepository.findById(50)).thenReturn(Optional.of(topic));
        when(questionRepository.existsByTopicId(50)).thenReturn(true);

        assertThatThrownBy(() -> topicService.delete(50))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be deleted");

        verify(topicRepository, never()).delete(topic);
    }
}
