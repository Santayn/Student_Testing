package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.course.CourseTemplate;
import org.santayn.testing.models.course.CourseVersion;
import org.santayn.testing.repository.CourseTemplateRepository;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseTemplateRepository courseTemplateRepository;
    private final CourseVersionRepository courseVersionRepository;
    private final SubjectRepository subjectRepository;
    private final PersonRepository personRepository;

    @Transactional(readOnly = true)
    public List<CourseTemplate> findTemplates(Integer subjectId, Integer authorPersonId, boolean publicOnly) {
        return courseTemplateRepository.findByFilters(subjectId, authorPersonId, publicOnly);
    }

    @Transactional(readOnly = true)
    public CourseTemplate getTemplate(Integer id) {
        return courseTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course template not found: " + id));
    }

    @Transactional
    public CourseTemplate createTemplate(Integer subjectId, Integer authorPersonId, String name, boolean publicVisible) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new IllegalArgumentException("Subject not found: " + subjectId);
        }
        requirePerson(authorPersonId);

        String templateName = FacultyService.requireText(name, "Name");
        if (courseTemplateRepository.existsBySubjectIdAndName(subjectId, templateName)) {
            throw new AuthConflictException("Course template already exists for subject: " + subjectId + "/" + templateName);
        }

        CourseTemplate template = new CourseTemplate();
        template.setSubjectId(subjectId);
        template.setAuthorPersonId(authorPersonId);
        template.setName(templateName);
        template.setPublicVisible(publicVisible);
        template.setCreatedAt(Instant.now());
        return courseTemplateRepository.save(template);
    }

    @Transactional
    public CourseTemplate updateTemplate(Integer templateId,
                                         Integer subjectId,
                                         Integer authorPersonId,
                                         String name,
                                         boolean publicVisible) {
        CourseTemplate template = getTemplate(templateId);
        if (!subjectRepository.existsById(subjectId)) {
            throw new IllegalArgumentException("Subject not found: " + subjectId);
        }
        requirePerson(authorPersonId);

        String templateName = FacultyService.requireText(name, "Name");
        if (courseTemplateRepository.existsBySubjectIdAndNameAndIdNot(subjectId, templateName, templateId)) {
            throw new AuthConflictException("Course template already exists for subject: " + subjectId + "/" + templateName);
        }

        template.setSubjectId(subjectId);
        template.setAuthorPersonId(authorPersonId);
        template.setName(templateName);
        template.setPublicVisible(publicVisible);
        return template;
    }

    @Transactional
    public void deleteTemplate(Integer templateId) {
        CourseTemplate template = getTemplate(templateId);
        if (courseVersionRepository.findByCourseTemplateIdOrderByVersionNumberDesc(templateId).isEmpty()) {
            courseTemplateRepository.delete(template);
            return;
        }

        template.setPublicVisible(false);
    }

    @Transactional(readOnly = true)
    public List<CourseVersion> findVersions(Integer courseTemplateId) {
        return courseVersionRepository.findByCourseTemplateIdOrderByVersionNumberDesc(courseTemplateId);
    }

    @Transactional(readOnly = true)
    public CourseVersion getVersion(Integer id) {
        return courseVersionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course version not found: " + id));
    }

    @Transactional
    public CourseVersion createVersion(Integer courseTemplateId,
                                       int versionNumber,
                                       String title,
                                       String description,
                                       Integer createdByPersonId,
                                       boolean published,
                                       Integer publishedByPersonId,
                                       String changeNotes) {
        if (!courseTemplateRepository.existsById(courseTemplateId)) {
            throw new IllegalArgumentException("Course template not found: " + courseTemplateId);
        }
        requirePerson(createdByPersonId);
        if (published && publishedByPersonId != null) {
            requirePerson(publishedByPersonId);
        }
        int normalizedVersionNumber = Math.max(1, versionNumber);
        if (courseVersionRepository.existsByCourseTemplateIdAndVersionNumber(courseTemplateId, normalizedVersionNumber)) {
            throw new AuthConflictException("Course version already exists: " + courseTemplateId + "/" + normalizedVersionNumber);
        }

        CourseVersion version = new CourseVersion();
        version.setCourseTemplateId(courseTemplateId);
        version.setVersionNumber(normalizedVersionNumber);
        version.setTitle(FacultyService.requireText(title, "Title"));
        version.setDescription(FacultyService.trimToNull(description));
        version.setCreatedByPersonId(createdByPersonId);
        version.setPublished(published);
        if (published) {
            unpublishCurrentVersion(courseTemplateId);
            version.setPublishedAtUtc(Instant.now());
            version.setPublishedByPersonId(publishedByPersonIdOrCreator(publishedByPersonId, createdByPersonId));
        }
        version.setChangeNotes(FacultyService.trimToNull(changeNotes));
        return courseVersionRepository.save(version);
    }

    @Transactional
    public CourseVersion updateVersion(Integer versionId,
                                       int versionNumber,
                                       String title,
                                       String description,
                                       Integer createdByPersonId,
                                       String changeNotes) {
        CourseVersion version = getVersion(versionId);
        requirePerson(createdByPersonId);

        int normalizedVersionNumber = Math.max(1, versionNumber);
        if (courseVersionRepository.existsByCourseTemplateIdAndVersionNumberAndIdNot(
                version.getCourseTemplateId(),
                normalizedVersionNumber,
                versionId
        )) {
            throw new AuthConflictException("Course version already exists: " + version.getCourseTemplateId() + "/" + normalizedVersionNumber);
        }

        version.setVersionNumber(normalizedVersionNumber);
        version.setTitle(FacultyService.requireText(title, "Title"));
        version.setDescription(FacultyService.trimToNull(description));
        version.setCreatedByPersonId(createdByPersonId);
        version.setChangeNotes(FacultyService.trimToNull(changeNotes));
        return version;
    }

    @Transactional
    public CourseVersion publishVersion(Integer versionId, Integer publishedByPersonId) {
        CourseVersion version = courseVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Course version not found: " + versionId));
        requirePerson(publishedByPersonId);

        unpublishCurrentVersion(version.getCourseTemplateId());
        version.setPublished(true);
        version.setPublishedAtUtc(Instant.now());
        version.setPublishedByPersonId(publishedByPersonId);
        return version;
    }

    @Transactional
    public CourseVersion unpublishVersion(Integer versionId) {
        CourseVersion version = courseVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Course version not found: " + versionId));
        version.setPublished(false);
        version.setPublishedAtUtc(null);
        version.setPublishedByPersonId(null);
        return version;
    }

    private void requirePerson(Integer personId) {
        if (personId == null || !personRepository.existsById(personId)) {
            throw new IllegalArgumentException("Person not found: " + personId);
        }
    }

    private void unpublishCurrentVersion(Integer courseTemplateId) {
        courseVersionRepository.findByCourseTemplateIdAndPublishedTrue(courseTemplateId)
                .ifPresent(existing -> {
                    existing.setPublished(false);
                    existing.setPublishedAtUtc(null);
                    existing.setPublishedByPersonId(null);
                    courseVersionRepository.flush();
                });
    }

    private static Integer publishedByPersonIdOrCreator(Integer publishedByPersonId, Integer createdByPersonId) {
        return publishedByPersonId == null ? createdByPersonId : publishedByPersonId;
    }
}
