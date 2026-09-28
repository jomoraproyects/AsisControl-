package com.empresa.asiscontrol.auth.bootstrap;

import com.empresa.asiscontrol.shared.config.RecoveryProperties;
import com.empresa.asiscontrol.shared.web.RequestMetadata;
import com.empresa.asiscontrol.usuarios.service.UserService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "asiscontrol.recovery", name = "enabled", havingValue = "true")
public class AdminRecoveryRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminRecoveryRunner.class);
    private final RecoveryProperties properties;
    private final ServerPasswordFileReader passwordFileReader;
    private final UserService userService;

    public AdminRecoveryRunner(RecoveryProperties properties, ServerPasswordFileReader passwordFileReader,
                               UserService userService) {
        this.properties = properties;
        this.passwordFileReader = passwordFileReader;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String password = passwordFileReader.read(properties.passwordFile());
        userService.recoverSuperAdmin(properties.username(), password, properties.resetMfa(),
                new RequestMetadata("SERVER", "recovery", UUID.randomUUID().toString()));
        LOGGER.info("SUPER_ADMIN server recovery completed; disable recovery and remove the password file");
    }
}

