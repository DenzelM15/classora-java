package com.classora.service;

import com.classora.dto.Dtos.OptionView;
import com.classora.dto.Dtos.QuestionView;
import com.classora.dto.Dtos.QuizInfo;
import com.classora.dto.Dtos.QuizSummary;
import com.classora.model.Question;
import com.classora.model.Quiz;
import com.classora.model.QuizOption;
import com.classora.util.DateTimes;

import java.util.Comparator;
import java.util.List;

/** Entity -> response conversions for quizzes. Never exposes which option is correct. */
final class QuizMapper {

    private QuizMapper() {
    }

    static QuizInfo info(Quiz q) {
        return new QuizInfo(q.getId(), q.getTitle(), q.getDescription(),
                DateTimes.toIso(q.getCreatedAt()), q.getTimeLimit(), q.getAttemptsAllowed(),
                DateTimes.toIso(q.getStartAt()), DateTimes.toIso(q.getEndAt()), q.isPublished());
    }

    static QuizSummary summary(Quiz q, long questionCount, Integer attemptsUsed, Integer unfinished) {
        return new QuizSummary(q.getId(), q.getTitle(), q.getDescription(),
                DateTimes.toIso(q.getCreatedAt()), q.getTimeLimit(), q.getAttemptsAllowed(),
                DateTimes.toIso(q.getStartAt()), DateTimes.toIso(q.getEndAt()), q.isPublished(),
                (int) questionCount, attemptsUsed, unfinished);
    }

    static List<QuestionView> questions(List<Question> questions) {
        return questions.stream()
                .sorted(Comparator.comparingInt(Question::getId))
                .map(q -> new QuestionView(q.getId(), q.getText(),
                        q.getOptions().stream()
                                .sorted(Comparator.comparingInt(QuizOption::getId))
                                .map(o -> new OptionView(o.getId(), o.getText()))
                                .toList()))
                .toList();
    }
}
