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
public class CreateGroupService {

    private final FacultyRepository facultyRepository;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final LectureService lectureService;

    public CreateGroupService(FacultyRepository facultyRepository,
                              GroupRepository groupRepository,
                              TeacherRepository teacherRepository,
                              LectureService lectureService) {
        this.facultyRepository = facultyRepository;
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.lectureService = lectureService;
    }


    public Optional<Faculty> getFacultyById(Integer facultyId) {
        return facultyRepository.findById(facultyId);
    }

    public void addGroup(String name, Integer facultyId) {
        Group group = new Group();
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Факультет не найден"));
        group.setName(name);
        group.setFaculty(faculty);
        groupRepository.save(group);
    }

    public void deleteGroup(Integer GroupId) {
        Optional<Group> group = groupRepository.findById(GroupId);
        if (group.isPresent()) {
            groupRepository.deleteGroupStudentByGroupId(GroupId);
            groupRepository.clearGroup_IdFromStudent(GroupId);
            groupRepository.deleteTeacherGroupByGroupId(GroupId);
            groupRepository.deleteGroupById(GroupId);
        } else {
            throw new RuntimeException("Group с ID " + GroupId + " не найден.");
        }
    }
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }
}