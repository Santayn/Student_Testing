package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ImageSearchServiceTests {

    private static ImageSearchService.FoundImage image(String source) {
        return new ImageSearchService.FoundImage(source, "t", "https://upload.wikimedia.org/a.jpg", "a", "CC0", "", "");
    }

    /** Wikimedia отвечает сразу, Openverse — через openverseDelayMs (или падает). */
    private static class StubSearch extends ImageSearchService {
        final AtomicInteger wikimediaCalls = new AtomicInteger();
        final AtomicInteger openverseCalls = new AtomicInteger();
        volatile long openverseDelayMs;

        StubSearch(long openverseDelayMs) {
            super(new AiProperties(), null, new ObjectMapper());
            this.openverseDelayMs = openverseDelayMs;
        }

        @Override
        List<FoundImage> wikimedia(String query, int limit) {
            wikimediaCalls.incrementAndGet();
            return List.of(image("wikimedia"));
        }

        @Override
        List<FoundImage> openverse(String query, int limit) throws Exception {
            openverseCalls.incrementAndGet();
            Thread.sleep(openverseDelayMs);
            return List.of(image("openverse"));
        }
    }

    @Test
    void fastSourcesAreMergedAndCached() {
        StubSearch search = new StubSearch(10);

        ImageSearchService.SearchResult first = search.search("tree", 8);
        ImageSearchService.SearchResult second = search.search("Tree ", 8);

        assertThat(first.images()).extracting(ImageSearchService.FoundImage::source).containsExactly("wikimedia", "openverse");
        assertThat(first.unavailableSources()).isEmpty();
        assertThat(second).isSameAs(first);
        assertThat(search.wikimediaCalls).hasValue(1);
    }

    @Test
    void slowOpenverseDoesNotHoldResultsAndIsSkippedAfterwards() {
        StubSearch search = new StubSearch(20_000);

        long started = System.nanoTime();
        ImageSearchService.SearchResult result = search.search("database", 8);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;

        assertThat(elapsedMs).isLessThan(4_000);
        assertThat(result.images()).extracting(ImageSearchService.FoundImage::source).containsExactly("wikimedia");
        assertThat(result.unavailableSources()).containsExactly("openverse");

        search.openverseDelayMs = 10;
        ImageSearchService.SearchResult next = search.search("index", 8);
        assertThat(next.unavailableSources()).containsExactly("openverse");
        assertThat(search.openverseCalls).hasValue(1);
    }
}
