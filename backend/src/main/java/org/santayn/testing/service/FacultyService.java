package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.repository.FacultyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FacultyService {

    private final FacultyRepository facultyRepository;

    @Transactional(readOnly = true)
    public List<Faculty> findAll() {
        return facultyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Faculty get(Integer id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + id));
    }

    @Transactional
    public Faculty create(String name, String code, String description) {
        String normalizedCode = normalizeCode(code);
        if (facultyRepository.existsByCode(normalizedCode)) {
            throw new AuthConflictException("Faculty code already exists: " + normalizedCode);
        }

        Faculty faculty = new Faculty();
        faculty.setName(requireText(name, "Name"));
        faculty.setCode(normalizedCode);
        faculty.setDescription(trimToNull(description));
        return facultyRepository.save(faculty);
    }

    @Transactional
    public Faculty update(Integer id, String name, String code, String description) {
        Faculty faculty = get(id);
        if (name != null) {
            faculty.setName(requireText(name, "Name"));
        }
        if (code != null) {
            String normalizedCode = normalizeCode(code);
            if (!normalizedCode.equals(faculty.getCode()) && facultyRepository.existsByCode(normalizedCode)) {
                throw new AuthConflictException("Faculty code already exists: " + normalizedCode);
            }
            faculty.setCode(normalizedCode);
        }
        faculty.setDescription(trimToNull(description));
        return faculty;
    }

    @Transactional
    public void delete(Integer id) {
        Faculty faculty = get(id);
        facultyRepository.delete(faculty);
    }

    private static String normalizeCode(String code) {
        return requireText(code, "Code").toLowerCase(Locale.ROOT);
    }

    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
