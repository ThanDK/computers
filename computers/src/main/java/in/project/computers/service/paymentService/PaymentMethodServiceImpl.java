package in.project.computers.service.paymentService;

import in.project.computers.DTO.payment.PaymentMethodRequest;
import in.project.computers.DTO.payment.PaymentMethodResponse;
import in.project.computers.entity.payment.PaymentMethod;
import in.project.computers.repository.generalReposiroty.PaymentMethodRepository;
import in.project.computers.service.awsS3Bucket.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final S3Service s3Service;

    @Override
    @Transactional
    public PaymentMethodResponse createPaymentMethod(PaymentMethodRequest request, MultipartFile qrCodeImage) {
        if (qrCodeImage == null || qrCodeImage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR Code image is required.");
        }

        String imageUrl = s3Service.uploadFile(qrCodeImage);
        log.info("Uploaded new QR code image to S3: {}", imageUrl);

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .bankName(request.getBankName())
                .accountName(request.getAccountName())
                .accountNumber(request.getAccountNumber())
                .qrCodeImageUrl(imageUrl)
                .isDefault(false) // New methods are never default initially
                .build();

        PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Created new payment method with ID: {}", savedMethod.getId());

        if (paymentMethodRepository.count() == 1) {
            savedMethod.setDefault(true);
            paymentMethodRepository.save(savedMethod);
            log.info("First payment method created. Setting it as default.");
        }

        return toResponse(savedMethod);
    }

    @Override
    public List<PaymentMethodResponse> getAllPaymentMethods() {
        return paymentMethodRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentMethodResponse getPaymentMethodById(String id) {
        PaymentMethod paymentMethod = findByIdOrThrow(id);
        return toResponse(paymentMethod);
    }

    @Override
    public PaymentMethodResponse getDefaultPaymentMethod() {
        return paymentMethodRepository.findByIsDefault(true)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No default payment method is set."));
    }

    @Override
    @Transactional
    public PaymentMethodResponse updatePaymentMethod(String id, PaymentMethodRequest request, MultipartFile qrCodeImage) {
        PaymentMethod paymentMethod = findByIdOrThrow(id);

        paymentMethod.setBankName(request.getBankName());
        paymentMethod.setAccountName(request.getAccountName());
        paymentMethod.setAccountNumber(request.getAccountNumber());

        if (qrCodeImage != null && !qrCodeImage.isEmpty()) {
            log.info("New QR code image provided for payment method ID: {}. Replacing old one.", id);

            try {
                String oldKey = s3Service.extractKeyFromUrl(paymentMethod.getQrCodeImageUrl());
                s3Service.deleteFileByKey(oldKey);
            } catch (Exception e) {
                log.error("Could not delete old QR code image from S3 for ID {}: {}", id, e.getMessage());
            }

            String newImageUrl = s3Service.uploadFile(qrCodeImage);
            paymentMethod.setQrCodeImageUrl(newImageUrl);
        }

        PaymentMethod updatedMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Updated payment method with ID: {}", updatedMethod.getId());
        return toResponse(updatedMethod);
    }

    @Override
    @Transactional
    public void deletePaymentMethod(String id) {
        PaymentMethod paymentMethod = findByIdOrThrow(id);

        if (paymentMethod.isDefault()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete the default payment method. Please set another method as default first.");
        }

        // Delete image from S3
        try {
            String key = s3Service.extractKeyFromUrl(paymentMethod.getQrCodeImageUrl());
            s3Service.deleteFileByKey(key);
            log.info("Deleted QR code image from S3 for payment method ID: {}", id);
        } catch (Exception e) {
            log.error("Could not delete QR code image from S3 for ID {}: {}", id, e.getMessage());
        }

        paymentMethodRepository.delete(paymentMethod);
        log.info("Deleted payment method with ID: {}", id);
    }

    @Override
    @Transactional
    public PaymentMethodResponse setDefaultPaymentMethod(String id) {
        PaymentMethod newDefault = findByIdOrThrow(id);

        if (newDefault.isDefault()) {
            log.warn("Attempted to set payment method {} as default, but it already is.", id);
            return toResponse(newDefault); // Already the default, do nothing.
        }

        // Find and unset the current default
        paymentMethodRepository.findByIsDefault(true).ifPresent(currentDefault -> {
            currentDefault.setDefault(false);
            paymentMethodRepository.save(currentDefault);
            log.info("Unset payment method {} as default.", currentDefault.getId());
        });

        // Set the new default
        newDefault.setDefault(true);
        PaymentMethod savedDefault = paymentMethodRepository.save(newDefault);
        log.info("Set payment method {} as the new default.", savedDefault.getId());

        return toResponse(savedDefault);
    }

    private PaymentMethod findByIdOrThrow(String id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment method not found with ID: " + id));
    }

    private PaymentMethodResponse toResponse(PaymentMethod entity) {
        return PaymentMethodResponse.builder()
                .id(entity.getId())
                .bankName(entity.getBankName())
                .accountName(entity.getAccountName())
                .accountNumber(entity.getAccountNumber())
                .qrCodeImageUrl(entity.getQrCodeImageUrl())
                .isDefault(entity.isDefault())
                .build();
    }
}