package in.project.computers.exception;

import in.project.computers.DTO.inventory.StockConflictInfo;
import lombok.Getter;

import java.util.List;

@Getter
public class StockConflictException extends RuntimeException {

    private final List<StockConflictInfo> conflicts;

    public StockConflictException(List<StockConflictInfo> conflicts) {
        super(buildErrorMessage(conflicts));
        this.conflicts = conflicts;
    }

    private static String buildErrorMessage(List<StockConflictInfo> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return "An unspecified stock conflict occurred.";
        }
        // Example: "Insufficient stock for: 'Nvidia RTX 4090' (requested: 2, available: 1)"
        return "Insufficient stock for one or more items.";
    }
}