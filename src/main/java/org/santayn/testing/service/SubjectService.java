package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

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
        subjectRepository.delete(subject);
    }
}
