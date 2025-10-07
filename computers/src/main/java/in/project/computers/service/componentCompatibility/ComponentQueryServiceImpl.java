package in.project.computers.service.componentCompatibility;

import in.project.computers.entity.component.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComponentQueryServiceImpl implements ComponentQueryService {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Cpu> findCompatibleCpus(CompatibilityContext context) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("type").is("cpu"));
        if (context.getRequiredSocketId() != null) {
            criteria.add(Criteria.where("socket._id").is(context.getRequiredSocketId()));
        }
        return executeQuery(criteria, Cpu.class);
    }

    @Override
    public List<Motherboard> findCompatibleMotherboards(CompatibilityContext context) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("type").is("motherboard"));

        if (context.getRequiredSocketId() != null) {
            criteria.add(Criteria.where("socket._id").is(context.getRequiredSocketId()));
        }
        if (context.getRequiredRamTypeId() != null) {
            criteria.add(Criteria.where("ramType._id").is(context.getRequiredRamTypeId()));
        }
        if (!CollectionUtils.isEmpty(context.getSupportedMoboFormFactorIds())) {
            criteria.add(Criteria.where("formFactor._id").in(context.getSupportedMoboFormFactorIds()));
        }
        if (context.getTotalRamModules() > 0) {
            criteria.add(Criteria.where("ram_slot_count").gte(context.getTotalRamModules()));
        }
        if (context.getTotalGpuCount() > 0) {
            criteria.add(Criteria.where("pcie_x16_slot_count").gte(context.getTotalGpuCount()));
        }
        return executeQuery(criteria, Motherboard.class);
    }

    @Override
    public List<RamKit> findCompatibleRamKits(CompatibilityContext context) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("type").is("ram"));
        if (context.getRequiredRamTypeId() != null) {
            criteria.add(Criteria.where("ramType._id").is(context.getRequiredRamTypeId()));
        }
        return executeQuery(criteria, RamKit.class);
    }

    @Override
    public List<Gpu> findCompatibleGpus(CompatibilityContext context) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("type").is("gpu"));
        if (context.getMaxGpuLengthMm() != null && context.getMaxGpuLengthMm() > 0) {
            criteria.add(Criteria.where("length_mm").lte(context.getMaxGpuLengthMm()));
        }
        return executeQuery(criteria, Gpu.class);
    }

    @Override
    public List<Case> findCompatibleCases(CompatibilityContext context) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("type").is("case"));
        if (context.getSelectedMotherboard() != null && context.getSelectedMotherboard().getFormFactor() != null) {
            criteria.add(Criteria.where("supportedFormFactors._id").is(context.getSelectedMotherboard().getFormFactor().getId()));
        }
        if (context.getSelectedPsu() != null && context.getSelectedPsu().getFormFactor() != null) {
            criteria.add(Criteria.where("supportedPsuFormFactors._id").is(context.getSelectedPsu().getFormFactor().getId()));
        }
        if (!CollectionUtils.isEmpty(context.getSelectedGpus())) {
            int maxGpuLength = context.getSelectedGpus().stream()
                    .mapToInt(g -> g.getComponent().getLength_mm()).max().orElse(0);
            if (maxGpuLength > 0) {
                criteria.add(Criteria.where("max_gpu_length_mm").gte(maxGpuLength));
            }
        }
        if (context.getSelectedCooler() != null) {
            Cooler cooler = context.getSelectedCooler();
            boolean isAio = cooler.getRadiatorSize_mm() >= 120;
            if (isAio) {
                criteria.add(Criteria.where("supportedRadiatorSizesMm").is(cooler.getRadiatorSize_mm()));
            } else {
                criteria.add(Criteria.where("max_cooler_height_mm").gte(cooler.getHeight_mm()));
            }
        }
        return executeQuery(criteria, Case.class);
    }

    @Override
    public List<Psu> findCompatiblePsus(CompatibilityContext context) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("type").is("psu"));
        criteria.add(Criteria.where("wattage").gte(context.getRequiredWattage()));
        if (!CollectionUtils.isEmpty(context.getSupportedPsuFormFactorIds())) {
            // THE FIX IS HERE: Query against the DBRef field directly, not a nested "_id".
            criteria.add(Criteria.where("formFactor").in(context.getSupportedPsuFormFactorIds()));
        }
        return executeQuery(criteria, Psu.class);
    }

    @Override
    public List<Cooler> findCompatibleCoolers(CompatibilityContext context) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("type").is("cooler"));
        if (context.getRequiredSocketId() != null) {
            criteria.add(Criteria.where("supportedSockets._id").is(context.getRequiredSocketId()));
        }

        if (context.getMaxCoolerHeightMm() != null || !CollectionUtils.isEmpty(context.getSupportedRadiatorSizesMm())) {
            List<Criteria> sizeConstraints = new ArrayList<>();
            if(context.getMaxCoolerHeightMm() != null) {
                sizeConstraints.add(new Criteria().andOperator(
                        Criteria.where("radiatorSize_mm").lt(120),
                        Criteria.where("height_mm").lte(context.getMaxCoolerHeightMm())
                ));
            }
            if(!CollectionUtils.isEmpty(context.getSupportedRadiatorSizesMm())) {
                sizeConstraints.add(new Criteria().andOperator(
                        Criteria.where("radiatorSize_mm").gte(120),
                        Criteria.where("radiatorSize_mm").in(context.getSupportedRadiatorSizesMm())
                ));
            }
            criteria.add(new Criteria().orOperator(sizeConstraints));
        }
        return executeQuery(criteria, Cooler.class);
    }

    @Override
    public List<StorageDrive> findCompatibleStorageDrives(CompatibilityContext context) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("type").is("storage"));
        return executeQuery(criteria, StorageDrive.class);
    }

    private <T> List<T> executeQuery(List<Criteria> criteria, Class<T> entityClass) {
        if (criteria.isEmpty()) {
            return mongoTemplate.findAll(entityClass);
        }
        Query query = new Query(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        return mongoTemplate.find(query, entityClass);
    }
}