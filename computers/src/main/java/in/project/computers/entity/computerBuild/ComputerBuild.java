package in.project.computers.entity.computerBuild;

import in.project.computers.entity.component.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "computer_builds")
public class ComputerBuild {
    @Id
    private String id;
    private String userId;
    private String buildName;

    private Cpu cpu;
    private Motherboard motherboard;
    private Psu psu;
    private Case caseDetail;
    private Cooler cooler;

    private List<BuildPart<RamKit>> ramKits;
    private List<BuildPart<Gpu>> gpus;
    private List<BuildPart<StorageDrive>> storageDrives;
}