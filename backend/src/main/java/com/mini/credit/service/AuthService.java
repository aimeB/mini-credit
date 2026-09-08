package com.mini.credit.service;

import com.mini.credit.dto.auth.AuthResponse;
import com.mini.credit.dto.auth.LoginRequest;
import com.mini.credit.dto.auth.RegisterRequest;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.AuditAction;
import com.mini.credit.enums.security.AuditModule;
import com.mini.credit.enums.security.AuditSeverity;
import com.mini.credit.exception.AuthException;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.security.JwtProvider;
import com.mini.credit.service.audit.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private AuditService auditService;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        String username = loginRequest.getUsername() != null ? loginRequest.getUsername().trim() : "";
        String ipAddress = request != null ? request.getRemoteAddr() : "unknown";
        String userAgent = request != null ? request.getHeader("User-Agent") : "unknown";

        Optional<Utilisateur> userOptional = utilisateurRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            logAuthAttempt(username, false, null, null, false, "USER_NOT_FOUND", ipAddress, userAgent);
            throw new AuthException("USER_NOT_FOUND", "Utilisateur introuvable", HttpStatus.UNAUTHORIZED);
        }

        Utilisateur utilisateur = userOptional.get();
        boolean enabled = Boolean.TRUE.equals(utilisateur.getActif()) && Boolean.TRUE.equals(utilisateur.getIsEnabled());
        boolean locked = !utilisateur.isAccountNonLocked();

        if (!enabled) {
            logAuthAttempt(username, true, false, locked, false, "ACCOUNT_DISABLED", ipAddress, userAgent);
            throw new AuthException("ACCOUNT_DISABLED", "Compte désactivé. Contactez l'administrateur.", HttpStatus.FORBIDDEN);
        }

        if (locked) {
            logAuthAttempt(username, true, true, true, false, "ACCOUNT_LOCKED", ipAddress, userAgent);
            throw new AuthException("ACCOUNT_LOCKED", "Compte verrouillé. Contactez l'administrateur.", HttpStatus.LOCKED);
        }

        boolean passwordMatch = passwordEncoder.matches(loginRequest.getPassword(), utilisateur.getMotDePasseHash());
        if (!passwordMatch) {
            logAuthAttempt(username, true, true, false, false, "BAD_PASSWORD", ipAddress, userAgent);
            throw new AuthException("BAD_PASSWORD", "Mot de passe incorrect", HttpStatus.UNAUTHORIZED);
        }

        if (Boolean.TRUE.equals(utilisateur.getPasswordResetRequired())
                && !Boolean.TRUE.equals(utilisateur.getPasswordChangeRequired())) {
            logAuthAttempt(username, true, true, false, true, "PASSWORD_RESET_PENDING", ipAddress, userAgent);
            throw new AuthException(
                    "PASSWORD_RESET_PENDING",
                    "Activation ou réinitialisation en attente. Utilisez le mot de passe temporaire puis changez-le.",
                    HttpStatus.FORBIDDEN
            );
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(utilisateur, null, utilisateur.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtProvider.generateToken(utilisateur);

        logAuthAttempt(username, true, true, false, true, "LOGIN_SUCCESS", ipAddress, userAgent);
        return AuthResponse.from(token, utilisateur);
    }

    private void logAuthAttempt(
            String username,
            boolean userFound,
            Boolean enabled,
            Boolean locked,
            boolean passwordMatch,
            String reason,
            String ipAddress,
            String userAgent
    ) {
        String logReason = switch (reason) {
            case "BAD_PASSWORD" -> "BAD_CREDENTIALS";
            case "PASSWORD_RESET_PENDING" -> "RESET_PENDING";
            default -> reason;
        };

        String summary = "loginAttempt"
                + " | username=" + username
                + " | userFound=" + userFound
                + " | enabled=" + (enabled != null ? enabled : "n/a")
                + " | locked=" + (locked != null ? locked : "n/a")
            + " | pwdMatch=" + passwordMatch
            + " | reason=" + logReason
                + " | ip=" + ipAddress
                + " | userAgent=" + (userAgent != null ? userAgent : "unknown");

        boolean success = "LOGIN_SUCCESS".equals(reason);
        try {
            auditService.logActionRequiresNew(
                success ? AuditAction.LOGIN_SUCCESS : AuditAction.LOGIN_FAILURE,
                AuditModule.AUTHENTIFICATION,
                "AUTH",
                null,
                success,
                success ? AuditSeverity.INFO : AuditSeverity.WARNING,
                summary,
                null,
                null,
                null,
                    success ? null : logReason,
                null,
                null,
                null,
                null
            );
        } catch (Exception ex) {
            log.warn("AUTH_AUDIT_NON_BLOCKING_ERROR reason={} message={}", logReason, ex.getMessage());
        }

        if (success) {
            log.info("AUTH_LOGIN_SUCCESS {}", summary);
        } else {
            log.warn("AUTH_LOGIN_FAILURE {}", summary);
        }
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        if (utilisateurRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BusinessException("Username already exists");
        }

        if (utilisateurRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        Utilisateur utilisateur = registerRequest.toEntity();
        utilisateur.setMotDePasseHash(passwordEncoder.encode(registerRequest.getPassword()));

        utilisateurRepository.save(utilisateur);

        String token = jwtProvider.generateToken(utilisateur);

        return AuthResponse.from(token, utilisateur);
    }

    public Utilisateur getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    @Transactional(readOnly = true)
    public AuthResponse getCurrentUserResponse() {
        return AuthResponse.forCurrentUser(getCurrentUser());
    }
}
