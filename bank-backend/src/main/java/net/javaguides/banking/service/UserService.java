package net.javaguides.banking.service;



import net.javaguides.banking.dto.UserDTO;
import net.javaguides.banking.entity.User;

import java.util.List;

public interface UserService {

    void updateUserRole(Long userId, String roleName);

    List<User> getAllUsers();

    UserDTO getUserById(Long id);

    User findByUsername(String username);
}
