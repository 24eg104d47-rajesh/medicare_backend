package com.medicareai.service;

import com.medicareai.dto.user.DeliveryAddressRequest;
import com.medicareai.dto.user.DeliveryAddressResponse;
import com.medicareai.entity.DeliveryAddress;
import com.medicareai.entity.User;
import com.medicareai.exception.BadRequestException;
import com.medicareai.exception.ResourceNotFoundException;
import com.medicareai.repository.CustomerOrderRepository;
import com.medicareai.repository.DeliveryAddressRepository;
import com.medicareai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final DeliveryAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CustomerOrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<DeliveryAddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeliveryAddressResponse createAddress(Long userId, DeliveryAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        List<DeliveryAddress> existing = addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId);
        boolean shouldBeDefault = request.isDefaultAddress() || existing.isEmpty();

        if (shouldBeDefault) {
            existing.forEach(a -> a.setDefaultAddress(false));
            addressRepository.saveAll(existing);
        }

        String safePhone = request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : "Not Provided";
        String safeState = request.getState() != null && !request.getState().isBlank() ? request.getState().trim() : "Karnataka";

        DeliveryAddress address = DeliveryAddress.builder()
                .user(user)
                .recipientName(request.getRecipientName().trim())
                .phone(safePhone)
                .addressLine1(request.getAddressLine1().trim())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity().trim())
                .state(safeState)
                .postalCode(request.getPostalCode().trim())
                .country(request.getCountry() != null ? request.getCountry() : "India")
                .defaultAddress(shouldBeDefault)
                .build();

        DeliveryAddress saved = addressRepository.save(address);
        return mapToResponse(saved);
    }

    @Transactional
    public DeliveryAddressResponse updateAddress(Long userId, Long addressId, DeliveryAddressRequest request) {
        DeliveryAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found or unauthorized: " + addressId));

        if (request.isDefaultAddress() && !address.isDefaultAddress()) {
            List<DeliveryAddress> others = addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId);
            others.forEach(a -> a.setDefaultAddress(false));
            addressRepository.saveAll(others);
        }

        if (request.getRecipientName() != null) address.setRecipientName(request.getRecipientName().trim());
        if (request.getPhone() != null && !request.getPhone().isBlank()) address.setPhone(request.getPhone().trim());
        if (request.getAddressLine1() != null) address.setAddressLine1(request.getAddressLine1().trim());
        address.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) address.setCity(request.getCity().trim());
        if (request.getState() != null && !request.getState().isBlank()) address.setState(request.getState().trim());
        if (request.getPostalCode() != null) address.setPostalCode(request.getPostalCode().trim());
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry().trim());
        }
        address.setDefaultAddress(request.isDefaultAddress());

        DeliveryAddress updated = addressRepository.save(address);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        DeliveryAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found or unauthorized: " + addressId));

        if (orderRepository.existsByDeliveryAddressId(addressId)) {
            throw new BadRequestException("Cannot delete address because it is associated with existing orders. You can set another address as default instead.");
        }

        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);

        if (wasDefault) {
            List<DeliveryAddress> remaining = addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                DeliveryAddress nextDefault = remaining.get(0);
                nextDefault.setDefaultAddress(true);
                addressRepository.save(nextDefault);
            }
        }
    }

    @Transactional
    public DeliveryAddressResponse setDefaultAddress(Long userId, Long addressId) {
        List<DeliveryAddress> all = addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId);
        DeliveryAddress target = null;

        for (DeliveryAddress a : all) {
            if (a.getId().equals(addressId)) {
                a.setDefaultAddress(true);
                target = a;
            } else {
                a.setDefaultAddress(false);
            }
        }

        if (target == null) {
            throw new ResourceNotFoundException("Address not found: " + addressId);
        }

        addressRepository.saveAll(all);
        return mapToResponse(target);
    }

    private DeliveryAddressResponse mapToResponse(DeliveryAddress a) {
        return DeliveryAddressResponse.builder()
                .id(a.getId())
                .recipientName(a.getRecipientName())
                .phone(a.getPhone())
                .addressLine1(a.getAddressLine1())
                .addressLine2(a.getAddressLine2())
                .city(a.getCity())
                .state(a.getState())
                .postalCode(a.getPostalCode())
                .country(a.getCountry())
                .defaultAddress(a.isDefaultAddress())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
