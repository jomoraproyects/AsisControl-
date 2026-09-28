package com.empresa.asiscontrol.auth.controller;

import com.empresa.asiscontrol.auth.dto.AuthenticationResponse;
import com.empresa.asiscontrol.auth.dto.CsrfResponse;
import com.empresa.asiscontrol.auth.dto.LoginRequest;
import com.empresa.asiscontrol.auth.dto.MfaCodeRequest;
import com.empresa.asiscontrol.auth.dto.MfaConfirmationResponse;
import com.empresa.asiscontrol.auth.dto.MfaEnrollmentResponse;
import com.empresa.asiscontrol.auth.dto.MfaVerificationRequest;
import com.empresa.asiscontrol.auth.dto.SessionResponse;
import com.empresa.asiscontrol.auth.security.AsisUserPrincipal;
import com.empresa.asiscontrol.auth.service.AuthenticationService;
import com.empresa.asiscontrol.auth.service.MfaService;
import com.empresa.asiscontrol.shared.web.RequestMetadataProvider;
import com.empresa.asiscontrol.usuarios.dto.ChangePasswordRequest;
import com.empresa.asiscontrol.usuarios.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final MfaService mfaService;
    private final RequestMetadataProvider metadataProvider;
    private final UserService userService;

    public AuthController(AuthenticationService authenticationService, MfaService mfaService,
                          RequestMetadataProvider metadataProvider, UserService userService) {
        this.authenticationService = authenticationService;
        this.mfaService = mfaService;
        this.metadataProvider = metadataProvider;
        this.userService = userService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken()));
    }

    @PostMapping("/login")
    public AuthenticationResponse login(@Valid @RequestBody LoginRequest input,
                                        HttpServletRequest request, HttpServletResponse response) {
        return authenticationService.login(input, request, response, metadataProvider.from(request));
    }

    @PostMapping("/mfa/enroll")
    public MfaEnrollmentResponse enroll(HttpServletRequest request) {
        return mfaService.enroll(request);
    }

    @PostMapping("/mfa/confirm")
    public MfaConfirmationResponse confirm(@Valid @RequestBody MfaCodeRequest input,
                                           HttpServletRequest request, HttpServletResponse response) {
        return mfaService.confirm(input.code(), request, response, metadataProvider.from(request));
    }

    @PostMapping("/mfa/verify")
    public AuthenticationResponse verify(@Valid @RequestBody MfaVerificationRequest input,
                                         HttpServletRequest request, HttpServletResponse response) {
        return mfaService.verify(input, request, response, metadataProvider.from(request));
    }

    @GetMapping("/session")
    public SessionResponse session(@AuthenticationPrincipal AsisUserPrincipal principal) {
        return SessionResponse.from(principal);
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest input,
                                               @AuthenticationPrincipal AsisUserPrincipal principal,
                                               HttpServletRequest request) {
        userService.changeOwnPassword(principal, input, metadataProvider.from(request));
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AsisUserPrincipal principal,
                                       HttpServletRequest request) {
        authenticationService.logout(principal, request, metadataProvider.from(request));
        return ResponseEntity.noContent().build();
    }
}
