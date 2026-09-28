package com.empresa.asiscontrol.auth.bootstrap;

import com.empresa.asiscontrol.shared.config.BootstrapProperties;
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
@ConditionalOnProperty(prefix = "asiscontrol.bootstrap", name = "enabled", havingValue = "true")
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private final BootstrapProperties properties;
    private final ServerPasswordFileReader passwordFileReader;
    private final UserService userService;

    public AdminBootstrapRunner(BootstrapProperties properties, ServerPasswordFileReader passwordFileReader,
                                UserService userService) {
        this.properties = properties;
        this.passwordFileReader = passwordFileReader;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String password = passwordFileReader.read(properties.passwordFile());
        userService.bootstrap(properties.username(), properties.email(), password,
                new RequestMetadata("SERVER", "bootstrap", UUID.randomUUID().toString()));
        LOGGER.info("Bootstrap SUPER_ADMIN completed; disable bootstrap and remove the password file");
    }
}

