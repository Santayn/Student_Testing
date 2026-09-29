package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.subject.FacultySubject;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.subject.SubjectMembershipLoadType;
import org.santayn.testing.models.teacher.TeachingAssignment;
import org.santayn.testing.models.teacher.TeachingLoadType;
import org.santayn.testing.repository.FacultyRepository;
import org.santayn.testing.repository.FacultySubjectRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.SubjectMembershipLoadTypeRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TeachingLoadTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeachingServiceIntegrationTests {

    @Autowired private FacultyRepository facultyRepository;
    @Autowired private FacultySubjectRepository facultySubjectRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private SubjectMembershipRepository subjectMembershipRepository;
    @Autowired private SubjectMembershipLoadTypeRepository subjectMembershipLoadTypeRepository;
    @Autowired private TeachingLoadTypeRepository teachingLoadTypeRepository;
    @Autowired private TeachingAssignmentRepository teachingAssignmentRepository;
    @Autowired private TeachingService teachingService;

    @Test
    void createAssignmentAllowsActiveTeacherMembership() {
        TeachingFixture fixture = teachingFixture(1, 1, null);

        TeachingAssignment assignment = teachingService.createAssignment(
                fixture.subjectMembership().getId(),
                fixture.group().getId(),
                fixture.loadType().getId(),
                null,
                1,
                1,
                2026,
                BigDecimal.ONE,
                1,
                null
        );

        assertThat(assignment.getId()).isNotNull();
        assertThat(assignment.getSubjectMembershipId()).isEqualTo(fixture.subjectMembership().getId());
    }

    @Test
    void createAssignmentRejectsInactiveTeacherMembership() {
        TeachingFixture fixture = teachingFixture(1, 2, null);

        assertThatThrownBy(() -> teachingService.createAssignment(
                fixture.subjectMembership().getId(),
                fixture.group().getId(),
                fixture.loadType().getId(),
                null,
                1,
                1,
                2026,
                BigDecimal.ONE,
                1,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void createAssignmentRejectsRemovedTeacherMembership() {
        TeachingFixture fixture = teachingFixture(1, 1, Instant.now());

        assertThatThrownBy(() -> teachingService.createAssignment(
                fixture.subjectMembership().getId(),
                fixture.group().getId(),
                fixture.loadType().getId(),
                null,
                1,
                1,
                2026,
                BigDecimal.ONE,
                1,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void updateAssignmentRejectsInactiveTargetMembership() {
        TeachingFixture activeFixture = teachingFixture(1, 1, null);
        TeachingAssignment assignment = teachingAssignment(activeFixture);
        TeachingFixture inactiveFixture = teachingFixture(1, 2, null);

        assertThatThrownBy(() -> teachingService.updateAssignment(
                assignment.getId(),
                inactiveFixture.subjectMembership().getId(),
                inactiveFixture.group().getId(),
                inactiveFixture.loadType().getId(),
                null,
                1,
                1,
                2026,
                BigDecimal.ONE,
                1,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void addSubjectLoadTypeRejectsNonTeacherMembership() {
        TeachingFixture fixture = teachingFixture(2, 1, null);

        assertThatThrownBy(() -> teachingService.addSubjectLoadType(
                fixture.subjectMembership().getId(),
                fixture.loadType().getId(),
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    private TeachingFixture teachingFixture(int subjectMembershipRole,
                                            int subjectMembershipStatus,
                                            Instant removedAtUtc) {
        long suffix = System.nanoTime();

        Faculty faculty = new Faculty();
        faculty.setName("Teaching Faculty " + suffix);
        faculty.setCode("TF-" + suffix);
        faculty.setDescription("");
        faculty = facultyRepository.saveAndFlush(faculty);

        Subject subject = new Subject();
        subject.setName("Teaching Subject " + suffix);
        subject.setDescription("");
        subject = subjectRepository.saveAndFlush(subject);

        FacultySubject facultySubject = new FacultySubject();
        facultySubject.setFacultyId(faculty.getId());
        facultySubject.setSubjectId(subject.getId());
        facultySubjectRepository.saveAndFlush(facultySubject);

        Group group = new Group();
        group.setName("Teaching Group " + suffix);
        group.setCode("TG-" + suffix);
        group.setFacultyId(faculty.getId());
        group = groupRepository.saveAndFlush(group);

        Person person = new Person();
        person.setFirstName("Teaching");
        person.setLastName("Teacher");
        person.setDateOfBirth(LocalDate.of(1980, 1, 1));
        person.setEmail("teaching-" + suffix + "@test.local");
        person.setPhone("");
        person = personRepository.saveAndFlush(person);

        SubjectMembership membership = new SubjectMembership();
        membership.setSubjectId(subject.getId());
        membership.setPersonId(person.getId());
        membership.setRole(subjectMembershipRole);
        membership.setStatus(subjectMembershipStatus);
        membership.setAssignedAtUtc(Instant.now());
        membership.setRemovedAtUtc(removedAtUtc);
        membership = subjectMembershipRepository.saveAndFlush(membership);

        TeachingLoadType loadType = new TeachingLoadType();
        loadType.setName("Teaching Load " + suffix);
        loadType.setDescription("");
        loadType = teachingLoadTypeRepository.saveAndFlush(loadType);

        SubjectMembershipLoadType subjectLoadType = new SubjectMembershipLoadType();
        subjectLoadType.setSubjectMembershipId(membership.getId());
        subjectLoadType.setTeachingLoadTypeId(loadType.getId());
        subjectLoadType.setStatus(1);
        subjectLoadType.setAssignedAtUtc(Instant.now());
        subjectMembershipLoadTypeRepository.saveAndFlush(subjectLoadType);

        return new TeachingFixture(subject, membership, group, loadType);
    }

    private TeachingAssignment teachingAssignment(TeachingFixture fixture) {
        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setSubjectMembershipId(fixture.subjectMembership().getId());
        assignment.setGroupId(fixture.group().getId());
        assignment.setLoadTypeId(fixture.loadType().getId());
        assignment.setSemester(1);
        assignment.setStudyCourse(1);
        assignment.setAcademicYear(2026);
        assignment.setHoursPerWeek(BigDecimal.ONE);
        assignment.setStatus(1);
        return teachingAssignmentRepository.saveAndFlush(assignment);
    }

    private record TeachingFixture(Subject subject,
                                   SubjectMembership subjectMembership,
                                   Group group,
                                   TeachingLoadType loadType) {
    }
}
