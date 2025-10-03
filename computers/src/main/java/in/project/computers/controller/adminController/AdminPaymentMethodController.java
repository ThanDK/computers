package in.project.computers.controller.adminController;

import in.project.computers.DTO.payment.PaymentMethodRequest;
import in.project.computers.DTO.payment.PaymentMethodResponse;
import in.project.computers.service.paymentService.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payment-methods")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<PaymentMethodResponse> createPaymentMethod(
            @Valid @RequestPart("request") PaymentMethodRequest request,
            @RequestPart("qrCodeImage") MultipartFile qrCodeImage) {
        log.info("Admin request to create a new payment method for bank: {}", request.getBankName());
        PaymentMethodResponse response = paymentMethodService.createPaymentMethod(request, qrCodeImage);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> getAllPaymentMethods() {
        log.info("Admin request to fetch all payment methods.");
        List<PaymentMethodResponse> response = paymentMethodService.getAllPaymentMethods();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentMethodResponse> getPaymentMethodById(@PathVariable String id) {
        log.info("Admin request to fetch payment method with ID: {}", id);
        PaymentMethodResponse response = paymentMethodService.getPaymentMethodById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<PaymentMethodResponse> updatePaymentMethod(
            @PathVariable String id,
            @Valid @RequestPart("request") PaymentMethodRequest request,
            @RequestPart(value = "qrCodeImage", required = false) MultipartFile qrCodeImage) {
        log.info("Admin request to update payment method with ID: {}", id);
        PaymentMethodResponse response = paymentMethodService.updatePaymentMethod(id, request, qrCodeImage);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaymentMethod(@PathVariable String id) {
        log.info("Admin request to delete payment method with ID: {}", id);
        paymentMethodService.deletePaymentMethod(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/set-default")
    public ResponseEntity<PaymentMethodResponse> setDefaultPaymentMethod(@PathVariable String id) {
        log.info("Admin request to set payment method with ID: {} as default.", id);
        PaymentMethodResponse response = paymentMethodService.setDefaultPaymentMethod(id);
        return ResponseEntity.ok(response);
    }
}