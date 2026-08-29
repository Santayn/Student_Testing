package org.santayn.testing.service;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.santayn.testing.models.question.QuestionTypeSupport;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Проверка свободного текстового ответа без жесткого сравнения строк.
 *
 * <p>Алгоритм намеренно не делает ответ «правильным» по одному совпавшему слову.
 * Он комбинирует нормализацию, варианты эталонного ответа, похожесть строк,
 * покрытие ключевых слов, небольшую устойчивость к опечаткам и словарь частых
 * учебных сокращений.</p>
 */
public final class TextAnswerEvaluator {

    private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();
    private static final LevenshteinDistance LEVENSHTEIN = LevenshteinDistance.getDefaultInstance();
    private static final Pattern NON_LETTER_OR_DIGIT = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private static final Set<String> STOP_WORDS = Set.of(
            "а", "без", "более", "бы", "был", "была", "были", "было", "быть", "в", "вам", "вас",
            "весь", "во", "вот", "все", "всего", "всех", "вы", "где", "да", "для", "до", "его", "ее",
            "если", "есть", "еще", "же", "за", "здесь", "и", "из", "или", "им", "их", "к", "как", "ко",
            "когда", "который", "которые", "которых", "ли", "либо", "между", "меня", "мне", "может",
            "мы", "на", "над", "надо", "наш", "не", "него", "нее", "нет", "ни", "них", "но", "ну", "о",
            "об", "однако", "он", "она", "они", "оно", "от", "очень", "по", "под", "при", "с", "со",
            "так", "также", "такой", "там", "то", "того", "тоже", "той", "только", "том", "ты", "у", "уже",
            "чем", "что", "чтобы", "это", "этот", "этом", "этого", "является", "являются", "называется",
            "the", "a", "an", "and", "or", "to", "of", "in", "on", "for", "with", "by", "is", "are", "was",
            "were", "be", "been", "being", "this", "that", "these", "those", "as", "at", "from", "it"
    );

    private static final Map<String, List<String>> SYNONYM_EXPANSIONS = buildSynonymExpansions();

    private TextAnswerEvaluator() {
    }

    public static boolean isCorrect(String expectedRaw, String actualRaw) {
        String actualNormalized = normalizeForAnswer(actualRaw);
        if (actualNormalized == null) {
            return false;
        }

        for (String expectedVariant : QuestionTypeSupport.splitAcceptedTextAnswers(expectedRaw)) {
            String expectedNormalized = normalizeForAnswer(expectedVariant);
            if (expectedNormalized == null) {
                continue;
            }
            if (isVariantCorrect(expectedNormalized, actualNormalized)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVariantCorrect(String expectedNormalized, String actualNormalized) {
        if (expectedNormalized.equals(actualNormalized)) {
            return true;
        }

        List<String> expectedTokens = meaningfulTokens(expectedNormalized);
        List<String> actualTokens = meaningfulTokens(actualNormalized);
        if (expectedTokens.isEmpty() || actualTokens.isEmpty()) {
            return false;
        }

        if (new HashSet<>(expectedTokens).equals(new HashSet<>(actualTokens))) {
            return true;
        }

        double jaroWinklerScore = JARO_WINKLER.apply(expectedNormalized, actualNormalized);
        double levenshteinScore = normalizedLevenshteinSimilarity(expectedNormalized, actualNormalized);
        double expectedCoverage = tokenCoverage(expectedTokens, actualTokens);
        double actualCoverage = tokenCoverage(actualTokens, expectedTokens);
        double jaccard = jaccard(expectedTokens, actualTokens);

        if (expectedNormalized.length() >= 5 && actualNormalized.length() >= 5
                && jaroWinklerScore >= 0.96
                && levenshteinScore >= 0.88) {
            return true;
        }

        if (expectedCoverage >= 0.85 && actualCoverage >= 0.65) {
            return true;
        }

        if (expectedCoverage >= 0.75 && actualCoverage >= 0.55 && Math.max(jaroWinklerScore, levenshteinScore) >= 0.82) {
            return true;
        }

        return jaccard >= 0.72 && Math.max(jaroWinklerScore, levenshteinScore) >= 0.78;
    }

    private static String normalizeForAnswer(String value) {
        String trimmed = FacultyService.trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е');
        normalized = expandKnownTerms(normalized);
        normalized = NON_LETTER_OR_DIGIT.matcher(normalized).replaceAll(" ");
        normalized = MULTI_SPACE.matcher(normalized).replaceAll(" ").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static String expandKnownTerms(String normalized) {
        String expanded = normalized;
        for (Map.Entry<String, List<String>> entry : SYNONYM_EXPANSIONS.entrySet()) {
            String term = entry.getKey();
            String replacement = term + " " + String.join(" ", entry.getValue());
            expanded = expanded.replaceAll("(?iu)(^|[^\\p{L}\\p{N}])" + Pattern.quote(term) + "(?=$|[^\\p{L}\\p{N}])", "$1" + replacement);
        }
        return expanded;
    }

    private static List<String> meaningfulTokens(String normalizedValue) {
        String trimmed = FacultyService.trimToNull(normalizedValue);
        if (trimmed == null) {
            return List.of();
        }

        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String rawToken : trimmed.split(" ")) {
            String token = FacultyService.trimToNull(rawToken);
            if (token == null || STOP_WORDS.contains(token)) {
                continue;
            }
            tokens.add(stemToken(token));
        }
        return List.copyOf(tokens);
    }

    private static String stemToken(String token) {
        String stemmed = token;
        if (stemmed.length() > 7) {
            String[] russianEndings = {
                    "иями", "ями", "ами", "ого", "ему", "ыми", "ими", "ной", "ную", "ная", "ные", "ных",
                    "ого", "его", "ому", "ему", "ость", "ения", "ение", "ация", "ации", "иями", "ирует",
                    "ировать", "ированный", "ированная", "ированное", "ированные"
            };
            for (String ending : russianEndings) {
                if (stemmed.endsWith(ending) && stemmed.length() - ending.length() >= 4) {
                    return stemmed.substring(0, stemmed.length() - ending.length());
                }
            }
        }

        if (stemmed.length() > 6) {
            String[] englishEndings = {"ization", "ations", "ation", "ments", "ment", "ness", "ing", "ed", "es", "s"};
            for (String ending : englishEndings) {
                if (stemmed.endsWith(ending) && stemmed.length() - ending.length() >= 4) {
                    return stemmed.substring(0, stemmed.length() - ending.length());
                }
            }
        }
        return stemmed;
    }

    private static double tokenCoverage(List<String> expectedTokens, List<String> actualTokens) {
        if (expectedTokens.isEmpty()) {
            return 0;
        }

        int matched = 0;
        Set<Integer> usedActualIndexes = new HashSet<>();
        for (String expectedToken : expectedTokens) {
            for (int actualIndex = 0; actualIndex < actualTokens.size(); actualIndex++) {
                if (usedActualIndexes.contains(actualIndex)) {
                    continue;
                }
                if (tokensEquivalent(expectedToken, actualTokens.get(actualIndex))) {
                    matched++;
                    usedActualIndexes.add(actualIndex);
                    break;
                }
            }
        }
        return (double) matched / expectedTokens.size();
    }

    private static boolean tokensEquivalent(String expectedToken, String actualToken) {
        if (expectedToken.equals(actualToken)) {
            return true;
        }
        if (expectedToken.length() < 4 || actualToken.length() < 4) {
            return false;
        }
        return normalizedLevenshteinSimilarity(expectedToken, actualToken) >= 0.84;
    }

    private static double jaccard(List<String> expectedTokens, List<String> actualTokens) {
        Set<String> expected = new HashSet<>(expectedTokens);
        Set<String> actual = new HashSet<>(actualTokens);
        if (expected.isEmpty() || actual.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>();
        for (String expectedToken : expected) {
            for (String actualToken : actual) {
                if (tokensEquivalent(expectedToken, actualToken)) {
                    intersection.add(expectedToken);
                    break;
                }
            }
        }
        Set<String> union = new HashSet<>(expected);
        union.addAll(actual);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private static double normalizedLevenshteinSimilarity(String expectedNormalized, String actualNormalized) {
        int maxLength = Math.max(expectedNormalized.length(), actualNormalized.length());
        if (maxLength == 0) {
            return 1;
        }
        Integer distance = LEVENSHTEIN.apply(expectedNormalized, actualNormalized);
        if (distance == null) {
            return 0;
        }
        return 1.0 - ((double) distance / maxLength);
    }

    private static Map<String, List<String>> buildSynonymExpansions() {
        Map<String, List<String>> expansions = new HashMap<>();
        addExpansion(expansions, "cpu", "central", "processing", "unit", "центральный", "процессор");
        addExpansion(expansions, "цпу", "central", "processing", "unit", "центральный", "процессор");
        addExpansion(expansions, "sql", "structured", "query", "language", "структурированный", "язык", "запросов");
        addExpansion(expansions, "ооп", "object", "oriented", "programming", "объектно", "ориентированное", "программирование");
        addExpansion(expansions, "api", "application", "programming", "interface", "интерфейс", "программирования", "приложений");
        addExpansion(expansions, "ram", "random", "access", "memory", "оперативная", "память");
        addExpansion(expansions, "озу", "random", "access", "memory", "оперативная", "память");
        addExpansion(expansions, "ssd", "solid", "state", "drive", "твердотельный", "накопитель");
        return Map.copyOf(expansions);
    }

    private static void addExpansion(Map<String, List<String>> expansions, String term, String... words) {
        List<String> normalizedWords = new ArrayList<>();
        for (String word : words) {
            normalizedWords.add(word.toLowerCase(Locale.ROOT));
        }
        expansions.put(term.toLowerCase(Locale.ROOT), List.copyOf(normalizedWords));
    }
}
