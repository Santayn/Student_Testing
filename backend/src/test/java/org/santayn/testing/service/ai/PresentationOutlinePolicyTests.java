package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PresentationOutlinePolicyTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void russianShareCountsOnlyLetters() {
        assertThat(PresentationOutlineService.russianShare("Диаграммы классов UML")).isGreaterThan(0.7);
        assertThat(PresentationOutlineService.russianShare("Seed Informatics Lecture")).isZero();
        assertThat(PresentationOutlineService.russianShare("123 — …")).isEqualTo(1.0);
    }

    @Test
    void imageQueryFromTitleKeepsOnlyItsHead() {
        assertThat(PresentationOutlineService.shortQuery(
                "Спецификация разрабатываемого ПО при использовании UML объединяет несколько моделей", 5))
                .isEqualTo("Спецификация разрабатываемого ПО при использовании");
        assertThat(PresentationOutlineService.shortQuery("Отладка: основные этапы", 5)).isEqualTo("Отладка");
        assertThat(PresentationOutlineService.shortQuery(
                "Class diagrams - the core of object-oriented programming methods", 4)).isEqualTo("Class diagrams");
    }

    @Test
    void autoPickedImageMustShareAWordWithTheQuery() {
        ImageSearchService.FoundImage truck = found("File:Chevrolet Silverado tow truck.jpg");
        ImageSearchService.FoundImage diagram = found("File:UML class diagram example.png");

        assertThat(ImageSearchService.matchesQuery(truck, "documentation and maintenance of software")).isFalse();
        assertThat(ImageSearchService.matchesQuery(diagram, "class diagrams")).isTrue();
        assertThat(ImageSearchService.matchesQuery(found("File:Avanti West Coast central train.jpg"),
                "Class diagrams central link")).isFalse();
    }

    private static ImageSearchService.FoundImage found(String title) {
        return new ImageSearchService.FoundImage("wikimedia", title, "https://upload.wikimedia.org/a.jpg", "", "", "", "");
    }

    @Test
    void firstHeadingSkipsSectionMarkersAndDecorations() {
        String source = "## Файл «ВвПИ ЛЕК 9.docx»\n-------ЛЕКЦИЯ 9. Спецификация разрабатываемого ПО--------\nТекст";

        assertThat(PresentationOutlineService.firstHeading(source)).isEqualTo("ЛЕКЦИЯ 9. Спецификация разрабатываемого ПО");
        assertThat(PresentationOutlineService.firstHeading("## Текст лекции\nIntroduction to UML")).isNull();
    }

    @Test
    void tableRetoldAsBulletsIsRestoredFromTheSource() throws Exception {
        String source = """
                ## Текст лекции
                Таблица 1 – Сравнение существующих решений

                | Решение | Промежуточное представление | Особенности |
                |---|---|---|
                | Roslyn | синтаксическое дерево, затем CIL | API для анализаторов |
                | ANTLR | дерево разбора | генератор LL(*)-анализаторов |
                """;
        ObjectNode outline = outline("""
                [{"layout": "bullets", "title": "4. Обзор существующих решений",
                  "bullets": ["Таблица 1 – Сравнение существующих решений",
                              "Решение | Промежуточное представление | Особенности"],
                  "image": {"mode": "search", "query": "compilers"}}]
                """);

        PresentationOutlineService.repairTables(outline, source);

        JsonNode slide = outline.path("slides").get(0);
        assertThat(slide.path("layout").asText()).isEqualTo("table");
        assertThat(slide.path("bullets")).isEmpty();
        assertThat(slide.path("image").path("mode").asText()).isEqualTo("none");
        assertThat(slide.path("table").path("columns").get(2).asText()).isEqualTo("Особенности");
        assertThat(slide.path("table").path("rows")).hasSize(2);
        assertThat(slide.path("table").path("rows").get(1).get(0).asText()).isEqualTo("ANTLR");
    }

    @Test
    void tableWithoutDataIsDroppedAndOtherBulletsGetTheirOwnSlide() throws Exception {
        ObjectNode outline = outline("""
                [{"layout": "bullets", "title": "Обзор", "bullets": ["Таблица 2 – Нет такой", "A | B"],
                  "image": {"mode": "none"}},
                 {"layout": "bullets", "title": "Модели", "image": {"mode": "none"},
                  "bullets": ["Есть несколько моделей", "Модель | Назначение", "Логическая | абстракции"]}]
                """);

        PresentationOutlineService.repairTables(outline, "Текст без таблиц");

        assertThat(outline.path("slides")).hasSize(2);
        JsonNode bullets = outline.path("slides").get(0);
        JsonNode table = outline.path("slides").get(1);
        assertThat(bullets.path("bullets")).hasSize(1);
        assertThat(table.path("layout").asText()).isEqualTo("table");
        assertThat(table.path("table").path("rows").get(0).get(1).asText()).isEqualTo("абстракции");
    }

    @Test
    void noneRemovesAllImages() throws Exception {
        ObjectNode outline = outline("""
                [{"layout": "bullets", "title": "Модели", "image": {"mode": "search", "query": "uml"}},
                 {"layout": "bullets", "title": "Отладка", "image": {"mode": "generate", "prompt": "жук"}}]
                """);

        PresentationOutlineService.applyImagePolicy(outline, "none", true);

        assertThat(modes(outline)).containsExactly("none", "none");
    }

    @Test
    void someAddsImagesToAboutAThirdOfSlidesWhenModelProposedNone() throws Exception {
        ObjectNode outline = outline(slides(6, "none"));

        PresentationOutlineService.applyImagePolicy(outline, "some", false);

        assertThat(modes(outline).stream().filter("search"::equals)).hasSize(2);
        JsonNode first = outline.path("slides").get(0).path("image");
        assertThat(first.path("mode").asText()).isEqualTo("search");
        assertThat(first.path("query").asText()).isEqualTo("Слайд 1");
    }

    @Test
    void manyFillsEveryTextSlideButNotSectionsOrProcess() throws Exception {
        ObjectNode outline = outline("""
                [{"layout": "section", "title": "Раздел", "image": {"mode": "none"}},
                 {"layout": "bullets", "title": "Тезисы", "image": {"mode": "none"}},
                 {"layout": "process", "title": "Шаги", "image": {"mode": "none"}},
                 {"layout": "two_columns", "title": "Сравнение", "image": {"mode": "none"}}]
                """);

        PresentationOutlineService.applyImagePolicy(outline, "many", false);

        assertThat(modes(outline)).containsExactly("none", "search", "none", "search");
    }

    @Test
    void generationWithoutImageModelBecomesSearch() throws Exception {
        ObjectNode outline = outline("""
                [{"layout": "bullets", "title": "Отладка программ", "image": {"mode": "generate", "prompt": "жук"}}]
                """);

        PresentationOutlineService.applyImagePolicy(outline, "some", false);

        JsonNode image = outline.path("slides").get(0).path("image");
        assertThat(image.path("mode").asText()).isEqualTo("search");
        assertThat(image.path("query").asText()).isEqualTo("Отладка программ");
    }

    private ObjectNode outline(String slides) throws Exception {
        ObjectNode outline = objectMapper.createObjectNode();
        outline.put("title", "Лекция");
        outline.set("slides", objectMapper.readTree(slides));
        return outline;
    }

    private static String slides(int count, String mode) {
        List<String> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            items.add("{\"layout\": \"bullets\", \"title\": \"Слайд " + i + "\", \"image\": {\"mode\": \"" + mode + "\"}}");
        }
        return "[" + String.join(",", items) + "]";
    }

    private static List<String> modes(JsonNode outline) {
        List<String> modes = new ArrayList<>();
        outline.path("slides").forEach(slide -> modes.add(slide.path("image").path("mode").asText()));
        return modes;
    }
}
