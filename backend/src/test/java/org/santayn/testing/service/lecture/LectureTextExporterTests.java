package org.santayn.testing.service.lecture;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LectureTextExporterTests {

    private static final String MARKDOWN = """
            ## Оперативная память
            ОЗУ хранит **данные** программ, пока компьютер *включён*. Подробнее: [вики](https://ru.wikipedia.org).

            - энергозависимая
            - быстрая

            | Память | Скорость |
            |---|---|
            | ОЗУ | высокая |

            > Программа загружается в оперативную память.

            ```
            int x = 1;
            ```
            Эмодзи 😀 не ломает PDF.
            """;

    private final LectureTextExporter exporter = new LectureTextExporter();

    @Test
    void docxContainsHeadingListTableAndLink() throws Exception {
        byte[] docx = exporter.toDocx("Лекция 1", MARKDOWN);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            assertThat(text).contains("Лекция 1", "Оперативная память", "энергозависимая", "int x = 1;");
            assertThat(document.getTables()).hasSize(1);
            assertThat(document.getTables().get(0).getCTTbl().getTblGrid().sizeOfGridColArray())
                    .isEqualTo(document.getTables().get(0).getRow(0).getTableCells().size());
            assertThat(document.getTables().get(0).getRow(1).getCell(0).getText()).isEqualTo("ОЗУ");
            assertThat(document.getParagraphs())
                    .filteredOn(paragraph -> paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetOutlineLvl())
                    .extracting(paragraph -> paragraph.getText())
                    .startsWith("Лекция 1");
        }
    }

    @Test
    void pdfEmbedsCyrillicTextAndSurvivesMissingGlyphs() throws Exception {
        byte[] pdf = exporter.toPdf("Лекция 1", MARKDOWN + "\n" + "Длинный абзац для переноса строк. ".repeat(200));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Лекция 1", "Оперативная память", "энергозависимая", "ОЗУ", "высокая");
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
        }
    }

    @Test
    void parserMatchesWebAndMobileBlocks() {
        List<MarkdownDocument.Block> blocks = MarkdownDocument.parse(MARKDOWN);

        assertThat(blocks).hasSize(7);
        assertThat(blocks.get(0)).isInstanceOf(MarkdownDocument.Heading.class);
        assertThat(blocks.get(2)).isInstanceOf(MarkdownDocument.ListBlock.class);
        assertThat(blocks.get(3)).isInstanceOf(MarkdownDocument.Table.class);
        assertThat(MarkdownDocument.parseInline("[x](javascript:alert(1))").get(0).url()).isNull();
    }
}
