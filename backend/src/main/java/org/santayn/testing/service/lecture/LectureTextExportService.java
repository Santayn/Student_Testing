package org.santayn.testing.service.lecture;

import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.lecture.LectureMaterial;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.service.LectureMaterialService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Текст лекции в Word или PDF: скачать или сохранить в материалы лекции. */
@Service
public class LectureTextExportService {

    private static final String DOCX_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final LectureRepository lectureRepository;
    private final LectureTextExporter exporter;
    private final LectureMaterialService lectureMaterialService;

    public LectureTextExportService(LectureRepository lectureRepository,
                                    LectureTextExporter exporter,
                                    LectureMaterialService lectureMaterialService) {
        this.lectureRepository = lectureRepository;
        this.exporter = exporter;
        this.lectureMaterialService = lectureMaterialService;
    }

    @Transactional(readOnly = true)
    public ExportedFile export(Integer lectureId, String format) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture not found: " + lectureId));
        String body = lecture.getBody();
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("У лекции нет текста — сначала заполните его.");
        }
        String title = exportTitle(lecture.getTitle());
        String baseName = safeFileName(title);
        return switch (normalizeFormat(format)) {
            case "pdf" -> new ExportedFile(exporter.toPdf(title, body), baseName + ".pdf", MediaType.APPLICATION_PDF_VALUE);
            default -> new ExportedFile(exporter.toDocx(title, body), baseName + ".docx", DOCX_TYPE);
        };
    }

    /** Сохраняет текст лекции файлом в её материалы — студенты скачают его вместе с остальными, в т. ч. без сети. */
    @Transactional
    public LectureMaterial saveAsMaterial(Integer lectureId, String format) {
        ExportedFile file = export(lectureId, format);
        return lectureMaterialService.saveGenerated(lectureId, file.fileName(), file.contentType(), file.content());
    }

    public static ResponseEntity<byte[]> download(ExportedFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.content().length)
                .body(file.content());
    }

    static String normalizeFormat(String format) {
        String value = format == null ? "" : format.trim().toLowerCase(Locale.ROOT);
        if (!value.equals("pdf") && !value.equals("docx")) {
            throw new IllegalArgumentException("Формат должен быть docx или pdf.");
        }
        return value;
    }

    /** Файл материалов — выгрузка текста этой же лекции (повторно читать его как источник незачем). */
    public static boolean isTextExportOf(String lectureTitle, String fileName) {
        if (fileName == null) {
            return false;
        }
        String baseName = safeFileName(exportTitle(lectureTitle));
        return fileName.equalsIgnoreCase(baseName + ".pdf") || fileName.equalsIgnoreCase(baseName + ".docx");
    }

    private static String exportTitle(String title) {
        return title == null || title.isBlank() ? "Лекция" : title.trim();
    }

    static String safeFileName(String value) {
        String cleaned = value.replaceAll("[\\\\/:*?\"<>|]", " ").replaceAll("\\s+", " ").trim();
        return cleaned.length() > 100 ? cleaned.substring(0, 100).trim() : cleaned;
    }

    public record ExportedFile(byte[] content, String fileName, String contentType) {
    }
}
