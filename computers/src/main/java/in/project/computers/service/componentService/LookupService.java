package in.project.computers.service.componentService;

import in.project.computers.dto.lookup.FormFactorRequest;
import in.project.computers.dto.lookup.RamTypeRequest;
import in.project.computers.dto.lookup.SocketRequest;
import in.project.computers.dto.lookup.StorageInterfaceRequest;
import in.project.computers.entity.lookup.FormFactor;
import in.project.computers.entity.lookup.RamType;
import in.project.computers.entity.lookup.Socket;
import in.project.computers.entity.lookup.StorageInterface;

import java.util.List;
import java.util.Map;

public interface LookupService {

    Map<String, Object> getAllLookups();

    List<Socket> getAllSockets();
    List<RamType> getAllRamTypes();
    List<FormFactor> getAllFormFactors();
    List<StorageInterface> getAllStorageInterfaces();

    Socket createSocket(SocketRequest request);
    Socket updateSocket(String id, SocketRequest request);
    void deleteSocket(String id);

    RamType createRamType(RamTypeRequest request);
    RamType updateRamType(String id, RamTypeRequest request);
    void deleteRamType(String id);

    FormFactor createFormFactor(FormFactorRequest request);
    FormFactor updateFormFactor(String id, FormFactorRequest request);
    void deleteFormFactor(String id);

    StorageInterface createStorageInterface(StorageInterfaceRequest request);
    StorageInterface updateStorageInterface(String id, StorageInterfaceRequest request);
    void deleteStorageInterface(String id);
}