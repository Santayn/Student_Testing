package org.santayn.testing.service.lecture;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.santayn.testing.service.lecture.MarkdownDocument.Block;
import org.santayn.testing.service.lecture.MarkdownDocument.Span;
import org.santayn.testing.service.lecture.MarkdownDocument.Style;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Текст лекции (Markdown) → DOCX или PDF: преподаватели привыкли к Word и PDF, а не к .md.
 * В PDF встроены шрифты DejaVu (кириллица одинаково выглядит на любом компьютере и телефоне).
 */
@Component
public class LectureTextExporter {

    public byte[] toDocx(String title, String markdown) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setSpacingAfter(240);
            outlineLevel(titleParagraph, 0);
            run(titleParagraph, title, Style.BOLD, 20, null);
            for (Block block : MarkdownDocument.parse(markdown)) {
                appendDocx(document, block);
            }
            document.write(output);
            return output.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private void appendDocx(XWPFDocument document, Block block) {
        if (block instanceof MarkdownDocument.Heading heading) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setSpacingBefore(240);
            paragraph.setSpacingAfter(120);
            int size = heading.level() <= 2 ? 16 : heading.level() == 3 ? 14 : 12;
            outlineLevel(paragraph, Math.max(1, heading.level() - 1));
            heading.spans().forEach(span -> run(paragraph, span.text(), Style.BOLD, size, null));
        } else if (block instanceof MarkdownDocument.Paragraph text) {
            spans(document.createParagraph(), text.spans(), 11);
        } else if (block instanceof MarkdownDocument.ListBlock list) {
            for (int index = 0; index < list.items().size(); index++) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.setIndentationLeft(540);
                paragraph.setIndentationHanging(270);
                paragraph.setSpacingAfter(60);
                run(paragraph, list.ordered() ? (index + 1) + ".\t" : "•\t", Style.PLAIN, 11, null);
                spans(paragraph, list.items().get(index), 11);
            }
        } else if (block instanceof MarkdownDocument.Quote quote) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setIndentationLeft(360);
            paragraph.setBorderLeft(Borders.SINGLE);
            quote.spans().forEach(span -> run(paragraph, span.text(), Style.ITALIC, 11, null));
        } else if (block instanceof MarkdownDocument.Code code) {
            XWPFParagraph paragraph = document.createParagraph();
            String[] lines = code.text().split("\n", -1);
            for (int index = 0; index < lines.length; index++) {
                XWPFRun run = run(paragraph, lines[index], Style.CODE, 10, null);
                if (index < lines.length - 1) {
                    run.addBreak();
                }
            }
        } else if (block instanceof MarkdownDocument.Table table) {
            int columns = table.header().size();
            XWPFTable docxTable = document.createTable(table.rows().size() + 1, columns);
            docxTable.setWidth("100%");
            fillRow(docxTable, 0, table.header(), true);
            for (int row = 0; row < table.rows().size(); row++) {
                fillRow(docxTable, row + 1, table.rows().get(row), false);
            }
            document.createParagraph();
        } else if (block instanceof MarkdownDocument.Rule ignored) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setBorderBottom(Borders.SINGLE);
        }
    }

    /** Уровень структуры: заголовки видны в области навигации Word и попадают в оглавление. */
    private static void outlineLevel(XWPFParagraph paragraph, int level) {
        CTPPr properties = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        properties.addNewOutlineLvl().setVal(BigInteger.valueOf(level));
    }

    private void fillRow(XWPFTable table, int rowIndex, List<List<Span>> cells, boolean header) {
        for (int column = 0; column < table.getRow(rowIndex).getTableCells().size(); column++) {
            XWPFTableCell cell = table.getRow(rowIndex).getCell(column);
            XWPFParagraph paragraph = cell.getParagraphs().get(0);
            List<Span> spans = column < cells.size() ? cells.get(column) : List.of();
            for (Span span : spans) {
                run(paragraph, span.text(), header ? Style.BOLD : span.style(), 10, span.url());
            }
            if (header) {
                cell.setColor("EEF2FA");
            }
        }
    }

    private void spans(XWPFParagraph paragraph, List<Span> spans, int size) {
        paragraph.setSpacingAfter(120);
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        spans.forEach(span -> run(paragraph, span.text(), span.style(), size, span.url()));
    }

    private XWPFRun run(XWPFParagraph paragraph, String text, Style style, int size, String url) {
        XWPFRun run = url == null ? paragraph.createRun() : paragraph.createHyperlinkRun(url);
        run.setText(text);
        run.setFontSize(size);
        run.setFontFamily(style == Style.CODE ? "Courier New" : "Arial");
        run.setBold(style == Style.BOLD);
        run.setItalic(style == Style.ITALIC);
        if (url != null) {
            run.setColor("2E54DC");
            run.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
        }
        return run;
    }

    public byte[] toPdf(String title, String markdown) {
        try (PDDocument document = new PDDocument()) {
            PdfLayout layout = new PdfLayout(document);
            layout.paragraph(List.of(new Span(title, Style.BOLD, null)), 20, 0, Color.BLACK, 10);
            for (Block block : MarkdownDocument.parse(markdown)) {
                layout.block(block);
            }
            layout.close();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    /** Простая вёрстка A4: перенос по словам, новые страницы, таблицы сеткой. */
    private static final class PdfLayout {
        private static final float MARGIN = 56;
        private static final PDRectangle PAGE = PDRectangle.A4;
        private static final float WIDTH = PAGE.getWidth() - 2 * MARGIN;
        private static final Color TEXT = new Color(0x1B2A4A);
        private static final Color MUTED = new Color(0x5B6B8C);
        private static final Color LINK = new Color(0x2E54DC);
        private static final Color SOFT = new Color(0xF1F4FA);

        private final PDDocument document;
        private final PDFont regular;
        private final PDFont bold;
        private final PDFont mono;
        private final Map<PDFont, Map<Integer, Boolean>> glyphs = new HashMap<>();
        private PDPageContentStream content;
        private float y;

        PdfLayout(PDDocument document) throws IOException {
            this.document = document;
            this.regular = font("fonts/DejaVuSans.ttf");
            this.bold = font("fonts/DejaVuSans-Bold.ttf");
            this.mono = font("fonts/DejaVuSansMono.ttf");
            newPage();
        }

        private PDFont font(String resource) throws IOException {
            try (InputStream input = LectureTextExporter.class.getClassLoader().getResourceAsStream(resource)) {
                if (input == null) {
                    throw new IOException("Font not found: " + resource);
                }
                return PDType0Font.load(document, input);
            }
        }

        void block(Block block) throws IOException {
            if (block instanceof MarkdownDocument.Heading heading) {
                float size = heading.level() <= 2 ? 16 : heading.level() == 3 ? 14 : 12;
                space(10);
                paragraph(bolden(heading.spans()), size, 0, TEXT, 6);
            } else if (block instanceof MarkdownDocument.Paragraph text) {
                paragraph(text.spans(), 11, 0, TEXT, 8);
            } else if (block instanceof MarkdownDocument.ListBlock list) {
                for (int index = 0; index < list.items().size(); index++) {
                    String marker = list.ordered() ? (index + 1) + "." : "•";
                    ensure(16);
                    write(marker, regular, 11, MARGIN + 4, y - 11, list.ordered() ? TEXT : LINK);
                    paragraph(list.items().get(index), 11, 20, TEXT, 4);
                }
                space(4);
            } else if (block instanceof MarkdownDocument.Quote quote) {
                float top = y;
                paragraph(quote.spans(), 11, 14, MUTED, 8);
                content.setStrokingColor(LINK);
                content.setLineWidth(2);
                line(MARGIN + 3, Math.min(top, PAGE.getHeight() - MARGIN), MARGIN + 3, y + 8);
            } else if (block instanceof MarkdownDocument.Code code) {
                for (String raw : code.text().split("\n", -1)) {
                    for (String line : wrapChars(raw, mono, 9, WIDTH - 16)) {
                        ensure(13);
                        content.setNonStrokingColor(SOFT);
                        content.addRect(MARGIN, y - 13, WIDTH, 13);
                        content.fill();
                        write(line, mono, 9, MARGIN + 8, y - 10, TEXT);
                        y -= 13;
                    }
                }
                space(8);
            } else if (block instanceof MarkdownDocument.Table table) {
                table(table);
            } else if (block instanceof MarkdownDocument.Rule ignored) {
                ensure(12);
                content.setStrokingColor(new Color(0xD5DBE8));
                content.setLineWidth(1);
                line(MARGIN, y - 6, MARGIN + WIDTH, y - 6);
                y -= 12;
            }
        }

        void paragraph(List<Span> spans, float size, float indent, Color color, float after) throws IOException {
            float leading = size * 1.45f;
            for (List<Word> line : wrap(spans, size, WIDTH - indent)) {
                ensure(leading);
                float x = MARGIN + indent;
                for (Word word : line) {
                    write(word.text(), word.font(), size, x, y - size, word.url() == null ? color : LINK);
                    x += word.width();
                }
                y -= leading;
            }
            y -= after;
        }

        private void table(MarkdownDocument.Table table) throws IOException {
            int columns = Math.max(1, table.header().size());
            float columnWidth = WIDTH / columns;
            List<List<List<Span>>> rows = new ArrayList<>();
            rows.add(table.header());
            rows.addAll(table.rows());
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                List<List<Span>> row = rows.get(rowIndex);
                boolean header = rowIndex == 0;
                List<List<List<Word>>> cells = new ArrayList<>();
                int lines = 1;
                for (int column = 0; column < columns; column++) {
                    List<Span> cell = column < row.size() ? row.get(column) : List.of();
                    List<List<Word>> wrapped = wrap(header ? bolden(cell) : cell, 10, columnWidth - 10);
                    cells.add(wrapped);
                    lines = Math.max(lines, wrapped.size());
                }
                float height = lines * 14 + 8;
                ensure(height);
                if (header) {
                    content.setNonStrokingColor(SOFT);
                    content.addRect(MARGIN, y - height, WIDTH, height);
                    content.fill();
                }
                for (int column = 0; column < columns; column++) {
                    float lineY = y - 4;
                    for (List<Word> line : cells.get(column)) {
                        float x = MARGIN + column * columnWidth + 5;
                        for (Word word : line) {
                            write(word.text(), word.font(), 10, x, lineY - 10, TEXT);
                            x += word.width();
                        }
                        lineY -= 14;
                    }
                }
                content.setStrokingColor(new Color(0xD5DBE8));
                content.setLineWidth(0.7f);
                content.addRect(MARGIN, y - height, WIDTH, height);
                content.stroke();
                for (int column = 1; column < columns; column++) {
                    line(MARGIN + column * columnWidth, y, MARGIN + column * columnWidth, y - height);
                }
                y -= height;
            }
            space(10);
        }

        private List<Span> bolden(List<Span> spans) {
            return spans.stream().map(span -> new Span(span.text(), Style.BOLD, span.url())).toList();
        }

        private List<List<Word>> wrap(List<Span> spans, float size, float width) throws IOException {
            List<List<Word>> lines = new ArrayList<>();
            List<Word> current = new ArrayList<>();
            float used = 0;
            for (Span span : spans) {
                PDFont font = span.style() == Style.BOLD ? bold : span.style() == Style.CODE ? mono : regular;
                for (String token : span.text().split("(?<=\\s)")) {
                    String text = printable(token, font);
                    if (text.isEmpty()) {
                        continue;
                    }
                    float tokenWidth = font.getStringWidth(text) / 1000 * size;
                    if (used + tokenWidth > width && !current.isEmpty()) {
                        lines.add(current);
                        current = new ArrayList<>();
                        used = 0;
                        text = text.stripLeading();
                        tokenWidth = font.getStringWidth(text) / 1000 * size;
                    }
                    if (tokenWidth > width) {
                        // Слово длиннее строки (URL, формула) — режем по символам.
                        for (String piece : wrapChars(text, font, size, width)) {
                            if (!current.isEmpty()) {
                                lines.add(current);
                            }
                            current = new ArrayList<>(List.of(new Word(piece, font, font.getStringWidth(piece) / 1000 * size, span.url())));
                            used = current.get(0).width();
                        }
                        continue;
                    }
                    current.add(new Word(text, font, tokenWidth, span.url()));
                    used += tokenWidth;
                }
            }
            if (!current.isEmpty()) {
                lines.add(current);
            }
            if (lines.isEmpty()) {
                lines.add(List.of());
            }
            return lines;
        }

        private List<String> wrapChars(String text, PDFont font, float size, float width) throws IOException {
            String value = printable(text, font);
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (int offset = 0; offset < value.length(); ) {
                int codePoint = value.codePointAt(offset);
                String next = current + new String(Character.toChars(codePoint));
                if (font.getStringWidth(next) / 1000 * size > width && !current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                    continue;
                }
                current.appendCodePoint(codePoint);
                offset += Character.charCount(codePoint);
            }
            lines.add(current.toString());
            return lines;
        }

        /** Символы, которых нет в шрифте (эмодзи и т. п.), заменяются на «?», иначе PDFBox падает. */
        private String printable(String text, PDFont font) {
            Map<Integer, Boolean> cache = glyphs.computeIfAbsent(font, key -> new HashMap<>());
            StringBuilder builder = new StringBuilder();
            text.codePoints().forEach(codePoint -> {
                if (codePoint == '\t') {
                    builder.append("    ");
                    return;
                }
                if (Character.isISOControl(codePoint)) {
                    return;
                }
                boolean supported = cache.computeIfAbsent(codePoint, point -> {
                    try {
                        font.encode(new String(Character.toChars(point)));
                        return true;
                    } catch (IOException | IllegalArgumentException error) {
                        return false;
                    }
                });
                if (supported) {
                    builder.appendCodePoint(codePoint);
                } else {
                    builder.append('?');
                }
            });
            return builder.toString();
        }

        private void write(String text, PDFont font, float size, float x, float baseline, Color color) throws IOException {
            if (text.isEmpty()) {
                return;
            }
            content.beginText();
            content.setFont(font, size);
            content.setNonStrokingColor(color);
            content.newLineAtOffset(x, baseline);
            content.showText(text);
            content.endText();
        }

        private void line(float x1, float y1, float x2, float y2) throws IOException {
            content.moveTo(x1, y1);
            content.lineTo(x2, y2);
            content.stroke();
        }

        private void space(float amount) {
            y -= amount;
        }

        private void ensure(float height) throws IOException {
            if (y - height < MARGIN) {
                newPage();
            }
        }

        private void newPage() throws IOException {
            if (content != null) {
                content.close();
            }
            PDPage page = new PDPage(PAGE);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = PAGE.getHeight() - MARGIN;
        }

        void close() throws IOException {
            content.close();
        }

        private record Word(String text, PDFont font, float width, String url) {
        }
    }
}
