package in.project.computers.service.componentService;

import in.project.computers.dto.lookup.*;
import in.project.computers.entity.lookup.*;
import in.project.computers.repository.ComponentRepo.ComponentRepository;
import in.project.computers.repository.ComponentRepo.lookup.FormFactorRepository;
import in.project.computers.repository.ComponentRepo.lookup.RamTypeRepository;
import in.project.computers.repository.ComponentRepo.lookup.SocketRepository;
import in.project.computers.repository.ComponentRepo.lookup.StorageInterfaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LookupServiceImpl implements LookupService {

    private final SocketRepository socketRepository;
    private final RamTypeRepository ramTypeRepository;
    private final FormFactorRepository formFactorRepository;
    private final StorageInterfaceRepository storageInterfaceRepository;
    private final ComponentRepository componentRepository;

    @Override
    public Map<String, Object> getAllLookups() {
        Map<String, Object> lookups = new HashMap<>();
        lookups.put("sockets", socketRepository.findAll());
        lookups.put("ramTypes", ramTypeRepository.findAll());
        lookups.put("storageInterfaces", storageInterfaceRepository.findAll());
        Map<FormFactorType, List<FormFactor>> groupedFormFactors = formFactorRepository.findAll().stream()
                .collect(Collectors.groupingBy(FormFactor::getType));
        lookups.put("formFactors", groupedFormFactors);
        lookups.put("radiatorSizes", List.of(120, 140, 240, 280, 360, 420));
        return lookups;
    }

    @Override
    public List<Socket> getAllSockets() { return socketRepository.findAll(); }
    @Override
    public List<RamType> getAllRamTypes() { return ramTypeRepository.findAll(); }
    @Override
    public List<FormFactor> getAllFormFactors() { return formFactorRepository.findAll(); }
    @Override
    public List<StorageInterface> getAllStorageInterfaces() { return storageInterfaceRepository.findAll(); }

    @Override
    public Socket createSocket(SocketRequest request) {
        if (socketRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Socket with name '" + request.getName() + "' already exists.");
        }
        Socket socket = new Socket(null, request.getName(), request.getBrand());
        return socketRepository.save(socket);
    }

    @Override
    public Socket updateSocket(String id, SocketRequest request) {
        Socket socket = socketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Socket not found with id: " + id));
        Optional<Socket> existingByName = socketRepository.findByName(request.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another socket with name '" + request.getName() + "' already exists.");
        }
        socket.setName(request.getName());
        socket.setBrand(request.getBrand());
        return socketRepository.save(socket);
    }

    @Override
    public void deleteSocket(String id) {
        if (!socketRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Socket not found with id: " + id);
        }
        if (componentRepository.existsBySocketId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Socket. It is currently in use by one or more components.");
        }
        socketRepository.deleteById(id);
    }

    @Override
    public RamType createRamType(RamTypeRequest request) {
        if (ramTypeRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RAM Type with name '" + request.getName() + "' already exists.");
        }
        RamType ramType = new RamType(null, request.getName());
        return ramTypeRepository.save(ramType);
    }

    @Override
    public RamType updateRamType(String id, RamTypeRequest request) {
        RamType ramType = ramTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RAM Type not found with id: " + id));
        Optional<RamType> existingByName = ramTypeRepository.findByName(request.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another RAM Type with name '" + request.getName() + "' already exists.");
        }
        ramType.setName(request.getName());
        return ramTypeRepository.save(ramType);
    }

    @Override
    public void deleteRamType(String id) {
        if (!ramTypeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "RAM Type not found with id: " + id);
        }
        if (componentRepository.existsByRamTypeId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete RAM Type. It is currently in use by one or more components.");
        }
        ramTypeRepository.deleteById(id);
    }

    @Override
    public FormFactor createFormFactor(FormFactorRequest request) {
        if (formFactorRepository.findByNameAndType(request.getName(), request.getType()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Form Factor with name '" + request.getName() + "' and type '" + request.getType() + "' already exists.");
        }
        FormFactor formFactor = new FormFactor(null, request.getName(), request.getType());
        return formFactorRepository.save(formFactor);
    }

    @Override
    public FormFactor updateFormFactor(String id, FormFactorRequest request) {
        FormFactor formFactor = formFactorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form Factor not found with id: " + id));

        Optional<FormFactor> existing = formFactorRepository.findByNameAndType(request.getName(), request.getType());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another Form Factor with name '" + request.getName() + "' and type '" + request.getType() + "' already exists.");
        }
        formFactor.setName(request.getName());
        formFactor.setType(request.getType());
        return formFactorRepository.save(formFactor);
    }

    @Override
    public void deleteFormFactor(String id) {
        if (!formFactorRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Form Factor not found with id: " + id);
        }
        if (componentRepository.existsByFormFactorId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Form Factor. It is currently in use by one or more components.");
        }
        formFactorRepository.deleteById(id);
    }

    @Override
    public StorageInterface createStorageInterface(StorageInterfaceRequest request) {
        if (storageInterfaceRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Storage Interface with name '" + request.getName() + "' already exists.");
        }
        StorageInterface storageInterface = new StorageInterface(null, request.getName());
        return storageInterfaceRepository.save(storageInterface);
    }

    @Override
    public StorageInterface updateStorageInterface(String id, StorageInterfaceRequest request) {
        StorageInterface storageInterface = storageInterfaceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Storage Interface not found with id: " + id));
        Optional<StorageInterface> existingByName = storageInterfaceRepository.findByName(request.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another Storage Interface with name '" + request.getName() + "' already exists.");
        }
        storageInterface.setName(request.getName());
        return storageInterfaceRepository.save(storageInterface);
    }

    @Override
    public void deleteStorageInterface(String id) {
        if (!storageInterfaceRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Storage Interface not found with id: " + id);
        }
        if (componentRepository.existsByStorageInterfaceId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Storage Interface. It is currently in use by one or more components.");
        }
        storageInterfaceRepository.deleteById(id);
    }
}