package es.uclm.esibuy.users.service;

import es.uclm.esibuy.users.dto.UpdateUserRequestDTO;
import es.uclm.esibuy.users.model.User;

public interface AdminUserService {

    User obtenerPorId(String id);

    User actualizar(String id, UpdateUserRequestDTO request);
}