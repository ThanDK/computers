package in.project.computers.util;

import in.project.computers.entity.lookup.*;
import in.project.computers.repository.ComponentRepo.lookup.FormFactorRepository;
import in.project.computers.repository.ComponentRepo.lookup.RamTypeRepository;
import in.project.computers.repository.ComponentRepo.lookup.SocketRepository;
import in.project.computers.repository.ComponentRepo.lookup.StorageInterfaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final SocketRepository socketRepository;
    private final RamTypeRepository ramTypeRepository;
    private final FormFactorRepository formFactorRepository;
    private final StorageInterfaceRepository storageInterfaceRepository;

    @Override
    public void run(String... args) {
        System.out.println("Checking and loading lookup data...");

        seedSockets();
        seedRamTypes();
        seedFormFactors();
        seedStorageInterfaces();

        System.out.println("Data loading complete.");
    }


    private void seedSockets() {

        List<Socket> desiredSockets = List.of(
                new Socket(null, "AM5", "AMD"),
                new Socket(null, "AM4", "AMD"),
                new Socket(null, "LGA1700", "Intel"),
                new Socket(null, "LGA1200", "Intel"),
                new Socket(null, "TR5", "AMD")
        );


        Set<String> existingSocketNames = socketRepository.findAll()
                .stream()
                .map(Socket::getName)
                .collect(Collectors.toSet());

        List<Socket> newSockets = desiredSockets.stream()
                .filter(socket -> !existingSocketNames.contains(socket.getName()))
                .collect(Collectors.toList());

        if (!newSockets.isEmpty()) {
            System.out.println("Seeding " + newSockets.size() + " new Sockets...");
            socketRepository.saveAll(newSockets);
        }
    }

    private void seedRamTypes() {
        List<String> desiredNames = List.of(
                "DDR5",
                "DDR4",
                "DDR3"
        );
        Set<String> existingNames = ramTypeRepository.findAll().stream().map(RamType::getName).collect(Collectors.toSet());

        List<RamType> newItems = desiredNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> new RamType(null, name))
                .collect(Collectors.toList());

        if (!newItems.isEmpty()) {
            System.out.println("Seeding " + newItems.size() + " new RAM Types...");
            ramTypeRepository.saveAll(newItems);
        }
    }

    private void seedFormFactors() {
        List<FormFactor> desiredFormFactors = List.of(
                // Motherboard Form Factors
                new FormFactor(null, "ATX", FormFactorType.MOTHERBOARD),
                new FormFactor(null, "Micro-ATX", FormFactorType.MOTHERBOARD),
                new FormFactor(null, "Mini-ITX", FormFactorType.MOTHERBOARD),

                // PSU Form Factors
                new FormFactor(null, "ATX", FormFactorType.PSU),
                new FormFactor(null, "SFX", FormFactorType.PSU),
                new FormFactor(null, "SFX-L", FormFactorType.PSU),

                // Storage Form Factors
                new FormFactor(null, "2.5 inch", FormFactorType.STORAGE),
                new FormFactor(null, "3.5 inch", FormFactorType.STORAGE),
                new FormFactor(null, "M.2 2280", FormFactorType.STORAGE)
        );

        // Use a Set of a simple string "name:type" to check for existence
        Set<String> existingFormFactors = formFactorRepository.findAll().stream()
                .map(ff -> ff.getName() + ":" + ff.getType())
                .collect(Collectors.toSet());

        List<FormFactor> newItems = desiredFormFactors.stream()
                .filter(ff -> !existingFormFactors.contains(ff.getName() + ":" + ff.getType()))
                .collect(Collectors.toList());

        if (!newItems.isEmpty()) {
            System.out.println("Seeding " + newItems.size() + " new Form Factors...");
            formFactorRepository.saveAll(newItems);
        }
    }


    private void seedStorageInterfaces() {
        List<String> desiredNames = List.of(
                "NVMe",
                "NVMe M.2",
                "SATA III",
                "SATA II"
        );
        Set<String> existingNames = storageInterfaceRepository.findAll().stream().map(StorageInterface::getName).collect(Collectors.toSet());

        List<StorageInterface> newItems = desiredNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> new StorageInterface(null, name))
                .collect(Collectors.toList());

        if (!newItems.isEmpty()) {
            System.out.println("Seeding " + newItems.size() + " new Storage Interfaces...");
            storageInterfaceRepository.saveAll(newItems);
        }
    }
}