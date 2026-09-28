package com.empresa.asiscontrol.integration;

import com.empresa.asiscontrol.roles.entity.Rol;
import com.empresa.asiscontrol.roles.entity.UsuarioRol;
import com.empresa.asiscontrol.roles.repository.RolRepository;
import com.empresa.asiscontrol.roles.repository.UsuarioRolRepository;
import com.empresa.asiscontrol.usuarios.entity.Usuario;
import com.empresa.asiscontrol.usuarios.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class MySqlIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
            .withDatabaseName("asiscontrol_test")
            .withUsername("asiscontrol_test")
            .withPassword("integration-test-only");

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected RolRepository rolRepository;
    @Autowired protected UsuarioRolRepository usuarioRolRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected TransactionTemplate transactions;
    @Autowired protected Clock clock;

    protected Usuario createReadyUser(String prefix, String rawPassword, String roleCode) {
        String username = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        return transactions.execute(status -> {
            Usuario user = Usuario.crear(username, username.toLowerCase(), null, null,
                    passwordEncoder.encode(rawPassword), clock.instant());
            user.cambiarPassword(passwordEncoder.encode(rawPassword), false, clock.instant());
            usuarioRepository.save(user);
            Rol role = rolRepository.findByCodigo(roleCode).orElseThrow();
            usuarioRolRepository.save(UsuarioRol.asignar(user, role, null, clock.instant()));
            return user;
        });
    }

    protected Usuario createInactiveUser(String prefix, String rawPassword, String roleCode) {
        Usuario user = createReadyUser(prefix, rawPassword, roleCode);
        transactions.executeWithoutResult(status -> {
            Usuario managed = usuarioRepository.findById(user.getId()).orElseThrow();
            managed.desactivar(clock.instant());
        });
        return user;
    }
}

