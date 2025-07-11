package in.project.computers.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap; // Use LinkedHashMap for order
import java.util.Map;
import java.util.Optional;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = { ResponseStatusException.class })
    protected ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {


        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", ex.getStatusCode().value());


        String reason = Optional.ofNullable(ex.getReason()).orElse("No specific reason provided.");
        body.put("error", reason);
        //body.put("path", ((org.springframework.web.context.request.ServletWebRequest)request).getRequest().getRequestURI());

        return handleExceptionInternal(ex, body, new HttpHeaders(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(value = { Exception.class })
    protected ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 500);
        body.put("error", "An unexpected internal server error occurred.");
        return handleExceptionInternal(ex, body, new HttpHeaders(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}