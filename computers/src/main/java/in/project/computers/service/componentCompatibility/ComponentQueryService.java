package in.project.computers.service.componentCompatibility;

import in.project.computers.entity.component.*;

import java.util.List;

public interface ComponentQueryService {

    List<Cpu> findCompatibleCpus(CompatibilityContext context);

    List<Motherboard> findCompatibleMotherboards(CompatibilityContext context);

    List<RamKit> findCompatibleRamKits(CompatibilityContext context);

    List<Gpu> findCompatibleGpus(CompatibilityContext context);

    List<Case> findCompatibleCases(CompatibilityContext context);

    List<Psu> findCompatiblePsus(CompatibilityContext context);

    List<Cooler> findCompatibleCoolers(CompatibilityContext context);

    List<StorageDrive> findCompatibleStorageDrives(CompatibilityContext context);
}