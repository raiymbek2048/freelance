package kg.freelance.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum ReputationLevel {

    NEWCOMER("Новичок", "gray"),
    BEGINNER("Начинающий", "blue"),
    EXPERIENCED("Опытный", "green"),
    PROFESSIONAL("Профессионал", "purple"),
    EXPERT("Эксперт", "amber");

    private final String label;
    private final String color;

    public static ReputationLevel calculate(int completedOrders, BigDecimal rating) {
        double r = rating != null ? rating.doubleValue() : 0.0;
        if (completedOrders >= 50 && r >= 4.5) return EXPERT;
        if (completedOrders >= 20 && r >= 4.0) return PROFESSIONAL;
        if (completedOrders >= 5 && r >= 3.0) return EXPERIENCED;
        if (completedOrders >= 1) return BEGINNER;
        return NEWCOMER;
    }
}
