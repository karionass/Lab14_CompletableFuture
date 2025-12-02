import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OrderProcessor {
    public static void main(String[] args) {
        OrderProcessingService service = new OrderProcessingService();

        // Тестовые заказы
        List<Order> orders = Arrays.asList(
                new Order("ORD001", "PROD001", 2, "customer1@example.com"), // Успешно
                new Order("ORD002", "PROD002", 10, "customer2@example.com"), // Большая скидка
                new Order("ORD003", "PROD003", 5, "customer3@example.com"),
                new Order("ORD004", "PROD004", 1, "customer4@example.com"),
                new Order("ORD005", "PROD005", 3, "customer5@example.com"),
                new Order("ORD006", "PROD999", 1, "customer6@example.com"), // Несуществующий товар
                new Order("ORD007", "PROD001", 100, "customer7@example.com") // Слишком большое количество
        );

        long totalStartTime = System.currentTimeMillis();

        try {
            // Обработка всех заказов параллельно
            CompletableFuture<List<OrderResult>> resultsFuture =
                    service.processMultipleOrders(orders);

            List<OrderResult> results = resultsFuture.join();
            printResults(results, totalStartTime);
        } finally {
            service.shutdown();
        }
    }

    private static void printResults(List<OrderResult> results, long totalStartTime) {
        long totalDuration = System.currentTimeMillis() - totalStartTime;

        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║ ИТОГОВЫЕ РЕЗУЛЬТАТЫ ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        // Сбор статистики
        List<OrderResult> successful = results.stream().filter(OrderResult::isSuccess).collect(Collectors.toList());
        List<OrderResult> failed = results.stream().filter(r -> !r.isSuccess()).collect(Collectors.toList());

        double totalAmount = successful.stream().mapToDouble(OrderResult::getTotalAmount).sum();

        System.out.println("Успешные заказы:");
        successful.forEach(r ->
                System.out.printf(" ✓ %s - %.2fT - %s%n", r.getOrderId(), r.getTotalAmount(), r.getMessage()));

        System.out.println("\nНеуспешные заказы:");
        failed.forEach(r ->
                System.out.printf(" ✗ %s - %s%n", r.getOrderId(), r.getMessage().replace("Ошибка обработки: ", "")));

        int totalOrders = results.size();
        int successfulCount = successful.size();
        int failedCount = failed.size();
        double successRate = (double) successfulCount / totalOrders * 100;

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("\nСТАТИСТИКА:");
        System.out.printf(" Всего заказов: %d%n", totalOrders);
        System.out.printf(" Успешных: %d (%.1f%%)%n", successfulCount, successRate);
        System.out.printf(" Неуспешных: %d (%.1f%%)%n", failedCount, 100.0 - successRate);
        System.out.printf(" Общая сумма: %.2fT%n", totalAmount);
        System.out.printf(" Общее время обработки: %d мс%n", totalDuration);
        System.out.println("═══════════════════════════════════════════════");
    }
}