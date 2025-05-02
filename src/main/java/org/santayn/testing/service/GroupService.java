
package org.santayn.testing.service;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }
    public List<Group> getAllGroup() {
        return groupRepository.findAllGroups();
    }


    public Group getSpecificGroupByGroupName(String name) {

        List<Group> groups = groupRepository.findGroupByName(name);
        Optional<Group> specificGroup = groups.stream()
                .filter(group -> group.getName().equals(name))
                .findFirst();

        return specificGroup.orElseThrow(() -> new RuntimeException(
                "Question with ID " + name + " not found in Test with ID "));
    }
}