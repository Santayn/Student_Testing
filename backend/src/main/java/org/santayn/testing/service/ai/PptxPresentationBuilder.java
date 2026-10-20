package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Собирает PPTX 16:9 по плану презентации в фирменных цветах (тема «Политех»: синий #2E54DC,
 * тёмно-синий #0A1A3F, зелёный акцент #11D56B). Шаблон не нужен — фигуры рисуются кодом.
 */
public class PptxPresentationBuilder {

    static final double WIDTH = 960;
    static final double HEIGHT = 540;
    private static final double MARGIN = 48;

    private static final Color PRIMARY = new Color(0x2E54DC);
    private static final Color DARK = new Color(0x0A1A3F);
    private static final Color ACCENT = new Color(0x11D56B);
    private static final Color SOFT = new Color(0xF1F6FF);
    private static final Color TEXT = new Color(0x1B2A4A);
    private static final Color MUTED = new Color(0x5B6B8C);
    private static final Color ON_PRIMARY_SOFT = new Color(0xCFE0FF);
    private static final String FONT = "Arial";

    private final XMLSlideShow slideShow = new XMLSlideShow();
    private final String presentationTitle;
    private int slideNumber;

    private PptxPresentationBuilder(String presentationTitle) {
        this.presentationTitle = presentationTitle;
        slideShow.setPageSize(new Dimension((int) WIDTH, (int) HEIGHT));
    }

    /**
     * @param outline план: title, subtitle, slides[] (см. PresentationOutlineService)
     * @param images  картинки слайдов по индексу слайда
     */
    public static Result build(JsonNode outline, Map<Integer, SlideImage> images, boolean withPdf) throws IOException {
        String title = textOr(outline.path("title"), "Презентация");
        PptxPresentationBuilder builder = new PptxPresentationBuilder(title);
        builder.titleSlide(title, textOr(outline.path("subtitle"), ""));
        int index = 0;
        for (JsonNode slide : outline.path("slides")) {
            builder.contentSlide(slide, images.get(index));
            index++;
        }
        try (XMLSlideShow show = builder.slideShow) {
            ByteArrayOutputStream pptx = new ByteArrayOutputStream();
            show.write(pptx);
            byte[] pdf = withPdf ? renderPdf(show) : null;
            return new Result(pptx.toByteArray(), pdf, show.getSlides().size());
        }
    }

    private void titleSlide(String title, String subtitle) {
        XSLFSlide slide = newSlide(PRIMARY);
        rect(slide, 0, 0, 14, HEIGHT, ACCENT);
        text(slide, MARGIN + 12, 170, WIDTH - 2 * MARGIN - 12, 140, title, fitSize(title, 44, 28, 60), Color.WHITE, true);
        if (!subtitle.isBlank()) {
            text(slide, MARGIN + 12, 320, WIDTH - 2 * MARGIN - 12, 80, subtitle, 22, ON_PRIMARY_SOFT, false);
        }
    }

    private void contentSlide(JsonNode slide, SlideImage image) {
        String layout = slide.path("layout").asText("bullets");
        String title = textOr(slide.path("title"), "");
        XSLFSlide target;
        switch (layout) {
            case "section" -> {
                target = newSlide(DARK);
                rect(target, MARGIN, 300, 120, 6, ACCENT);
                text(target, MARGIN, 170, WIDTH - 2 * MARGIN, 120, title, fitSize(title, 40, 28, 50), Color.WHITE, true);
            }
            case "quote" -> {
                target = newSlide(Color.WHITE);
                header(target, title);
                rect(target, MARGIN, 140, 8, 280, ACCENT);
                String quote = firstOr(slide.path("bullets"), title);
                XSLFTextBox box = text(target, MARGIN + 32, 140, WIDTH - 2 * MARGIN - 32, 280, quote,
                        fitSize(quote, 28, 18, 160), TEXT, false);
                box.getTextParagraphs().forEach(paragraph -> paragraph.getTextRuns().forEach(run -> run.setItalic(true)));
                footer(target);
            }
            case "two_columns" -> {
                target = newSlide(Color.WHITE);
                header(target, title);
                double columnWidth = (WIDTH - 2 * MARGIN - 24) / 2;
                column(target, MARGIN, columnWidth, slide.path("left"));
                column(target, MARGIN + columnWidth + 24, columnWidth, slide.path("right"));
                footer(target);
            }
            case "process" -> {
                target = newSlide(Color.WHITE);
                header(target, title);
                process(target, strings(slide.path("steps")));
                footer(target);
            }
            default -> {
                target = newSlide(Color.WHITE);
                header(target, title);
                List<String> bullets = strings(slide.path("bullets"));
                double bodyWidth = image == null ? WIDTH - 2 * MARGIN : 520;
                bullets(target, MARGIN, 130, bodyWidth, 360, bullets, TEXT);
                if (image != null) {
                    picture(target, image.content(), new Rectangle2D.Double(MARGIN + 540, 130, WIDTH - 2 * MARGIN - 540, 340));
                }
                footer(target);
            }
        }
        notes(target, slide.path("notes").asText(""), image == null ? null : image.attribution());
    }

    private XSLFSlide newSlide(Color background) {
        XSLFSlide slide = slideShow.createSlide();
        // Фон — фигурой во весь слайд: заливку фона слайда не учитывает рендер POI (PDF-копия).
        rect(slide, 0, 0, WIDTH, HEIGHT, background);
        slideNumber++;
        return slide;
    }

    private void header(XSLFSlide slide, String title) {
        rect(slide, 0, 0, WIDTH, 8, PRIMARY);
        text(slide, MARGIN, 34, WIDTH - 2 * MARGIN, 76, title, fitSize(title, 30, 20, 70), DARK, true);
        rect(slide, MARGIN, 112, 64, 4, ACCENT);
    }

    private void footer(XSLFSlide slide) {
        text(slide, MARGIN, HEIGHT - 40, 600, 24, presentationTitle, 11, MUTED, false);
        XSLFTextBox number = text(slide, WIDTH - MARGIN - 80, HEIGHT - 40, 80, 24, String.valueOf(slideNumber), 11, MUTED, false);
        number.getTextParagraphs().forEach(paragraph -> paragraph.setTextAlign(TextParagraph.TextAlign.RIGHT));
    }

    private void column(XSLFSlide slide, double x, double width, JsonNode column) {
        XSLFAutoShape card = slide.createAutoShape();
        card.setShapeType(ShapeType.ROUND_RECT);
        card.setAnchor(new Rectangle2D.Double(x, 136, width, 344));
        card.setFillColor(SOFT);
        card.setLineColor(null);
        text(slide, x + 20, 150, width - 40, 40, textOr(column.path("title"), ""), 22, PRIMARY, true);
        bullets(slide, x + 20, 196, width - 40, 276, strings(column.path("bullets")), TEXT);
    }

    private void process(XSLFSlide slide, List<String> steps) {
        if (steps.isEmpty()) {
            return;
        }
        int count = steps.size();
        double arrow = 28;
        double gap = 10;
        double boxWidth = (WIDTH - 2 * MARGIN - (count - 1) * (arrow + 2 * gap)) / count;
        double y = 200;
        double height = 170;
        int fontSize = count > 4 ? 14 : 17;
        for (int index = 0; index < count; index++) {
            double x = MARGIN + index * (boxWidth + arrow + 2 * gap);
            XSLFAutoShape box = slide.createAutoShape();
            box.setShapeType(ShapeType.ROUND_RECT);
            box.setAnchor(new Rectangle2D.Double(x, y, boxWidth, height));
            box.setFillColor(index % 2 == 0 ? PRIMARY : DARK);
            box.setLineColor(null);
            box.setWordWrap(true);
            box.setVerticalAlignment(org.apache.poi.sl.usermodel.VerticalAlignment.MIDDLE);
            box.clearText();
            XSLFTextParagraph paragraph = box.addNewTextParagraph();
            paragraph.setTextAlign(TextParagraph.TextAlign.CENTER);
            run(paragraph, (index + 1) + ". " + steps.get(index), fontSize, Color.WHITE, true);
            if (index < count - 1) {
                XSLFAutoShape connector = slide.createAutoShape();
                connector.setShapeType(ShapeType.RIGHT_ARROW);
                connector.setAnchor(new Rectangle2D.Double(x + boxWidth + gap, y + height / 2 - 14, arrow, 28));
                connector.setFillColor(ACCENT);
                connector.setLineColor(null);
            }
        }
    }

    private void bullets(XSLFSlide slide, double x, double y, double width, double height, List<String> items, Color color) {
        if (items.isEmpty()) {
            return;
        }
        int totalLength = items.stream().mapToInt(String::length).sum();
        int fontSize = totalLength > 520 ? 15 : totalLength > 380 ? 17 : totalLength > 240 ? 19 : 22;
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle2D.Double(x, y, width, height));
        box.setWordWrap(true);
        box.clearText();
        for (String item : items) {
            XSLFTextParagraph paragraph = box.addNewTextParagraph();
            paragraph.setBullet(true);
            paragraph.setBulletCharacter("•");
            paragraph.setBulletFontColor(ACCENT);
            paragraph.setLeftMargin(22.0);
            paragraph.setIndent(-22.0);
            paragraph.setSpaceAfter(40.0);
            run(paragraph, item, fontSize, color, false);
        }
    }

    private void picture(XSLFSlide slide, byte[] content, Rectangle2D frame) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                return;
            }
            PictureData.PictureType type = ImageGenerationService.isPng(content)
                    ? PictureData.PictureType.PNG
                    : PictureData.PictureType.JPEG;
            XSLFPictureData data = slideShow.addPicture(content, type);
            XSLFPictureShape picture = slide.createPicture(data);
            double scale = Math.min(frame.getWidth() / image.getWidth(), frame.getHeight() / image.getHeight());
            double width = image.getWidth() * scale;
            double height = image.getHeight() * scale;
            picture.setAnchor(new Rectangle2D.Double(
                    frame.getX() + (frame.getWidth() - width) / 2,
                    frame.getY() + (frame.getHeight() - height) / 2,
                    width,
                    height
            ));
        } catch (IOException ignored) {
            // битая картинка — слайд останется без неё
        }
    }

    private void notes(XSLFSlide slide, String notes, String attribution) {
        String text = notes == null ? "" : notes.trim();
        if (attribution != null && !attribution.isBlank()) {
            text = (text.isEmpty() ? "" : text + "\n\n") + "Изображение: " + attribution;
        }
        if (text.isEmpty()) {
            return;
        }
        XSLFNotes notesSlide = slideShow.getNotesSlide(slide);
        for (XSLFTextShape shape : notesSlide.getPlaceholders()) {
            if (shape.getTextType() == Placeholder.BODY) {
                shape.setText(text);
                return;
            }
        }
    }

    private static XSLFAutoShape rect(XSLFSlide slide, double x, double y, double width, double height, Color color) {
        XSLFAutoShape shape = slide.createAutoShape();
        shape.setShapeType(ShapeType.RECT);
        shape.setAnchor(new Rectangle2D.Double(x, y, width, height));
        shape.setFillColor(color);
        shape.setLineColor(null);
        return shape;
    }

    private static XSLFTextBox text(XSLFSlide slide, double x, double y, double width, double height,
                                    String value, int fontSize, Color color, boolean bold) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle2D.Double(x, y, width, height));
        box.setWordWrap(true);
        box.clearText();
        run(box.addNewTextParagraph(), value, fontSize, color, bold);
        return box;
    }

    private static void run(XSLFTextParagraph paragraph, String value, int fontSize, Color color, boolean bold) {
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(value);
        run.setFontFamily(FONT);
        run.setFontSize((double) fontSize);
        run.setFontColor(color);
        run.setBold(bold);
    }

    /** Чем длиннее заголовок, тем мельче шрифт: PowerPoint не ужимает текст сам при открытии. */
    static int fitSize(String text, int max, int min, int comfortableLength) {
        int length = text == null ? 0 : text.length();
        if (length <= comfortableLength) {
            return max;
        }
        double ratio = (double) comfortableLength / length;
        return Math.max(min, (int) Math.round(max * Math.sqrt(ratio)));
    }

    private static byte[] renderPdf(XMLSlideShow show) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (XSLFSlide slide : show.getSlides()) {
                BufferedImage image = new BufferedImage((int) WIDTH * 2, (int) HEIGHT * 2, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    graphics.setPaint(Color.WHITE);
                    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                    graphics.scale(2, 2);
                    slide.draw(graphics);
                } finally {
                    graphics.dispose();
                }
                PDPage page = new PDPage(new PDRectangle((float) WIDTH, (float) HEIGHT));
                document.addPage(page);
                PDImageXObject pdfImage = JPEGFactory.createFromImage(document, image, 0.9f);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.drawImage(pdfImage, 0, 0, (float) WIDTH, (float) HEIGHT);
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private static List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                String text = item.asText("").trim();
                if (!text.isEmpty()) {
                    values.add(text);
                }
            });
        }
        return values;
    }

    private static String firstOr(JsonNode node, String fallback) {
        List<String> values = strings(node);
        return values.isEmpty() ? fallback : values.get(0);
    }

    private static String textOr(JsonNode node, String fallback) {
        String value = node == null ? "" : node.asText("").trim();
        return value.isEmpty() ? fallback : value;
    }

    public record SlideImage(byte[] content, String attribution) {
    }

    public record Result(byte[] pptx, byte[] pdf, int slideCount) {
    }
}
