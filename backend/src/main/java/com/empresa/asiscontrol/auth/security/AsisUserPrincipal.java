package com.empresa.asiscontrol.auth.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AsisUserPrincipal implements UserDetails, CredentialsContainer, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final UUID publicId;
    private final String username;
    private String password;
    private final boolean active;
    private final boolean mustChangePassword;
    private final long authVersion;
    private final Set<String> roles;
    private final Set<GrantedAuthority> authorities;

    public AsisUserPrincipal(
            Long userId,
            UUID publicId,
            String username,
            String password,
            boolean active,
            boolean mustChangePassword,
            long authVersion,
            Set<String> roles,
            Set<String> permissions) {
        this.userId = userId;
        this.publicId = publicId;
        this.username = username;
        this.password = password;
        this.active = active;
        this.mustChangePassword = mustChangePassword;
        this.authVersion = authVersion;
        this.roles = Collections.unmodifiableSet(new LinkedHashSet<>(roles));
        LinkedHashSet<GrantedAuthority> granted = new LinkedHashSet<>();
        roles.forEach(role -> granted.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> granted.add(new SimpleGrantedAuthority(permission)));
        this.authorities = Collections.unmodifiableSet(granted);
    }

    public Long userId() { return userId; }
    public UUID publicId() { return publicId; }
    public long authVersion() { return authVersion; }
    public boolean mustChangePassword() { return mustChangePassword; }
    public Set<String> roles() { return roles; }
    public boolean requiresMfa() { return roles.contains("SUPER_ADMIN") || roles.contains("RRHH"); }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return active; }

    @Override
    public void eraseCredentials() {
        password = null;
    }
}

