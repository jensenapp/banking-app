package net.javaguides.banking.security;


import lombok.RequiredArgsConstructor;
import net.javaguides.banking.entity.Account;
import net.javaguides.banking.repository.AccountRepository;
import net.javaguides.banking.security.services.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("accountSecurityService")
@RequiredArgsConstructor
public class AccountSecurityService {

    private final AccountRepository accountRepository;

    boolean isOwner(Authentication authentication,Long id){
        Account account = accountRepository.findById(id).orElseThrow(() -> new RuntimeException("account not found"));
        UserDetailsImpl userDetails=(UserDetailsImpl) authentication.getPrincipal();
        if (account.getUser()==null) {
            return false;
        }
        return userDetails.getId().equals(account.getUser().getUserId());
    }

}
