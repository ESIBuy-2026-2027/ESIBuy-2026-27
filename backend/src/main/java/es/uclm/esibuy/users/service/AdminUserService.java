package es.uclm.esibuy.users.service;

import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.dto.UpdateUserRequestDTO;
import es.uclm.esibuy.users.dto.UserResponseDTO;
import es.uclm.esibuy.users.model.User;

public interface AdminUserService {

    User obtenerPorId(String id);

    User actualizar(String id, UpdateUserRequestDTO request);

    void eliminar(String id, String authenticatedUserId);

    UserResponseDTO createClientAdmin(ClientRegistrationRequest request);
    
    UserResponseDTO createSellerAdmin(SellerRegistrationRequest request);
}