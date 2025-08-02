package in.project.computers.DTO.builds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComputerBuildRequest {
    private String buildName;
    private String cpuId;
    private String motherboardId;
    private String psuId;
    private String caseId;
    private String coolerId;


    @Builder.Default
    private Map<String, Integer> ramKits = new HashMap<>();

    @Builder.Default
    private Map<String, Integer> gpus = new HashMap<>();

    @Builder.Default
    private Map<String, Integer> storageDrives = new HashMap<>();
}