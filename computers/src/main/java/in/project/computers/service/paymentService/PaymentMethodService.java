package in.project.computers.service.paymentService;

import in.project.computers.DTO.payment.PaymentMethodRequest;
import in.project.computers.DTO.payment.PaymentMethodResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PaymentMethodService {

    PaymentMethodResponse createPaymentMethod(PaymentMethodRequest request, MultipartFile qrCodeImage);

    List<PaymentMethodResponse> getAllPaymentMethods();

    PaymentMethodResponse getPaymentMethodById(String id);

    PaymentMethodResponse getDefaultPaymentMethod();

    PaymentMethodResponse updatePaymentMethod(String id, PaymentMethodRequest request, MultipartFile qrCodeImage);

    void deletePaymentMethod(String id);

    PaymentMethodResponse setDefaultPaymentMethod(String id);
}