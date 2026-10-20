package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PptxPresentationBuilderTests {

    @Test
    void buildsAllLayoutsWithImageNotesAndPdf() throws Exception {
        JsonNode outline = new ObjectMapper().readTree("""
                {"title": "Реляционные базы данных", "subtitle": "Лекция 3",
                 "slides": [
                   {"layout": "section", "title": "Основы"},
                   {"layout": "bullets", "title": "Что такое СУБД", "bullets": ["Хранит данные", "Обеспечивает доступ"],
                    "notes": "Расскажите о истории."},
                   {"layout": "two_columns", "title": "SQL и NoSQL",
                    "left": {"title": "SQL", "bullets": ["Схема", "Транзакции"]},
                    "right": {"title": "NoSQL", "bullets": ["Гибкость"]}},
                   {"layout": "process", "title": "Запрос", "steps": ["Разбор", "План", "Выполнение"]},
                   {"layout": "quote", "title": "Определение", "bullets": ["Отношение — это таблица"]}
                 ]}
                """);
        PptxPresentationBuilder.SlideImage image = new PptxPresentationBuilder.SlideImage(png(), "Схема — Автор, CC BY-SA 4.0");

        PptxPresentationBuilder.Result result = PptxPresentationBuilder.build(outline, Map.of(1, image), true);

        assertThat(result.slideCount()).isEqualTo(6);
        assertThat(new String(result.pdf(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(result.pptx()))) {
            assertThat(show.getSlides()).hasSize(6);
            assertThat(slideText(show.getSlides().get(0))).contains("Реляционные базы данных", "Лекция 3");
            XSLFSlide bullets = show.getSlides().get(2);
            assertThat(slideText(bullets)).contains("Что такое СУБД", "Хранит данные");
            assertThat(bullets.getShapes()).anyMatch(XSLFPictureShape.class::isInstance);
            assertThat(notesText(show, bullets)).contains("Расскажите о истории.", "Изображение: Схема — Автор, CC BY-SA 4.0");
            assertThat(slideText(show.getSlides().get(4))).contains("1. Разбор", "3. Выполнение");
        }
    }

    @Test
    void shrinksLongTitles() {
        assertThat(PptxPresentationBuilder.fitSize("Коротко", 30, 20, 70)).isEqualTo(30);
        assertThat(PptxPresentationBuilder.fitSize("Очень ".repeat(40), 30, 20, 70)).isEqualTo(20);
    }

    private static String slideText(XSLFSlide slide) {
        StringBuilder builder = new StringBuilder();
        slide.getShapes().forEach(shape -> {
            if (shape instanceof XSLFTextShape text) {
                builder.append(text.getText()).append('\n');
            }
        });
        return builder.toString();
    }

    private static String notesText(XMLSlideShow show, XSLFSlide slide) {
        XSLFNotes notes = show.getNotesSlide(slide);
        for (XSLFTextShape shape : notes.getPlaceholders()) {
            if (shape.getTextType() == Placeholder.BODY) {
                return shape.getText();
            }
        }
        return "";
    }

    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
