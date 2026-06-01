package com.mini.credit.service;

import com.mini.credit.dto.auth.AuthResponse;
import com.mini.credit.dto.auth.LoginRequest;
import com.mini.credit.dto.auth.RegisterRequest;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.security.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtProvider.generateToken((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal());

            Utilisateur utilisateur = utilisateurRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new BusinessException("User not found"));

            // Vérifier que le compte est activé
            if (!utilisateur.getActif()) {
                log.warn("Login refused: account disabled for user {}", utilisateur.getUsername());
                throw new BusinessException("Ce compte a été désactivé. Contactez l'administrateur.");
            }

            if (utilisateur.getPasswordResetRequired()) {
                log.warn("Login refused: account not activated for user {}", utilisateur.getUsername());
                throw new BusinessException("Compte non activé. Veuillez cliquer sur le lien d'activation reçu par email.");
            }

            return AuthResponse.from(token, utilisateur);
        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage());
            throw new BusinessException("Invalid username or password");
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

        String token = jwtProvider.generateToken(org.springframework.security.core.userdetails.User.builder()
                .username(utilisateur.getUsername())
                .password(utilisateur.getPassword())
                .authorities(utilisateur.getAuthorities())
                .build());

        return AuthResponse.from(token, utilisateur);
    }

    public Utilisateur getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));
    }
}
