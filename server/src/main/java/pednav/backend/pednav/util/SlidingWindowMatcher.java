package pednav.backend.pednav.util;

import java.util.*;
import java.util.function.BiConsumer;

public class SlidingWindowMatcher<T> {

    private final long windowMs;

    public SlidingWindowMatcher(long windowMs) {
        this.windowMs = windowMs;
    }

    /**
     * timestamp 기반 sliding window 매칭 알고리즘
     * @param buffer timestamp → payload map
     * @param matchCondition 두 payload가 매칭 가능한지 판단하는 조건
     * @param onMatch 매칭된 두 payload를 처리하는 로직
     */
    public void match(
            Map<Long, T> buffer,
            BiConsumer<Long, Long> onMatch,
            BiConsumer<T, T> matchProcessor,
            MatchValidator<T> matchCondition
    ) {
        List<Long> timestamps = new ArrayList<>(buffer.keySet());
        timestamps.sort(Long::compareTo);

        Set<Long> matched = new HashSet<>();

        for (int i = 0; i < timestamps.size(); i++) {
            long ts1 = timestamps.get(i);
            if (matched.contains(ts1)) continue;
            T p1 = buffer.get(ts1);
            if (p1 == null) continue;

            for (int j = i + 1; j < timestamps.size(); j++) {
                long ts2 = timestamps.get(j);
                if (matched.contains(ts2)) continue;
                if (Math.abs(ts1 - ts2) > windowMs) break;

                T p2 = buffer.get(ts2);
                if (p2 == null) continue;

                if (matchCondition.canMatch(p1, p2)) {
                    matchProcessor.accept(p1, p2);
                    onMatch.accept(ts1, ts2);
                    matched.add(ts1);
                    matched.add(ts2);
                    break;
                }
            }
        }
    }

    @FunctionalInterface
    public interface MatchValidator<T> {
        boolean canMatch(T a, T b);
    }
}
