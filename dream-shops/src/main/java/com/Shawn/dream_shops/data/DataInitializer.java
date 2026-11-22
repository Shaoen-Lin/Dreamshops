package com.Shawn.dream_shops.data;

import com.Shawn.dream_shops.model.Role;
import com.Shawn.dream_shops.model.User;
import com.Shawn.dream_shops.repository.RoleRepository;
import com.Shawn.dream_shops.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${SHOP_BOOTSTRAP_ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${SHOP_BOOTSTRAP_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (adminEmail.isBlank() && adminPassword.isBlank()) {
            return;
        }
        int passwordLength = adminPassword.getBytes(StandardCharsets.UTF_8).length;
        if (adminEmail.isBlank() || adminPassword.isBlank() || passwordLength < 12 || passwordLength > 72) {
            throw new IllegalStateException("Set a bootstrap administrator email and a password of 12 to 72 UTF-8 bytes");
        }

        User existing = userRepo.findByEmail(adminEmail.trim());
        if (existing != null) {
            if (existing.getRoles().stream().noneMatch(role -> "ROLE_ADMIN".equals(role.getName()))) {
                throw new IllegalStateException("The bootstrap administrator email belongs to an existing non-admin account");
            }
            return;
        }

        Role role = roleRepo.findByName("ROLE_ADMIN").orElseGet(() -> roleRepo.save(new Role("ROLE_ADMIN")));
        User admin = new User();
        admin.setEmail(adminEmail.trim());
        admin.setFirstName("DreamShops");
        admin.setLastName("Admin");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRoles(Set.of(role));
        userRepo.save(admin);
    }
}
