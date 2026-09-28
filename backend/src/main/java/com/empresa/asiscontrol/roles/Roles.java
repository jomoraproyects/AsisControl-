package com.empresa.asiscontrol.roles;

import java.util.Set;

public final class Roles {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String RRHH = "RRHH";
    public static final String SUPERVISOR = "SUPERVISOR";
    public static final String CONDUCTOR = "CONDUCTOR";
    public static final Set<String> PROTEGIDOS = Set.of(SUPER_ADMIN, RRHH, SUPERVISOR, CONDUCTOR);

    private Roles() {
    }
}
