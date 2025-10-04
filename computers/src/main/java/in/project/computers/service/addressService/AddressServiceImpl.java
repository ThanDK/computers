package in.project.computers.service.addressService;

import in.project.computers.DTO.address.AddressRequest;
import in.project.computers.DTO.address.AddressResponse;
import in.project.computers.entity.user.Address;
import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.generalReposiroty.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final UserRepository userRepository;
    private final AddressConverter addressConverter;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(String userId) {
        log.debug("Fetching all addresses for user ID: {}", userId);
        UserEntity user = findUserById(userId);
        return user.getSavedAddresses().stream()
                .map(addressConverter::convertEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(String userId, String addressId) {
        log.debug("Fetching address ID {} for user ID: {}", addressId, userId);
        UserEntity user = findUserById(userId);
        Address address = findAddressInUser(user, addressId);
        return addressConverter.convertEntityToResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse addAddress(String userId, AddressRequest addressRequest) {
        log.info("Attempting to add a new address for user ID: {}", userId);
        UserEntity user = findUserById(userId);

        Address newAddress = addressConverter.convertRequestToEntity(addressRequest);
        newAddress.setId(UUID.randomUUID().toString());

        if (newAddress.isDefault() || user.getSavedAddresses().isEmpty()) {
            unsetDefaultAddresses(user);
            newAddress.setDefault(true);
        }

        user.getSavedAddresses().add(newAddress);
        userRepository.save(user);
        log.info("... successfully added new address with ID {} for user ID: {}", newAddress.getId(), userId);
        return addressConverter.convertEntityToResponse(newAddress);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(String userId, String addressId, AddressRequest addressRequest) {
        log.info("Updating address ID {} for user ID: {}", addressId, userId);
        UserEntity user = findUserById(userId);
        Address addressToUpdate = findAddressInUser(user, addressId);

        boolean wasDefault = addressToUpdate.isDefault();

        if (addressRequest.isDefault()) {
            unsetDefaultAddresses(user);
        }

        addressConverter.updateEntityFromRequest(addressToUpdate, addressRequest);


        if (wasDefault && !addressToUpdate.isDefault()) {

            boolean aDefaultExists = user.getSavedAddresses().stream().anyMatch(Address::isDefault);

            if (!aDefaultExists && !user.getSavedAddresses().isEmpty()) {
                user.getSavedAddresses().getFirst().setDefault(true);
                log.info("... demoted default address. Auto-promoting new default to address ID: {}", user.getSavedAddresses().getFirst().getId());
            }
        }

        userRepository.save(user);
        log.info("... successfully saved updates for address ID: {}", addressId);
        return addressConverter.convertEntityToResponse(addressToUpdate);
    }

    @Override
    @Transactional
    public void deleteAddress(String userId, String addressId) {
        log.info("Attempting to delete address ID {} for user ID: {}", addressId, userId);
        UserEntity user = findUserById(userId);

        Address addressToDelete = findAddressInUser(user, addressId);

        boolean wasDefault = addressToDelete.isDefault();
        user.getSavedAddresses().remove(addressToDelete);

        if (wasDefault && !user.getSavedAddresses().isEmpty()) {
            user.getSavedAddresses().getFirst().setDefault(true);
            log.info("... deleted address was default. Setting new default to address ID: {}", user.getSavedAddresses().getFirst().getId());
        }

        userRepository.save(user);
        log.info("... address with ID: {} deleted successfully from user's profile.", addressId);
    }

    @Override
    @Transactional
    public void setDefaultAddress(String userId, String addressId) {
        log.info("Setting address ID {} as default for user ID: {}", addressId, userId);
        UserEntity user = findUserById(userId);
        findAddressInUser(user, addressId);

        unsetDefaultAddresses(user);
        user.getSavedAddresses().forEach(addr -> {
            if (addr.getId().equals(addressId)) {
                addr.setDefault(true);
            }
        });
        userRepository.save(user);
        log.info("... default address updated successfully.");
    }

    private UserEntity findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId));
    }

    private Address findAddressInUser(UserEntity user, String addressId) {
        return user.getSavedAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found with ID: " + addressId + " for this user."));
    }

    private void unsetDefaultAddresses(UserEntity user) {
        user.getSavedAddresses().forEach(addr -> addr.setDefault(false));
    }
}