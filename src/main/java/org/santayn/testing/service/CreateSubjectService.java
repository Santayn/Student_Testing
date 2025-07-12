package org.santayn.testing.service;

import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CreateSubjectService {

    private final FacultyRepository facultyRepository;
    private final SubjectRepository subjectRepository;
    private final GroupRepository groupRepository;

    public CreateSubjectService(FacultyRepository facultyRepository, SubjectRepository subjectRepository, GroupRepository groupRepository) {
        this.facultyRepository = facultyRepository;
        this.subjectRepository = subjectRepository;
        this.groupRepository = groupRepository;
    }


    public Optional<Subject> getSubjectById(Integer subjectId) {
        return subjectRepository.findById(subjectId);
    }

    public void addSubject(String name, String description) {
        Subject subject = new Subject();
        subject.setName(name);
        subject.setDescription(description);
        subjectRepository.save(subject);
    }

    public void deleteSubject(Integer subjectId) {
        Optional<Subject> subject = subjectRepository.findById(subjectId);
        if (subject.isPresent()) {
            subjectRepository.deleteSubjectFacultyBySubjectId(subjectId);
            subjectRepository.deleteSubjectById(subjectId);
            subjectRepository.deleteTeacherSubjectBySubjectId(subjectId);
            subjectRepository.clearSubject_IdFromLecture(subjectId);
        } else {
            throw new RuntimeException("Group с subjectId " + subjectId + " не найден.");
        }
    }
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }
}