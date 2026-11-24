package com.classora.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankingTest {

    @Test
    void equalAveragesShareARankAndTheNextRankIsSkipped() {
        List<Ranking.Ranked> ranked = Ranking.rank(List.of(
                new Ranking.Input(1, "Zed", "A", 80),
                new Ranking.Input(2, "Amy", "B", 80),
                new Ranking.Input(3, "Bob", "C", 40),
                new Ranking.Input(4, "Cat", "D", 90)));

        assertEquals(List.of(1, 2, 2, 4), ranked.stream().map(Ranking.Ranked::rank).toList());
        assertEquals(List.of(4, 2, 1, 3), ranked.stream().map(Ranking.Ranked::studentId).toList());
    }

    @Test
    void passMarkIsFiftyPercent() {
        List<Ranking.Ranked> ranked = Ranking.rank(List.of(
                new Ranking.Input(1, "A", "A", 50),
                new Ranking.Input(2, "B", "B", 49.9)));
        assertTrue(ranked.get(0).passed());
        assertFalse(ranked.get(1).passed());
    }

    @Test
    void emptyInputGivesEmptyRanking() {
        assertTrue(Ranking.rank(List.of()).isEmpty());
    }
}
