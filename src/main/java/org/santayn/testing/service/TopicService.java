package org.santayn.testing.service;

import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeacherRepository;
import org.santayn.testing.repository.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public TopicService(TopicRepository topicRepository,
                        SubjectRepository subjectRepository,
                        TeacherRepository teacherRepository) {
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
    }


    public List<Topic> getTopicsBySubjectId(Integer subjectId) {
        return topicRepository.findBySubjectId(subjectId);
    }

    public Topic addTopic(Integer subjectId, String name) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Предмет не найден"));
        Topic topic = new Topic();
        topic.setName(name);
        topic.setSubject(subject);
        return topicRepository.save(topic);
    }


    public void deleteTopic(Integer topicId) {
        topicRepository.deleteById(topicId);
    }

}