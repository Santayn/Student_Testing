package org.santayn.testing.service.ai;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Текст из материалов лекции: PDF (текстовый слой), DOCX, PPTX, TXT/MD. */
@Component
public class DocumentTextExtractor {

    /** @return текст или null, если формат не поддерживается или файл не читается */
    public String extract(Path file, String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(file.toFile())) {
                    return new PDFTextStripper().getText(document);
                }
            }
            if (name.endsWith(".docx")) {
                try (InputStream input = Files.newInputStream(file);
                     XWPFDocument document = new XWPFDocument(input);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    return extractor.getText();
                }
            }
            if (name.endsWith(".pptx")) {
                try (InputStream input = Files.newInputStream(file);
                     XMLSlideShow slideShow = new XMLSlideShow(input)) {
                    StringBuilder builder = new StringBuilder();
                    for (XSLFSlide slide : slideShow.getSlides()) {
                        for (XSLFShape shape : slide.getShapes()) {
                            if (shape instanceof XSLFTextShape textShape) {
                                builder.append(textShape.getText()).append('\n');
                            }
                        }
                        builder.append('\n');
                    }
                    return builder.toString();
                }
            }
            if (name.endsWith(".txt") || name.endsWith(".md")) {
                return Files.readString(file, StandardCharsets.UTF_8);
            }
        } catch (Exception error) {
            return null;
        }
        return null;
    }
}
