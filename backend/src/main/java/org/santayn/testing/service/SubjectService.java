package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.repository.CourseTemplateRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectMembershipRepository subjectMembershipRepository;
    private final CourseTemplateRepository courseTemplateRepository;
    private final LectureRepository lectureRepository;

    @Transactional(readOnly = true)
    public List<Subject> findAll() {
        return subjectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Subject get(Integer id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
    }

    @Transactional
    public Subject create(String name, String description) {
        String normalizedName = FacultyService.requireText(name, "Name");
        if (subjectRepository.existsByName(normalizedName)) {
            throw new AuthConflictException("Subject already exists: " + normalizedName);
        }

        Subject subject = new Subject();
        subject.setName(normalizedName);
        subject.setDescription(FacultyService.trimToNull(description));
        return subjectRepository.save(subject);
    }

    @Transactional
    public Subject update(Integer id, String name, String description) {
        Subject subject = get(id);
        if (name != null) {
            String normalizedName = FacultyService.requireText(name, "Name");
            if (!normalizedName.equals(subject.getName()) && subjectRepository.existsByName(normalizedName)) {
                throw new AuthConflictException("Subject already exists: " + normalizedName);
            }
            subject.setName(normalizedName);
        }
        subject.setDescription(FacultyService.trimToNull(description));
        return subject;
    }

    @Transactional
    public void delete(Integer id) {
        Subject subject = get(id);
        boolean hasMembershipHistory = subjectMembershipRepository.existsBySubjectId(id);
        boolean hasCourseTemplates = courseTemplateRepository.existsBySubjectId(id);
        boolean hasLectures = lectureRepository.existsBySubjectId(id);
        if (hasMembershipHistory || hasCourseTemplates || hasLectures) {
            throw new ResourceInUseException(
                    "subject",
                    id,
                    "Subject cannot be deleted while learning content or membership history references it.",
                    java.util.Map.of(
                            "subjectMemberships", hasMembershipHistory,
                            "courseTemplates", hasCourseTemplates,
                            "lectures", hasLectures
                    )
            );
        }
        subjectRepository.delete(subject);
    }
}
