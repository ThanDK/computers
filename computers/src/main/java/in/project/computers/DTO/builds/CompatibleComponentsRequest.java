package in.project.computers.DTO.builds;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a user's current computer build configuration, which may be partial.
 * Used to request a list of compatible components for the empty slots.
 * This DTO supports single selections (like CPU) and multiple selections with quantities (like Storage Drives).
 */
@Data
@NoArgsConstructor
public class CompatibleComponentsRequest {

    private String cpuId;
    private String motherboardId;
    private String psuId;
    private String caseId;
    private String coolerId;


    private Map<String, Integer> ramKits;


    private Map<String, Integer> gpus;


    private Map<String, Integer> storageDrives;
}