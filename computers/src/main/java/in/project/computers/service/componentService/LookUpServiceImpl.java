package in.project.computers.service.componentService;

import in.project.computers.entity.lookup.FormFactor;
import in.project.computers.entity.lookup.FormFactorType;
import in.project.computers.repository.ComponentRepo.lookup.FormFactorRepository;
import in.project.computers.repository.ComponentRepo.lookup.RamTypeRepository;
import in.project.computers.repository.ComponentRepo.lookup.SocketRepository;
import in.project.computers.repository.ComponentRepo.lookup.StorageInterfaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LookUpServiceImpl implements LookupService{
    private final SocketRepository socketRepository;
    private final RamTypeRepository ramTypeRepository;
    private final FormFactorRepository formFactorRepository;
    private final StorageInterfaceRepository storageInterfaceRepository;

    public Map<String, Object> getAllLookups() {
        Map<String, Object> lookups = new HashMap<>();

        // Simple lists are fine
        lookups.put("sockets", socketRepository.findAll());
        lookups.put("ramTypes", ramTypeRepository.findAll());
        lookups.put("storageInterfaces", storageInterfaceRepository.findAll());

        // For FormFactors, it's better to group them by type for the UI
        Map<FormFactorType, List<FormFactor>> groupedFormFactors = formFactorRepository.findAll().stream()
                .collect(Collectors.groupingBy(FormFactor::getType));
        lookups.put("formFactors", groupedFormFactors);

        // Add a static list for radiator sizes
        lookups.put("radiatorSizes", List.of(120, 140, 240, 280, 360, 420));

        return lookups;
    }

//    // You can add methods here to create/update individual lookups later
//    // For example:
//    public Socket createSocket(Socket socket) {
//        // Add validation logic
//        return socketRepository.save(socket);
//    }
}
