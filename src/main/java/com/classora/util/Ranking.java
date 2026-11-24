package com.classora.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Standard competition ranking: equal averages share a rank (1, 1, 3, ...). */
public final class Ranking {

    private Ranking() {
    }

    public record Input(int studentId, String name, String surname, double averagePercentage) {
    }

    public record Ranked(int rank, int studentId, String name, String surname, boolean passed) {
    }

    public static List<Ranked> rank(List<Input> students) {
        List<Input> sorted = new ArrayList<>(students);
        sorted.sort(Comparator
                .comparingDouble(Input::averagePercentage).reversed()
                .thenComparing(Input::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(Input::studentId));

        List<Ranked> result = new ArrayList<>();
        double previous = Double.NaN;
        int currentRank = 0;

        for (int i = 0; i < sorted.size(); i++) {
            Input s = sorted.get(i);
            if (i == 0 || Double.compare(s.averagePercentage(), previous) != 0) {
                currentRank = i + 1;
            }
            previous = s.averagePercentage();
            result.add(new Ranked(currentRank, s.studentId(), s.name(), s.surname(),
                    Grading.isPass(s.averagePercentage())));
        }
        return result;
    }
}
