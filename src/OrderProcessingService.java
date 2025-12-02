import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class OrderProcessingService {
    private final ExecutorService executor;
    private final Map<String, Product> inventory;
    private final Random random;

    public OrderProcessingService() {
        this.executor = Executors.newFixedThreadPool(10);
        this.inventory = initializeInventory();
        this.random = new Random();
    }

    private Map<String, Product> initializeInventory() {
        Map<String, Product> inv = new HashMap<>();
        inv.put("PROD001", new Product("PROD001", "Ноутбук", 150000.0, 10));
        inv.put("PROD002", new Product("PROD002", "Мышь", 3500.0, 50));
        inv.put("PROD003", new Product("PROD003", "Клавиатура", 8500.0, 30));
        inv.put("PROD004", new Product("PROD004", "Монитор", 85000.0, 15));
        inv.put("PROD005", new Product("PROD005", "Наушники", 12000.0, 25));
        return inv;
    }

    // 1. Проверка наличия товара
    public CompletableFuture<Product> checkProductAvailability(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.printf("[%s] Проверка наличия товара...%n", order.getOrderId());
                Thread.sleep(1000); // Симуляция задержки

                Product product = inventory.get(order.getProductId());

                if (product == null) { // Проверка существования
                    throw new RuntimeException("Товар " + order.getProductId() + " не найден");
                }

                if (product.getStockQuantity() < order.getQuantity()) { // Проверка количества
                    throw new RuntimeException("Недостаточно товара на складе. Требуется: " + order.getQuantity() +
                            ", в наличии: " + product.getStockQuantity());
                }

                System.out.printf("[%s] Товар найден: %s%n", order.getOrderId(), product.toString());
                return product;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Прервано", e);
            }
        }, executor);
    }

    // 2. Расчет стоимости
    public CompletableFuture<Double> calculatePrice(Order order, Product product) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.printf("[%s] Расчет стоимости...%n", order.getOrderId());
                Thread.sleep(500); // Симуляция задержки

                double basePrice = product.getPrice() * order.getQuantity(); // Базовая стоимость
                double discount = 0.0;

                if (order.getQuantity() > 5) { // Скидка 10%
                    discount = basePrice * 0.10;
                }

                double priceAfterDiscount = basePrice - discount;
                double priceWithTax = priceAfterDiscount * 1.12; // Добавление налога 12%

                System.out.printf("[%s] Стоимость с учетом налога: %.2fT%n", order.getOrderId(), priceWithTax);
                return priceWithTax;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Прервано", e);
            }
        }, executor);
    }

    // 3. Обработка платежа
    public CompletableFuture<Boolean> processPayment(Order order, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.printf("[%s] Обработка платежа на сумму %.2f₸...%n",
                        order.getOrderId(), amount);
                Thread.sleep(2000); // Симуляция задержки

                if (random.nextDouble() < 0.10) { // С вероятностью 10% выбрасываем исключение
                    throw new RuntimeException("Payment failed (симуляция ошибки)");
                }

                System.out.printf("[%s] Платеж успешен%n", order.getOrderId());
                return true; // Иначе возвращаем true

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Прервано", e);
            }
        }, executor);
    }

    // 4. Резервирование товара
    public CompletableFuture<Void> reserveProduct(Order order, Product product) {
        // runAsync (значение не возвращаем, выполняем действие)
        return CompletableFuture.runAsync(() -> {
            try {
                System.out.printf("[%s] Резервирование товара %s...%n",
                        order.getOrderId(), product.getName());
                Thread.sleep(800); // Симуляция задержки

                synchronized (inventory) {
                    // 1. Создаем новый объект Product с уменьшенным запасом
                    int newStock = product.getStockQuantity() - order.getQuantity();

                    Product updatedProduct = new Product(
                            product.getProductId(),
                            product.getName(),
                            product.getPrice(),
                            newStock
                    );

                    // 2. Обновляем инвентарь
                    inventory.put(updatedProduct.getProductId(), updatedProduct);
                }

                System.out.printf("[%s] Товар зарезервирован (новый запас: %d)%n",
                        order.getOrderId(), inventory.get(order.getProductId()).getStockQuantity());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Прервано", e);
            }
        }, executor);
    }

    // 5. Отправка уведомления
    public CompletableFuture<Void> sendNotification(Order order, boolean success, double amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                System.out.printf("[%s] Отправка уведомления на %s...%n",
                        order.getOrderId(), order.getCustomerEmail());
                Thread.sleep(1000);

                String message = success ?
                        String.format("Заказ успешно оформлен на сумму %.2fT!", amount) :
                        "Не удалось оформить заказ. Пожалуйста, свяжитесь со службой поддержки.";

                System.out.printf("[%s] Уведомление отправлено: %s%n", order.getOrderId(), message);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Прервано", e);
            }
        }, executor);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Часть 2: Объединение всех этапов в один конвейер
    public CompletableFuture<OrderResult> processOrder(Order order) {
        System.out.printf("%n=== Начало обработки заказа %s ===%n", order.getOrderId());
        long startTime = System.currentTimeMillis();

        // 1. Проверка наличия
        return checkProductAvailability(order)
                // 2. Расчет цены и обработка платежа
                .thenCompose(product -> {
                    // Расчет цены
                    CompletableFuture<Double> priceFuture = calculatePrice(order, product);

                    // Обработка платежа после расчета цены
                    CompletableFuture<Boolean> paymentFuture = priceFuture
                            .thenCompose(price -> processPayment(order, price));

                    // Резервирование после успешного платежа
                    CompletableFuture<Void> reserveFuture = paymentFuture
                            .thenCompose(paymentSuccess -> {
                                if (paymentSuccess) {
                                    return reserveProduct(order, product);
                                }
                                throw new RuntimeException("Платеж не прошел");
                            });

                    // Объединяем результат цены и резервирования
                    return priceFuture.thenCombine(reserveFuture, (price, v) -> {
                        return price;
                    });
                })

                // 3. Отправка уведомления и создание OrderResult
                .thenCompose(finalPrice -> {
                    return sendNotification(order, true, finalPrice)
                            .thenApply(v -> new OrderResult( // Создаем успешный результат
                                    order.getOrderId(),
                                    true,
                                    "Заказ успешно обработан",
                                    finalPrice
                            ));
                })

                // 4. Таймаут: прерывает операцию, если > 10 сек
                .orTimeout(10, TimeUnit.SECONDS)

                // 5. Обработка ошибок: при исключении или успехе
                .handle((result, ex) -> {
                    if (ex != null) {
                        Throwable cause = (ex instanceof CompletionException || ex instanceof TimeoutException) ?
                                ex.getCause() != null ? ex.getCause() : ex : ex;

                        String errorMessage = "Ошибка обработки: " + cause.getMessage();
                        return new OrderResult(order.getOrderId(), false, errorMessage, 0.0);
                    }
                    return result;
                })

                // 6. Логирование
                .whenComplete((result, ex) -> {
                    long duration = System.currentTimeMillis() - startTime;
                    if (ex == null && result.isSuccess()) { // Успех
                        System.out.printf("[%s] ✓ Заказ успешно обработан за %d мс%n",
                                order.getOrderId(), duration);
                    } else {
                        String message = result != null ? result.getMessage() : "Неизвестная ошибка";
                        System.out.printf("[%s] ✗ Ошибка обработки: %s (за %d мс)%n",
                                order.getOrderId(), message, duration);
                    }
                });
    }

    // Часть 3: Обработка множества заказов параллельно
    public CompletableFuture<List<OrderResult>> processMultipleOrders(List<Order> orders) {
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║ ПАРАЛЛЕЛЬНАЯ ОБРАБОТКА ЗАКАЗОВ ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        // 1. Создание списка для каждого заказа
        List<CompletableFuture<OrderResult>> futures = orders.stream()
                .map(this::processOrder)
                .collect(Collectors.toList());

        // 2. allOf для ожидания завершения всех задач
        CompletableFuture<Void> allOfFuture = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        return allOfFuture.thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList())
        );
    }
}