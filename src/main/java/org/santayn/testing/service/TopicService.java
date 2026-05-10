package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TopicRepository;
import org.santayn.testing.models.subject.SubjectMembership;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final LectureRepository lectureRepository;
    private final SubjectMembershipRepository subjectMembershipRepository;

    @Transactional(readOnly = true)
    public List<Topic> findAll(Integer subjectId, Integer courseLectureId, Integer subjectMembershipId) {
        if (subjectMembershipId != null) {
            SubjectMembership membership = requireSubjectMembership(subjectMembershipId);
            if (subjectId != null && !subjectId.equals(membership.getSubjectId())) {
                throw new IllegalArgumentException("Subject membership " + subjectMembershipId
                        + " does not belong to subject " + subjectId + ".");
            }
            return topicRepository.findBySubjectMembershipIdOrderByOrdinalAsc(subjectMembershipId);
        }
        if (courseLectureId != null) {
            LecturePlacement lecturePlacement = requireLecturePlacement(courseLectureId);
            if (subjectId != null && !subjectId.equals(lecturePlacement.subjectId())) {
                throw new IllegalArgumentException("Lecture " + courseLectureId
                        + " does not belong to subject " + subjectId + ".");
            }
            if (lecturePlacement.subjectMembershipId() != null) {
                return topicRepository.findBySubjectMembershipIdOrderByOrdinalAsc(lecturePlacement.subjectMembershipId());
            }
            return topicRepository.findBySubjectIdOrderByOrdinalAsc(lecturePlacement.subjectId());
        }
        if (subjectId != null) {
            requireSubjectExists(subjectId);
            return topicRepository.findBySubjectIdOrderByOrdinalAsc(subjectId);
        }
        return topicRepository.findAllByOrderBySubjectMembershipIdAscOrdinalAsc();
    }

    @Transactional(readOnly = true)
    public Topic get(Integer id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + id));
    }

    @Transactional
    public Topic create(Integer subjectId,
                        Integer courseLectureId,
                        Integer subjectMembershipId,
                        int ordinal,
                        String name,
                        String description) {
        TopicPlacement placement = requireTopicPlacement(subjectId, courseLectureId, subjectMembershipId);
        int normalizedOrdinal = Math.max(1, ordinal);
        if (topicRepository.existsBySubjectMembershipIdAndOrdinal(placement.subjectMembershipId(), normalizedOrdinal)) {
            throw new AuthConflictException("Teacher topic ordinal already exists: "
                    + placement.subjectMembershipId() + "/" + normalizedOrdinal);
        }

        Topic topic = new Topic();
        topic.setSubjectId(placement.subjectId());
        topic.setSubjectMembershipId(placement.subjectMembershipId());
        topic.setCourseLectureId(null);
        topic.setOrdinal(normalizedOrdinal);
        topic.setName(FacultyService.requireText(name, "Name"));
        topic.setDescription(FacultyService.trimToNull(description));
        return topicRepository.save(topic);
    }

    @Transactional
    public Topic update(Integer topicId,
                        Integer subjectId,
                        Integer courseLectureId,
                        Integer subjectMembershipId,
                        int ordinal,
                        String name,
                        String description) {
        Topic topic = get(topicId);
        TopicPlacement placement = requireTopicPlacement(subjectId, courseLectureId, subjectMembershipId);
        int normalizedOrdinal = Math.max(1, ordinal);
        if (topicRepository.existsBySubjectMembershipIdAndOrdinalAndIdNot(placement.subjectMembershipId(), normalizedOrdinal, topicId)) {
            throw new AuthConflictException("Teacher topic ordinal already exists: "
                    + placement.subjectMembershipId() + "/" + normalizedOrdinal);
        }

        topic.setSubjectId(placement.subjectId());
        topic.setSubjectMembershipId(placement.subjectMembershipId());
        topic.setCourseLectureId(null);
        topic.setOrdinal(normalizedOrdinal);
        topic.setName(FacultyService.requireText(name, "Name"));
        topic.setDescription(FacultyService.trimToNull(description));
        return topic;
    }

    @Transactional
    public void delete(Integer topicId) {
        topicRepository.delete(get(topicId));
    }

    private TopicPlacement requireTopicPlacement(Integer subjectId,
                                                 Integer courseLectureId,
                                                 Integer subjectMembershipId) {
        if (subjectMembershipId == null) {
            throw new IllegalArgumentException("SubjectMembershipId is required.");
        }
        SubjectMembership membership = requireSubjectMembership(subjectMembershipId);

        if (subjectId != null && !subjectId.equals(membership.getSubjectId())) {
            throw new IllegalArgumentException("Subject membership " + subjectMembershipId
                    + " does not belong to subject " + subjectId + ".");
        }

        if (courseLectureId != null) {
            LecturePlacement lecturePlacement = requireLecturePlacement(courseLectureId);
            if (!lecturePlacement.subjectId().equals(membership.getSubjectId())) {
                throw new IllegalArgumentException("Lecture " + courseLectureId
                        + " does not belong to subject membership " + subjectMembershipId + ".");
            }
            if (lecturePlacement.subjectMembershipId() != null
                    && !lecturePlacement.subjectMembershipId().equals(subjectMembershipId)) {
                throw new IllegalArgumentException("Lecture " + courseLectureId
                        + " belongs to another teacher subject membership.");
            }
        }

        requireSubjectExists(membership.getSubjectId());
        return new TopicPlacement(membership.getSubjectId(), membership.getId(), null);
    }

    private SubjectMembership requireSubjectMembership(Integer subjectMembershipId) {
        return subjectMembershipRepository.findById(subjectMembershipId)
                .orElseThrow(() -> new IllegalArgumentException("Subject membership not found: " + subjectMembershipId));
    }

    private LecturePlacement requireLecturePlacement(Integer courseLectureId) {
        var lecture = lectureRepository.findById(courseLectureId)
                .orElseThrow(() -> new IllegalArgumentException("Course lecture not found: " + courseLectureId));
        Integer lectureSubjectId = lecture.getSubjectId();
        if (lectureSubjectId == null) {
            lectureSubjectId = lectureRepository.findSubjectIdByLectureId(courseLectureId)
                    .orElseThrow(() -> new IllegalArgumentException("Course lecture not found: " + courseLectureId));
        }
        return new LecturePlacement(lectureSubjectId, lecture.getSubjectMembershipId());
    }

    private void requireSubjectExists(Integer subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new IllegalArgumentException("Subject not found: " + subjectId);
        }
    }

    private record TopicPlacement(Integer subjectId, Integer subjectMembershipId, Integer courseLectureId) {
    }

    private record LecturePlacement(Integer subjectId, Integer subjectMembershipId) {
    }
}
