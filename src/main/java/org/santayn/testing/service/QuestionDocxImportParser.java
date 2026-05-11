package org.santayn.testing.service;

import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QuestionDocxImportParser {

    private static final Pattern NUMBERED_QUESTION_PREFIX = Pattern.compile(
            "(?iu)^(?:(?:вопрос|question|q)\\s*)?(?:№\\s*)?\\d+\\s*(?:[.)]|:|-)\\s*"
    );
    private static final Pattern WORD_QUESTION_PREFIX = Pattern.compile(
            "(?iu)^(?:вопрос|question)\\s*(?:№\\s*)?\\d*\\s*(?:[.)]|:|-)\\s*"
    );
    private static final Pattern LABELED_OPTION = Pattern.compile(
            "(?iu)^([*+])?\\s*([A-ZА-ЯЁ]|\\d{1,2})\\s*(?:[.)]|:|-)\\s+(.+)$"
    );
    private static final Pattern BULLET_OPTION = Pattern.compile(
            "(?iu)^([*+])?\\s*[-•]\\s*(?:\\[([xх+*])\\]\\s*)?(.+)$"
    );
    private static final Pattern CHECKBOX_OPTION = Pattern.compile(
            "(?iu)^\\[([xх+*])\\]\\s+(.+)$"
    );
    private static final Pattern ANSWER_LINE = Pattern.compile(
            "(?iu)^(?:правильн(?:ый|ые)?\\s+ответ(?:ы)?|ответ(?:ы)?|correct(?:\\s+answer)?)\\s*[:=\\-]\\s*(.+)$"
    );
    private static final Pattern POINTS_LINE = Pattern.compile(
            "(?iu)^(?:балл(?:ы|ов)?|points?|score)\\s*[:=\\-]\\s*(\\d+(?:[,.]\\d+)?)$"
    );

    public List<ParsedQuestion> parse(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<List<String>> blocks = buildBlocks(extractLines(document));
            if (blocks.isEmpty()) {
                throw new IllegalArgumentException("DOCX file does not contain questions.");
            }

            List<ParsedQuestion> questions = new ArrayList<>();
            for (int i = 0; i < blocks.size(); i++) {
                questions.add(parseBlock(blocks.get(i), i + 1));
            }
            return questions;
        } catch (IOException | POIXMLException error) {
            throw new IllegalArgumentException("Only valid .docx files are supported.", error);
        }
    }

    private List<String> extractLines(XWPFDocument document) {
        List<String> lines = new ArrayList<>();
        for (IBodyElement element : document.getBodyElements()) {
            collectLines(element, lines);
        }
        return lines;
    }

    private void collectLines(IBodyElement element, List<String> lines) {
        if (element instanceof XWPFParagraph paragraph) {
            lines.add(normalizeText(paragraph.getText()));
            return;
        }

        if (element instanceof XWPFTable table) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (IBodyElement cellElement : cell.getBodyElements()) {
                        collectLines(cellElement, lines);
                    }
                }
                lines.add("");
            }
        }
    }

    private List<List<String>> buildBlocks(List<String> lines) {
        List<List<String>> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String rawLine : lines) {
            String line = normalizeText(rawLine);
            if (line.isBlank()) {
                finishBlock(blocks, current);
                current = new ArrayList<>();
                continue;
            }

            boolean startsNewQuestion = isQuestionStart(line)
                    && !current.isEmpty()
                    && hasAnswerLine(current);
            if (startsNewQuestion) {
                finishBlock(blocks, current);
                current = new ArrayList<>();
            }

            current.add(current.isEmpty() && isQuestionStart(line) ? stripQuestionPrefix(line) : line);
        }

        finishBlock(blocks, current);
        return blocks;
    }

    private ParsedQuestion parseBlock(List<String> lines, int blockNumber) {
        List<String> questionLines = new ArrayList<>();
        List<RawOption> options = new ArrayList<>();
        String answer = null;
        BigDecimal points = BigDecimal.ONE;

        for (String line : lines) {
            Matcher pointsMatcher = POINTS_LINE.matcher(line);
            if (pointsMatcher.matches()) {
                points = parsePoints(pointsMatcher.group(1), blockNumber);
                continue;
            }

            Matcher answerMatcher = ANSWER_LINE.matcher(line);
            if (answerMatcher.matches()) {
                answer = requireLength(answerMatcher.group(1).trim(), "Correct answer", blockNumber);
                continue;
            }

            RawOption option = parseOption(line, options.size() + 1, blockNumber);
            if (option != null) {
                options.add(option);
                continue;
            }

            questionLines.add(line);
        }

        String questionText = requireLength(String.join(" ", questionLines).trim(), "Question", blockNumber);
        if (questionText.isBlank()) {
            throw new IllegalArgumentException("Question block " + blockNumber + " has no question text.");
        }

        applyAnswerToOptions(answer, options);
        int correctOptions = (int) options.stream().filter(RawOption::correct).count();
        int type = options.isEmpty()
                ? QuestionTypeSupport.TYPE_TEXT
                : correctOptions > 1 ? QuestionTypeSupport.TYPE_MULTIPLE : QuestionTypeSupport.TYPE_SINGLE;
        String correctAnswer = answer;
        if (correctAnswer == null && !options.isEmpty()) {
            correctAnswer = options.stream()
                    .filter(RawOption::correct)
                    .map(RawOption::labelOrText)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse(null);
        }

        return new ParsedQuestion(
                questionText,
                type,
                points,
                FacultyService.trimToNull(correctAnswer),
                options.stream()
                        .map(option -> new ParsedOption(option.text(), option.correct()))
                        .toList()
        );
    }

    private RawOption parseOption(String line, int ordinal, int blockNumber) {
        Matcher labeledMatcher = LABELED_OPTION.matcher(line);
        if (labeledMatcher.matches()) {
            OptionText optionText = normalizeOptionText(labeledMatcher.group(3), labeledMatcher.group(1) != null);
            return new RawOption(
                    normalizeLabel(labeledMatcher.group(2)),
                    requireLength(optionText.text(), "Option", blockNumber),
                    optionText.correct()
            );
        }

        Matcher bulletMatcher = BULLET_OPTION.matcher(line);
        if (bulletMatcher.matches()) {
            boolean correct = bulletMatcher.group(1) != null || bulletMatcher.group(2) != null;
            OptionText optionText = normalizeOptionText(bulletMatcher.group(3), correct);
            return new RawOption(
                    String.valueOf(ordinal),
                    requireLength(optionText.text(), "Option", blockNumber),
                    optionText.correct()
            );
        }

        Matcher checkboxMatcher = CHECKBOX_OPTION.matcher(line);
        if (checkboxMatcher.matches()) {
            OptionText optionText = normalizeOptionText(checkboxMatcher.group(2), true);
            return new RawOption(
                    String.valueOf(ordinal),
                    requireLength(optionText.text(), "Option", blockNumber),
                    optionText.correct()
            );
        }

        return null;
    }

    private void applyAnswerToOptions(String answer, List<RawOption> options) {
        if (answer == null || options.isEmpty()) {
            return;
        }

        Set<String> tokens = splitAnswerTokens(answer);
        Set<String> comparableTokens = new HashSet<>();
        for (String token : tokens) {
            comparableTokens.add(normalizeComparable(token));
            comparableTokens.add(normalizeLabel(token));
        }

        for (RawOption option : options) {
            if (comparableTokens.contains(normalizeLabel(option.label()))
                    || comparableTokens.contains(normalizeComparable(option.text()))) {
                option.correct(true);
            }
        }
    }

    private Set<String> splitAnswerTokens(String answer) {
        String normalized = normalizeText(answer);
        String[] parts = normalized.split("[,;]+");
        if (parts.length == 1 && normalized.matches("(?iu)(?:[A-ZА-ЯЁ0-9]{1,2}\\s+)+[A-ZА-ЯЁ0-9]{1,2}")) {
            parts = normalized.split("\\s+");
        }

        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            String token = part.trim();
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private OptionText normalizeOptionText(String rawText, boolean correct) {
        String text = normalizeText(rawText);
        boolean normalizedCorrect = correct;

        if (text.endsWith("*") || text.endsWith("+")) {
            normalizedCorrect = true;
            text = text.substring(0, text.length() - 1).trim();
        }

        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.endsWith("(+)") || lower.endsWith("(x)") || lower.endsWith("[x]")
                || lower.endsWith("(правильно)") || lower.endsWith("(correct)")) {
            normalizedCorrect = true;
            text = text.replaceFirst("(?iu)\\s*(?:\\(\\+\\)|\\(x\\)|\\[x\\]|\\(правильно\\)|\\(correct\\))$", "").trim();
        }

        return new OptionText(text, normalizedCorrect);
    }

    private BigDecimal parsePoints(String value, int blockNumber) {
        try {
            BigDecimal points = new BigDecimal(value.replace(',', '.'));
            if (points.compareTo(BigDecimal.ZERO) < 0) {
                throw new NumberFormatException("negative");
            }
            return points;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Question block " + blockNumber + " has invalid points value.", error);
        }
    }

    private String requireLength(String value, String fieldName, int blockNumber) {
        String text = normalizeText(value);
        if (text.length() > 2000) {
            throw new IllegalArgumentException(fieldName + " in question block " + blockNumber + " is longer than 2000 characters.");
        }
        return text;
    }

    private boolean isQuestionStart(String line) {
        return NUMBERED_QUESTION_PREFIX.matcher(line).find() || WORD_QUESTION_PREFIX.matcher(line).find();
    }

    private boolean hasAnswerLine(List<String> lines) {
        return lines.stream().anyMatch(line -> ANSWER_LINE.matcher(line).matches());
    }

    private String stripQuestionPrefix(String line) {
        String result = WORD_QUESTION_PREFIX.matcher(line).replaceFirst("");
        result = NUMBERED_QUESTION_PREFIX.matcher(result).replaceFirst("");
        return result.trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeComparable(String value) {
        return normalizeText(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeLabel(String value) {
        return normalizeComparable(value)
                .replace('А', 'A')
                .replace('В', 'B')
                .replace('С', 'C')
                .replace('Е', 'E')
                .replace('Н', 'H')
                .replace('К', 'K')
                .replace('М', 'M')
                .replace('О', 'O')
                .replace('Р', 'P')
                .replace('Т', 'T')
                .replace('Х', 'X');
    }

    private void finishBlock(List<List<String>> blocks, List<String> current) {
        if (current.stream().anyMatch(line -> !line.isBlank())) {
            blocks.add(List.copyOf(current));
        }
    }

    public record ParsedQuestion(String question, int type, BigDecimal points, String correctAnswer, List<ParsedOption> options) {
    }

    public record ParsedOption(String text, boolean correct) {
    }

    private record OptionText(String text, boolean correct) {
    }

    private static final class RawOption {
        private final String label;
        private final String text;
        private boolean correct;

        private RawOption(String label, String text, boolean correct) {
            this.label = label;
            this.text = text;
            this.correct = correct;
        }

        private String label() {
            return label;
        }

        private String text() {
            return text;
        }

        private boolean correct() {
            return correct;
        }

        private void correct(boolean correct) {
            this.correct = correct;
        }

        private String labelOrText() {
            return label == null || label.isBlank() ? text : label;
        }
    }
}
