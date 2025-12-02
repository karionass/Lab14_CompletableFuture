public class Order {
    private String orderId;
    private String productId;
    private int quantity;
    private String customerEmail;

    public Order(String orderId, String productId, int quantity, String customerEmail) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.customerEmail = customerEmail;
    }
    // Геттеры
    public String getOrderId() { return orderId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public String getCustomerEmail() { return customerEmail; }

    @Override
    public String toString() {
        return String.format("Order (id='%s', product='%s', qty=%d)", orderId, productId, quantity);
    }
}