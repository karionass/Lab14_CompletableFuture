public class OrderResult {
    private String orderId;
    private boolean success;
    private String message;
    private double totalAmount;
    public OrderResult(String orderId, boolean success, String message, double totalAmount) {
        this.orderId = orderId;
        this.success = success;
        this.message = message;
        this.totalAmount = totalAmount;
    }
    // Геттеры
    public String getOrderId() { return orderId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public double getTotalAmount() { return totalAmount; }
    @Override
    public String toString() {
        return String.format("OrderResult{id='%s', success=%s, message='%s', amount=%.2f₸}",
                orderId, success, message, totalAmount);
    }
}