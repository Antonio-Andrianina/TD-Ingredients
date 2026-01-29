import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Order {
    private Integer id;
    private String reference;
    private Instant creationDatetime;
    private List<DishOrder> dishOrderList;
    private OrderTypeEnum orderType;      // Nouveau
    private OrderStatusEnum orderStatus;  // Nouveau

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }

    public void setCreationDatetime(Instant creationDatetime) {
        this.creationDatetime = creationDatetime;
    }

    public List<DishOrder> getDishOrderList() {
        return dishOrderList;
    }

    public void setDishOrderList(List<DishOrder> dishOrderList) {
        this.dishOrderList = dishOrderList;
    }

    public OrderTypeEnum getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderTypeEnum orderType) {
        this.orderType = orderType;
    }

    public OrderStatusEnum getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatusEnum orderStatus) {
        this.orderStatus = orderStatus;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", creationDatetime=" + creationDatetime +
                ", orderType=" + orderType +
                ", orderStatus=" + orderStatus +
                ", dishOrderList=" + dishOrderList +
                '}';
    }

    Double getTotalAmountWithoutVat() {
        if (dishOrderList == null || dishOrderList.isEmpty()) {
            return 0.0;
        }
        return dishOrderList.stream()
                .mapToDouble(dishOrder ->
                        dishOrder.getDish().getPrice() * dishOrder.getQuantity())
                .sum();
    }

    Double getTotalAmountWithVat() {
        double totalWithoutVat = getTotalAmountWithoutVat();
        double vatRate = 0.20; // 20% de TVA
        return totalWithoutVat * (1 + vatRate);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id) &&
                Objects.equals(reference, order.reference) &&
                Objects.equals(creationDatetime, order.creationDatetime) &&
                Objects.equals(dishOrderList, order.dishOrderList) &&
                orderType == order.orderType &&
                orderStatus == order.orderStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reference, creationDatetime, orderType, orderStatus, dishOrderList);
    }
}