
package org.santayn.testing.service;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.StudentRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final StudentRepository studentRepository;

    public GroupService(GroupRepository groupRepository, StudentRepository studentRepository) {
        this.groupRepository = groupRepository;
        this.studentRepository = studentRepository;
    }

    public List<Group> getAllGroup() {
        return groupRepository.findAllGroups();
    }

    public Group getSpecificGroupByGroupName(String name) {
        List<Group> groups = groupRepository.findGroupByName(name);
        if (groups.isEmpty()) {
            throw new RuntimeException("Группа с именем " + name + " не найдена.");
        }
        Optional<Group> specificGroup = groups.stream()
                .filter(group -> group.getName().equals(name))
                .findFirst();
        return specificGroup.orElseThrow(() -> new RuntimeException(
                "Группа с именем " + name + " не найдена."));
    }

    public void save(Group group) {
        groupRepository.save(group);
    }
}