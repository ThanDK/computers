package in.project.computers.service.orderService;

import com.paypal.base.rest.PayPalRESTException;
import in.project.computers.DTO.order.orderRequest.CreateOrderRequest;
import in.project.computers.DTO.order.orderResponse.CreateOrderResponse;
import in.project.computers.DTO.order.orderResponse.OrderResponse;
import in.project.computers.DTO.order.orderRequest.ShipOrderRequest;
import in.project.computers.entity.order.OrderStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OrderService {

    CreateOrderResponse createOrder(CreateOrderRequest request) throws PayPalRESTException;

    void capturePaypalOrder(String orderId, String paymentId, String payerId) throws PayPalRESTException;

    OrderResponse submitPaymentSlip(String orderId, MultipartFile slipImage);

    OrderResponse getOrderById(String orderId);

    List<OrderResponse> getCurrentUserOrders();

    OrderResponse cancelOrderByUser(String orderId);

    // cancelOrderByAdmin is removed as it's redundant with forceRefundByAdmin

    CreateOrderResponse retryPayment(String orderId) throws PayPalRESTException;

    OrderResponse requestRefund(String orderId);

    OrderResponse approveRefund(String orderId, MultipartFile refundSlip) throws PayPalRESTException;

    OrderResponse forceRefundByAdmin(String orderId) throws PayPalRESTException;

    OrderResponse shipOrder(String orderId, ShipOrderRequest request);

    OrderResponse updateShippingDetails(String orderId, ShipOrderRequest request);

    OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus);

    List<OrderStatus> getValidNextStatuses(String orderId);

    OrderResponse rejectRefund(String orderId);

    List<OrderResponse> getAllOrders();

    OrderResponse approvePaymentSlip(String orderId);

    OrderResponse rejectPaymentSlip(String orderId, String reason);

    OrderResponse revertSlipApproval(String orderId, String reason);

    OrderResponse getAnyOrderByIdForAdmin(String orderId);

    OrderResponse updateRefundSlip(String orderId, MultipartFile newSlipImage);
}