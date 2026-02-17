package com.vocab.bulgarian.study.service;

import com.vocab.bulgarian.study.domain.SrsState;
import com.vocab.bulgarian.study.domain.enums.ReviewRating;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Service
public class SrsAlgorithmService {

    private static final BigDecimal MIN_EASE = new BigDecimal("1.30");
    private static final BigDecimal EASE_DECREASE = new BigDecimal("0.20");

    /**
     * Apply SM-2 algorithm to update SrsState based on rating.
     * CORRECT: interval 0→1→6→prev×EF (rounded), EF unchanged
     * INCORRECT: interval→1, repetitionCount→0, EF max(1.30, EF-0.20)
     */
    public SrsState applyRating(SrsState state, ReviewRating rating) {
        if (rating == ReviewRating.CORRECT) {
            int newInterval = switch (state.getRepetitionCount()) {
                case 0 -> 1;
                case 1 -> 6;
                default -> (int) Math.round(state.getIntervalDays() * state.getEaseFactor().doubleValue());
            };
            state.setIntervalDays(Math.max(newInterval, 1));
            state.setRepetitionCount(state.getRepetitionCount() + 1);
        } else {
            state.setIntervalDays(1);
            state.setRepetitionCount(0);
            BigDecimal newEase = state.getEaseFactor().subtract(EASE_DECREASE);
            state.setEaseFactor(newEase.max(MIN_EASE).setScale(2, RoundingMode.HALF_UP));
        }
        state.setNextReviewDate(LocalDate.now().plusDays(state.getIntervalDays()));
        state.setLastReviewedAt(ZonedDateTime.now());
        return state;
    }
}
