package in.project.computers.dto.builds;

import in.project.computers.dto.component.componentResponse.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ComputerBuildDetailResponse {
    private String id;
    private String userId;
    private String buildName;


    private CpuResponse cpu;
    private MotherboardResponse motherboard;
    private PsuResponse psu;
    private CaseResponse caseDetail;
    private CoolerResponse cooler;


    private List<BuildPartDetail<RamKitResponse>> ramKits;
    private List<BuildPartDetail<GpuResponse>> gpus;
    private List<BuildPartDetail<StorageDriveResponse>> storageDrives;

    private BigDecimal totalPrice;
}