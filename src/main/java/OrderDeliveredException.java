public class OrderDeliveredException extends RuntimeException {
    public OrderDeliveredException(String message) {
        super(message);
    }

    public OrderDeliveredException(String reference, OrderStatusEnum status) {
        super("La commande " + reference + " a le statut " + status +
                " et ne peut plus être modifiée");
    }
}
