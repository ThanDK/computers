package in.project.computers.controller.userController;

import in.project.computers.DTO.payment.PaymentMethodResponse;
import in.project.computers.service.paymentService.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
@Slf4j
public class PublicPaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping("/default")
    public ResponseEntity<PaymentMethodResponse> getDefaultPaymentMethod() {
        log.info("Public request to fetch the default payment method.");
        PaymentMethodResponse response = paymentMethodService.getDefaultPaymentMethod();
        return ResponseEntity.ok(response);
    }
}