package com.classora.web;

import com.classora.model.Role;
import com.classora.service.ApiException;
import com.classora.service.AttemptService;
import com.classora.service.QuizService;
import com.classora.service.QuizService.CreateQuizCommand;
import com.classora.service.QuizService.OptionDraft;
import com.classora.service.QuizService.QuestionDraft;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizController {

    private static final String INVALID_QUIZ = "Invalid quiz ID";

    private final QuizService quizzes;
    private final AttemptService attempts;

    public QuizController(QuizService quizzes, AttemptService attempts) {
        this.quizzes = quizzes;
        this.attempts = attempts;
    }

    public void register(Router router) {
        router.get("/quizzes", Access.authenticated(),
                ctx -> ApiResponse.ok(quizzes.list(ctx.caller())));

        router.get("/quizzes/{id}", Access.authenticated(),
                ctx -> ApiResponse.ok(quizzes.get(ctx.caller(), ctx.pathInt("id", INVALID_QUIZ))));

        router.post("/quizzes", Access.staff(),
                ctx -> ApiResponse.created(quizzes.create(parseCreate(ctx.body()))));

        router.patch("/quizzes/{id}/status", Access.staff(),
                ctx -> ApiResponse.ok(quizzes.toggleStatus(ctx.pathInt("id", INVALID_QUIZ))));

        router.delete("/quizzes/{id}", Access.staff(),
                ctx -> ApiResponse.ok(quizzes.delete(ctx.pathInt("id", INVALID_QUIZ))));

        router.post("/quizzes/{id}/start", Access.roles(Role.STUDENT),
                ctx -> ApiResponse.created(attempts.start(ctx.caller(), ctx.pathInt("id", INVALID_QUIZ))));

        router.post("/quizzes/{id}/submit", Access.roles(Role.STUDENT), ctx -> {
            int quizId = ctx.pathInt("id", INVALID_QUIZ);
            Integer attemptId = Json.integer(ctx.body(), "attemptId");
            if (attemptId == null) {
                throw ApiException.badRequest("Invalid quiz attempt");
            }
            JsonNode answers = ctx.body().get("answers");
            if (answers == null || !answers.isArray()) {
                throw ApiException.badRequest("Answers must be an array");
            }
            return ApiResponse.created(attempts.submit(ctx.caller(), quizId, attemptId, parseAnswers(answers)));
        });
    }

    /** question id -> option id. Entries without usable numbers are skipped (unanswered). */
    static Map<Integer, Integer> parseAnswers(JsonNode answers) {
        Map<Integer, Integer> chosen = new HashMap<>();
        for (JsonNode answer : answers) {
            Integer questionId = Json.integer(answer, "questionId");
            Integer optionId = Json.integer(answer, "selectedOptionId");
            if (questionId != null && optionId != null) {
                chosen.put(questionId, optionId);
            }
        }
        return chosen;
    }

    static CreateQuizCommand parseCreate(JsonNode body) {
        List<QuestionDraft> questions = null;
        JsonNode questionNodes = body.get("questions");

        if (questionNodes != null && questionNodes.isArray()) {
            questions = new ArrayList<>();
            for (JsonNode q : questionNodes) {
                List<OptionDraft> options = null;
                JsonNode optionNodes = q.get("options");
                if (optionNodes != null && optionNodes.isArray()) {
                    options = new ArrayList<>();
                    for (JsonNode o : optionNodes) {
                        options.add(new OptionDraft(Json.text(o, "text"), Json.isTrue(o, "isCorrect")));
                    }
                }
                questions.add(new QuestionDraft(Json.text(q, "question"), options));
            }
        }

        return new CreateQuizCommand(
                Json.text(body, "title"),
                Json.text(body, "description"),
                Json.integer(body, "timeLimit"),
                Json.integer(body, "attemptsAllowed"),
                Json.text(body, "startAt"),
                Json.text(body, "endAt"),
                Json.isTrue(body, "published"),
                questions);
    }
}
