package org.santayn.testing.service.lecture;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбор Markdown текста лекции — тот же набор, что у веба (frontend/src/utils/markdown.js) и мобилки:
 * заголовки #–####, абзацы, списки, цитаты, код, таблицы, линия, **жирный**, *курсив*, `код`, ссылки.
 */
public final class MarkdownDocument {

    private static final Pattern HEADING = Pattern.compile("^(#{1,4})\\s+(.+?)\\s*#*\\s*$");
    private static final Pattern BULLET = Pattern.compile("^\\s*[-*+•]\\s+(.*)$");
    private static final Pattern ORDERED = Pattern.compile("^\\s*\\d{1,3}[.)]\\s+(.*)$");
    private static final Pattern QUOTE = Pattern.compile("^\\s*>\\s?(.*)$");
    private static final Pattern FENCE = Pattern.compile("^\\s*```");
    private static final Pattern RULE = Pattern.compile("^\\s*(?:-{3,}|\\*{3,}|_{3,})\\s*$");
    private static final Pattern TABLE_ROW = Pattern.compile("^\\s*\\|.*\\|\\s*$");
    private static final Pattern TABLE_DIVIDER = Pattern.compile("^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$");
    private static final Pattern INLINE = Pattern.compile(
            "(`[^`]+`)|(\\*\\*[^*]+\\*\\*|__[^_]+__)|(\\*[^*\\s][^*]*\\*|_[^_\\s][^_]*_)|(\\[[^\\]]+]\\([^)\\s]+\\))"
    );
    private static final Pattern LINK = Pattern.compile("^\\[([^\\]]+)]\\(([^)\\s]+)\\)$");

    private MarkdownDocument() {
    }

    public enum Style { PLAIN, BOLD, ITALIC, CODE }

    /** Фрагмент строки с одним стилем; url — у ссылок. */
    public record Span(String text, Style style, String url) {
    }

    public sealed interface Block permits Heading, Paragraph, ListBlock, Quote, Code, Table, Rule {
    }

    public record Heading(int level, List<Span> spans) implements Block {
    }

    public record Paragraph(List<Span> spans) implements Block {
    }

    public record ListBlock(boolean ordered, List<List<Span>> items) implements Block {
    }

    public record Quote(List<Span> spans) implements Block {
    }

    public record Code(String text) implements Block {
    }

    public record Table(List<List<Span>> header, List<List<List<Span>>> rows) implements Block {
    }

    public record Rule() implements Block {
    }

    public static List<Span> parseInline(String text) {
        List<Span> spans = new ArrayList<>();
        appendInline(text, Style.PLAIN, null, spans);
        return spans;
    }

    private static void appendInline(String text, Style outer, String url, List<Span> spans) {
        Matcher matcher = INLINE.matcher(text);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                spans.add(new Span(text.substring(last, matcher.start()), outer, url));
            }
            String token = matcher.group();
            if (matcher.group(1) != null) {
                spans.add(new Span(token.substring(1, token.length() - 1), Style.CODE, url));
            } else if (matcher.group(2) != null) {
                appendInline(token.substring(2, token.length() - 2), Style.BOLD, url, spans);
            } else if (matcher.group(3) != null) {
                appendInline(token.substring(1, token.length() - 1), outer == Style.BOLD ? Style.BOLD : Style.ITALIC, url, spans);
            } else {
                Matcher link = LINK.matcher(token);
                if (link.matches()) {
                    String href = link.group(2);
                    boolean safe = href.regionMatches(true, 0, "http://", 0, 7) || href.regionMatches(true, 0, "https://", 0, 8);
                    appendInline(link.group(1), outer, safe ? href : null, spans);
                }
            }
            last = matcher.end();
        }
        if (last < text.length()) {
            spans.add(new Span(text.substring(last), outer, url));
        }
    }

    public static List<Block> parse(String markdown) {
        String[] lines = (markdown == null ? "" : markdown).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        List<Block> blocks = new ArrayList<>();
        int index = 0;
        while (index < lines.length) {
            String line = lines[index];
            if (line.isBlank()) {
                index++;
            } else if (FENCE.matcher(line).find()) {
                List<String> code = new ArrayList<>();
                index++;
                while (index < lines.length && !FENCE.matcher(lines[index]).find()) {
                    code.add(lines[index++]);
                }
                index++;
                blocks.add(new Code(String.join("\n", code)));
            } else if (HEADING.matcher(line).matches()) {
                Matcher heading = HEADING.matcher(line);
                heading.matches();
                blocks.add(new Heading(heading.group(1).length(), parseInline(heading.group(2))));
                index++;
            } else if (RULE.matcher(line).matches()) {
                blocks.add(new Rule());
                index++;
            } else if (TABLE_ROW.matcher(line).matches() && index + 1 < lines.length
                    && TABLE_DIVIDER.matcher(lines[index + 1]).matches()) {
                List<List<Span>> header = row(line);
                List<List<List<Span>>> rows = new ArrayList<>();
                index += 2;
                while (index < lines.length && TABLE_ROW.matcher(lines[index]).matches()) {
                    rows.add(row(lines[index++]));
                }
                blocks.add(new Table(header, rows));
            } else if (QUOTE.matcher(line).matches()) {
                List<String> quote = new ArrayList<>();
                Matcher matcher;
                while (index < lines.length && (matcher = QUOTE.matcher(lines[index])).matches()) {
                    quote.add(matcher.group(1));
                    index++;
                }
                blocks.add(new Quote(parseInline(String.join(" ", quote).trim())));
            } else if (BULLET.matcher(line).matches() || ORDERED.matcher(line).matches()) {
                boolean ordered = !BULLET.matcher(line).matches();
                Pattern pattern = ordered ? ORDERED : BULLET;
                List<List<Span>> items = new ArrayList<>();
                Matcher matcher;
                while (index < lines.length && (matcher = pattern.matcher(lines[index])).matches()) {
                    items.add(parseInline(matcher.group(1)));
                    index++;
                }
                blocks.add(new ListBlock(ordered, items));
            } else {
                List<String> paragraph = new ArrayList<>();
                while (index < lines.length && !lines[index].isBlank() && !startsBlock(lines[index])) {
                    paragraph.add(lines[index++].trim());
                }
                if (paragraph.isEmpty()) {
                    paragraph.add(line.trim());
                    index++;
                }
                blocks.add(new Paragraph(parseInline(String.join(" ", paragraph))));
            }
        }
        return blocks;
    }

    private static boolean startsBlock(String line) {
        return HEADING.matcher(line).matches() || FENCE.matcher(line).find() || QUOTE.matcher(line).matches()
                || BULLET.matcher(line).matches() || ORDERED.matcher(line).matches() || RULE.matcher(line).matches()
                || TABLE_ROW.matcher(line).matches();
    }

    private static List<List<Span>> row(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        List<List<Span>> cells = new ArrayList<>();
        for (String cell : trimmed.split("\\|", -1)) {
            cells.add(parseInline(cell.trim()));
        }
        return cells;
    }

    public static String plain(List<Span> spans) {
        StringBuilder builder = new StringBuilder();
        spans.forEach(span -> builder.append(span.text()));
        return builder.toString();
    }
}
