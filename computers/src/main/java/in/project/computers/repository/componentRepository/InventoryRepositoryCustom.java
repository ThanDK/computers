package in.project.computers.repository.componentRepository;

import in.project.computers.DTO.inventory.StockConflictInfo;

import java.util.List;
import java.util.Map;

public interface InventoryRepositoryCustom {
    void bulkAtomicUpdateQuantities(Map<String, Integer> quantityChanges);

    /**
     * Attempts to atomically reserve stock for a map of components and their required quantities.
     * This is an all-or-nothing operation. If any component has insufficient stock,
     * no stock will be reserved for any component.
     *
     * @param requiredStock A map where the key is the componentId and the value is the quantity to reserve.
     * @return An empty list if the reservation was successful. If the reservation fails,
     *         returns a list of StockConflictInfo objects detailing each component that was out of stock.
     */
    List<StockConflictInfo> attemptReservation(Map<String, Integer> requiredStock);
}