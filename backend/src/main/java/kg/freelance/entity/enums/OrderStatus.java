package kg.freelance.entity.enums;

public enum OrderStatus {
    NEW,           // Новый, ищет исполнителя
    IN_PROGRESS,   // В работе
    REVISION,      // На доработке
    ON_REVIEW,     // На проверке у заказчика
    COMPLETED,     // Завершен успешно
    DISPUTED,      // Спор
    CANCELLED      // Отменен
}
