package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.lecture.LectureAssignment;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.teacher.TeachingAssignment;
import org.santayn.testing.models.teacher.TeachingLoadType;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.FacultyRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.LectureAssignmentRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TeachingLoadTypeRepository;
import org.santayn.testing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TeacherWorkloadIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private PersonRepository personRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FacultyRepository facultyRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private SubjectMembershipRepository subjectMembershipRepository;
    @Autowired private TeachingLoadTypeRepository teachingLoadTypeRepository;
    @Autowired private TeachingAssignmentRepository teachingAssignmentRepository;
    @Autowired private LectureRepository lectureRepository;
    @Autowired private LectureAssignmentRepository lectureAssignmentRepository;

    @Test
    void workloadReturnsCurrentTeacherPeriodInOneAggregate() throws Exception {
        String suffix = Long.toUnsignedString(System.nanoTime());

        Person teacher = new Person();
        teacher.setFirstName("Teacher");
        teacher.setLastName("Workload");
        teacher.setDateOfBirth(LocalDate.of(1990, 1, 1));
        teacher.setEmail("teacher-workload-" + suffix + "@test.local");
        teacher.setPhone("");
        teacher = personRepository.saveAndFlush(teacher);

        String login = "teacher-workload-" + suffix;
        User userEntity = new User();
        userEntity.setLogin(login);
        userEntity.setPasswordHash("test-password-hash");
        userEntity.setActive(true);
        userEntity.setPersonId(teacher.getId());
        userRepository.saveAndFlush(userEntity);

        Faculty faculty = new Faculty();
        faculty.setName("Faculty " + suffix);
        faculty.setCode("WF-" + suffix);
        faculty = facultyRepository.saveAndFlush(faculty);

        Group group = new Group();
        group.setName("Workload group " + suffix);
        group.setCode("WG-" + suffix);
        group.setFacultyId(faculty.getId());
        group = groupRepository.saveAndFlush(group);

        Subject subject = new Subject();
        subject.setName("Workload subject " + suffix);
        subject = subjectRepository.saveAndFlush(subject);

        SubjectMembership membership = new SubjectMembership();
        membership.setSubjectId(subject.getId());
        membership.setPersonId(teacher.getId());
        membership.setRole(1);
        membership.setStatus(1);
        membership = subjectMembershipRepository.saveAndFlush(membership);

        TeachingLoadType loadType = new TeachingLoadType();
        loadType.setName("Workload lectures " + suffix);
        loadType = teachingLoadTypeRepository.saveAndFlush(loadType);

        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setSubjectMembershipId(membership.getId());
        assignment.setGroupId(group.getId());
        assignment.setLoadTypeId(loadType.getId());
        assignment.setStudyCourse(1);
        assignment.setSemester(1);
        assignment.setAcademicYear(2026);
        assignment.setHoursPerWeek(BigDecimal.valueOf(2));
        assignment.setStatus(1);
        assignment = teachingAssignmentRepository.saveAndFlush(assignment);

        Lecture lecture = new Lecture();
        lecture.setSubjectId(subject.getId());
        lecture.setSubjectMembershipId(membership.getId());
        lecture.setOrdinal(1);
        lecture.setTitle("Workload lecture " + suffix);
        lecture.setContentFolderKey("workload-lecture-" + suffix);
        lecture = lectureRepository.saveAndFlush(lecture);

        LectureAssignment lectureAssignment = new LectureAssignment();
        lectureAssignment.setTeachingAssignmentId(assignment.getId());
        lectureAssignment.setCourseLectureId(lecture.getId());
        lectureAssignment.setRequired(true);
        lectureAssignment.setMinProgressPercent(100);
        lectureAssignment.setStatus(1);
        lectureAssignment.setCreatedAtUtc(Instant.now());
        lectureAssignment.setSnapshotGroupId(group.getId());
        lectureAssignment.setSnapshotTeacherPersonId(teacher.getId());
        lectureAssignment.setSnapshotSemester(1);
        lectureAssignment.setSnapshotAcademicYear(2026);
        lectureAssignmentRepository.saveAndFlush(lectureAssignment);

        mockMvc.perform(get("/api/v1/teaching/workload")
                        .param("studyCourse", "1")
                        .param("semester", "1")
                        .param("academicYear", "2026")
                        .with(user(login).authorities(new SimpleGrantedAuthority("teaching.manage"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments[0].assignment.id").value(assignment.getId()))
                .andExpect(jsonPath("$.assignments[0].groupName").value(group.getName()))
                .andExpect(jsonPath("$.assignments[0].groupCode").value(group.getCode()))
                .andExpect(jsonPath("$.assignments[0].lectureAssignments[0].courseLectureId").value(lecture.getId()))
                .andExpect(jsonPath("$.subjects[0].subjectId").value(subject.getId()))
                .andExpect(jsonPath("$.subjects[0].subjectMembershipId").value(membership.getId()))
                .andExpect(jsonPath("$.subjects[0].lectures[0].id").value(lecture.getId()));
    }

    @Test
    void profileContextReturnsTeacherSubjectsAndGroupsInOneAggregate() throws Exception {
        String suffix = Long.toUnsignedString(System.nanoTime());

        Person teacher = new Person();
        teacher.setFirstName("Teacher");
        teacher.setLastName("Profile");
        teacher.setDateOfBirth(LocalDate.of(1990, 1, 1));
        teacher.setEmail("teacher-profile-" + suffix + "@test.local");
        teacher.setPhone("");
        teacher = personRepository.saveAndFlush(teacher);

        String login = "teacher-profile-" + suffix;
        User userEntity = new User();
        userEntity.setLogin(login);
        userEntity.setPasswordHash("test-password-hash");
        userEntity.setActive(true);
        userEntity.setPersonId(teacher.getId());
        userRepository.saveAndFlush(userEntity);

        Faculty faculty = new Faculty();
        faculty.setName("Profile faculty " + suffix);
        faculty.setCode("PF-" + suffix);
        faculty = facultyRepository.saveAndFlush(faculty);

        Group group = new Group();
        group.setName("Profile group " + suffix);
        group.setCode("PG-" + suffix);
        group.setFacultyId(faculty.getId());
        group = groupRepository.saveAndFlush(group);

        Subject subject = new Subject();
        subject.setName("Profile subject " + suffix);
        subject = subjectRepository.saveAndFlush(subject);

        SubjectMembership membership = new SubjectMembership();
        membership.setSubjectId(subject.getId());
        membership.setPersonId(teacher.getId());
        membership.setRole(1);
        membership.setStatus(1);
        membership = subjectMembershipRepository.saveAndFlush(membership);

        TeachingLoadType loadType = new TeachingLoadType();
        loadType.setName("Profile lectures " + suffix);
        loadType = teachingLoadTypeRepository.saveAndFlush(loadType);

        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setSubjectMembershipId(membership.getId());
        assignment.setGroupId(group.getId());
        assignment.setLoadTypeId(loadType.getId());
        assignment.setStudyCourse(1);
        assignment.setSemester(1);
        assignment.setAcademicYear(2026);
        assignment.setHoursPerWeek(BigDecimal.ONE);
        assignment.setStatus(1);
        teachingAssignmentRepository.saveAndFlush(assignment);

        mockMvc.perform(get("/api/v1/teaching/profile-context")
                        .with(user(login).authorities(new SimpleGrantedAuthority("teaching.manage"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects[0].id").value(subject.getId()))
                .andExpect(jsonPath("$.groups[0].id").value(group.getId()));
    }

    @Test
    void workloadRequiresTeachingManageAuthority() throws Exception {
        mockMvc.perform(get("/api/v1/teaching/workload")
                        .param("studyCourse", "1")
                        .param("semester", "1")
                        .param("academicYear", "2026")
                        .with(user("student-only").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }
}
