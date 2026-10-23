package es.uclm.esibuy.users.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.uclm.esibuy.common.exception.AddressLimitExceededException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.ShippingAddressRequest;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.ShippingAddress;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import es.uclm.esibuy.common.exception.AddressNotFoundException;

@ExtendWith(MockitoExtension.class)
class ShippingAddressServiceTest {

    private static final String USER_ID = "user-1";

    @Mock
    private UserRepository userRepository;

    private ShippingAddressService service;
    private User client;

    @BeforeEach
    void setUp() {
        service = new ShippingAddressService(userRepository);
        client = new User("ana@example.com", "Ana", "García", UserRole.CLIENT);
        client.setClientProfile(new ClientProfile());
    }

    private ShippingAddressRequest request(boolean isDefault) {
        return new ShippingAddressRequest("  Ana García ", " Calle Mayor, 12 ", "  ", "13071",
                "Ciudad Real", "Ciudad Real", "", isDefault);
    }

    private ShippingAddress existing(String id, boolean isDefault) {
        return new ShippingAddress(id, "Ana", "Calle Sol, 1", null, "28001", "Madrid", "Madrid", null, isDefault);
    }

    @Test
    void shouldAddTheFirstAddressAsDefaultEvenIfNotRequested() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        ShippingAddress created = service.add(USER_ID, request(false));

        assertNotNull(created.getId());
        assertTrue(created.isDefaultAddress());
        verify(userRepository).save(client);
    }

    @Test
    void shouldTrimDataAndConvertBlankOptionalFieldsToNull() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        ShippingAddress created = service.add(USER_ID, request(false));

        assertEquals("Ana García", created.getRecipientName());
        assertEquals("Calle Mayor, 12", created.getStreet());
        assertNull(created.getAdditionalInfo());
        assertNull(created.getPhone());
    }

    @Test
    void shouldKeepTheCurrentDefaultWhenTheNewAddressIsNotDefault() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        client.getClientProfile().setAddresses(new ArrayList<>(List.of(existing("a1", true))));

        ShippingAddress created = service.add(USER_ID, request(false));

        assertFalse(created.isDefaultAddress());
        assertTrue(client.getClientProfile().getAddresses().get(0).isDefaultAddress());
        assertEquals(2, client.getClientProfile().getAddresses().size());
    }

    @Test
    void shouldReplaceTheCurrentDefaultWhenTheNewAddressIsDefault() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        client.getClientProfile().setAddresses(new ArrayList<>(List.of(existing("a1", true))));

        ShippingAddress created = service.add(USER_ID, request(true));

        assertTrue(created.isDefaultAddress());
        assertFalse(client.getClientProfile().getAddresses().get(0).isDefaultAddress());
    }

    @Test
    void shouldCreateTheClientProfileWhenItDoesNotExist() {
        client.setClientProfile(null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        service.add(USER_ID, request(false));

        assertNotNull(client.getClientProfile());
        assertEquals(1, client.getClientProfile().getAddresses().size());
    }

    @Test
    void shouldRejectTheAddressWhenTheLimitIsReached() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        List<ShippingAddress> full = new ArrayList<>();
        for (int i = 0; i < ShippingAddressService.MAX_ADDRESSES; i++) {
            full.add(existing("a" + i, i == 0));
        }
        client.getClientProfile().setAddresses(full);

        assertThrows(AddressLimitExceededException.class, () -> service.add(USER_ID, request(false)));
        verify(userRepository, never()).save(client);
    }

    @Test
    void shouldFailWhenTheUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> service.add("missing", request(false)));

        assertThrows(
                UserNotFoundException.class,
                () -> service.list("missing"));

        verify(userRepository, org.mockito.Mockito.times(2))
                .findById("missing");
    }

    @Test
    void shouldListAnEmptyCollectionWhenThereIsNoClientProfile() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        client.setClientProfile(null);

        assertTrue(service.list(USER_ID).isEmpty());
    }

    @Test
    void shouldListTheStoredAddresses() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        client.getClientProfile().setAddresses(new ArrayList<>(List.of(existing("a1", true))));

        assertEquals(1, service.list(USER_ID).size());
    }

    @Test
    void shouldUpdateAddressAndPreserveItsId() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        client.getClientProfile().setAddresses(
                new ArrayList<>(List.of(existing("a1", true))));

        var updated = service.update(USER_ID, "a1", request(false));

        assertEquals("a1", updated.id());
        assertEquals("Ana García", updated.recipientName());
        assertEquals("Calle Mayor, 12", updated.street());
        assertTrue(updated.defaultAddress());
        verify(userRepository).save(client);
    }

    @Test
    void shouldThrowWhenUpdatingAnAddressThatDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));

        assertThrows(
                AddressNotFoundException.class,
                () -> service.update(USER_ID, "missing", request(false)));

        verify(userRepository, never()).save(client);
    }

    @Test
    void shouldDeleteAddressAndAssignDefaultToAnotherAddress() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        client.getClientProfile().setAddresses(new ArrayList<>(List.of(
                existing("a1", true),
                existing("a2", false))));

        service.delete(USER_ID, "a1");

        assertEquals(1, client.getClientProfile().getAddresses().size());
        assertEquals("a2", client.getClientProfile().getAddresses().get(0).getId());
        assertTrue(client.getClientProfile().getAddresses().get(0).isDefaultAddress());
        verify(userRepository).save(client);
    }

    @Test
    void shouldDeleteNonDefaultAddressWithoutChangingTheDefault() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        client.getClientProfile().setAddresses(new ArrayList<>(List.of(
                existing("a1", true),
                existing("a2", false))));

        service.delete(USER_ID, "a2");

        assertEquals(1, client.getClientProfile().getAddresses().size());
        assertTrue(client.getClientProfile().getAddresses().get(0).isDefaultAddress());
    }

    @Test
    void shouldThrowWhenDeletingAnAddressThatDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(client));
        client.getClientProfile().setAddresses(
                new ArrayList<>(List.of(existing("a1", true))));

        assertThrows(
                AddressNotFoundException.class,
                () -> service.delete(USER_ID, "missing"));

        verify(userRepository, never()).save(client);
    }

    @Test
    void shouldThrowWhenUpdatingAnAddressForAMissingUser() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> service.update("missing", "a1", request(false)));
    }
}
