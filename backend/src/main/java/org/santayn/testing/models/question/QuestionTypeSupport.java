package org.santayn.testing.models.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class QuestionTypeSupport {

    /**
     * 1 — вопрос с одним вариантом ответа.
     */
    public static final int TYPE_SINGLE = 1;

    /**
     * 2 — вопрос с двумя и более правильными вариантами ответа.
     */
    public static final int TYPE_MULTIPLE = 2;

    /**
     * 3 — вопрос на сопоставление элементов колонки А с элементами колонки Б.
     */
    public static final int TYPE_MATCHING = 3;

    /**
     * 4 — текстовый вопрос со свободным ответом.
     */
    public static final int TYPE_TEXT = 4;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern NON_LETTER_OR_DIGIT = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private QuestionTypeSupport() {
    }

    public static boolean isSupported(int type) {
        return type >= TYPE_SINGLE && type <= TYPE_TEXT;
    }

    public static boolean isText(int type) {
        return type == TYPE_TEXT;
    }

    public static boolean usesSelectableOptions(int type) {
        return type == TYPE_SINGLE || type == TYPE_MULTIPLE;
    }

    public static boolean isSingleChoice(int type) {
        return type == TYPE_SINGLE;
    }

    public static boolean isMultipleChoice(int type) {
        return type == TYPE_MULTIPLE;
    }

    public static boolean isMatching(int type) {
        return type == TYPE_MATCHING;
    }

    public static String label(int type) {
        return switch (type) {
            case TYPE_SINGLE -> "Single choice";
            case TYPE_MULTIPLE -> "Multiple choice";
            case TYPE_MATCHING -> "Matching";
            case TYPE_TEXT -> "Text";
            default -> "Type " + type;
        };
    }

    public static List<String> splitAcceptedTextAnswers(String rawValue) {
        String trimmed = trimToNull(rawValue);
        if (trimmed == null) {
            return List.of();
        }
        String[] rawParts = trimmed.split("\\r?\\n|\\|");
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        for (String rawPart : rawParts) {
            String part = trimToNull(rawPart);
            if (part != null) {
                parts.add(part);
            }
        }
        return List.copyOf(parts);
    }

    public static String normalizeComparable(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е');
        normalized = NON_LETTER_OR_DIGIT.matcher(normalized).replaceAll(" ");
        normalized = MULTI_SPACE.matcher(normalized).replaceAll(" ").trim();
        return normalized.isBlank() ? null : normalized;
    }

    public static String serializeMatchingPairs(List<MatchingPair> pairs) {
        List<Map<String, Object>> payload = pairs.stream()
                .sorted(Comparator.comparingInt(MatchingPair::ordinal))
                .map(pair -> Map.<String, Object>of(
                        "ordinal", pair.ordinal(),
                        "left", pair.left(),
                        "right", pair.right()
                ))
                .toList();
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of("pairs", payload));
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("Failed to serialize matching pairs.", error);
        }
    }

    public static List<MatchingPair> parseMatchingPairs(String rawValue) {
        String trimmed = trimToNull(rawValue);
        if (trimmed == null) {
            return List.of();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(trimmed);
            JsonNode pairsNode = root.isArray() ? root : root.path("pairs");
            if (!pairsNode.isArray()) {
                return List.of();
            }

            List<MatchingPair> pairs = new ArrayList<>();
            int fallbackOrdinal = 1;
            for (JsonNode item : pairsNode) {
                String left = trimToNull(item.path("left").asText(null));
                String right = trimToNull(item.path("right").asText(null));
                if (left == null && right == null) {
                    fallbackOrdinal++;
                    continue;
                }
                int ordinal = item.hasNonNull("ordinal")
                        ? item.path("ordinal").asInt(fallbackOrdinal)
                        : fallbackOrdinal;
                pairs.add(new MatchingPair(ordinal, left, right));
                fallbackOrdinal++;
            }
            return pairs.stream()
                    .sorted(Comparator.comparingInt(MatchingPair::ordinal))
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }


    public static String displaySubmittedMatchingPairs(String expectedRawValue, String actualRawValue) {
        List<MatchingPair> expectedPairs = parseMatchingPairs(expectedRawValue);
        List<MatchingPair> actualPairs = parseMatchingPairs(actualRawValue);
        if (expectedPairs.isEmpty()) {
            return displayMatchingPairs(actualPairs);
        }

        Map<Integer, String> actualRightByOrdinal = actualPairs.stream()
                .collect(Collectors.toMap(
                        MatchingPair::ordinal,
                        pair -> pair.right() == null ? "" : pair.right(),
                        (left, right) -> left
                ));
        List<MatchingPair> displayPairs = expectedPairs.stream()
                .map(pair -> new MatchingPair(pair.ordinal(), pair.left(), actualRightByOrdinal.get(pair.ordinal())))
                .toList();
        return displayMatchingPairs(displayPairs);
    }

    public static String displayMatchingPairs(String rawValue) {
        return displayMatchingPairs(parseMatchingPairs(rawValue));
    }

    public static String displayMatchingPairs(List<MatchingPair> pairs) {
        return pairs.stream()
                .sorted(Comparator.comparingInt(MatchingPair::ordinal))
                .map(pair -> {
                    String left = trimToNull(pair.left());
                    String right = trimToNull(pair.right());
                    return (left == null ? "?" : left) + " -> " + (right == null ? "-" : right);
                })
                .collect(Collectors.joining(" | "));
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public record MatchingPair(int ordinal, String left, String right) {
    }
}
