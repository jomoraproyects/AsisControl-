package com.empresa.asiscontrol.auth.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class ServerPasswordFileReader {

    public String read(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException("Debe configurar un archivo de contraseña para la operación de servidor");
        }
        Path path = Path.of(configuredPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("El archivo de contraseña configurado no existe");
        }
        try {
            String password = Files.readString(path).strip();
            if (password.isEmpty()) {
                throw new IllegalStateException("El archivo de contraseña está vacío");
            }
            return password;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el archivo de contraseña", exception);
        }
    }
}

