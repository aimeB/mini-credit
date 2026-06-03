package com.mini.credit.entity.referentiel;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.membre.Membre;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

@Entity(name = "ReferentielUtilisateur")
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur extends BaseEntity implements UserDetails {


    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "mot_de_passe_hash", nullable = false, columnDefinition = "TEXT")
    private String motDePasseHash;

    @Column(name = "nom_complet", nullable = false, length = 150)
    private String nomComplet;

    private String telephone;
    private String email;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_role"))
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id")
    private Membre membre;

    /**
     * Employé associé (pour les utilisateurs avec poste métier)
     * OneToOne: un utilisateur = un employé
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", foreignKey = @ForeignKey(name = "fk_user_employe"))
    private Employe employe;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    @Column(name = "password_reset_required", nullable = false)
    @Builder.Default
    private Boolean passwordResetRequired = false;

    @Column(name = "password_reset_token", length = 255)
    private String passwordResetToken;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null) {
            // Ajouter le rôle
            String roleAuth = "ROLE_" + role.getCode().name();
            authorities.add(new SimpleGrantedAuthority(roleAuth));
            System.out.println("🔑 AUTHORITY RÔLE: " + roleAuth);
            
            // Ajouter les permissions du rôle
            if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
                System.out.println("📋 PERMISSIONS du rôle: " + role.getPermissions().size());
                for (RolePermission rolePermission : role.getPermissions()) {
                    if (rolePermission.getPermission() != null) {
                        String permAuth = rolePermission.getPermission().getCode().name();
                        authorities.add(new SimpleGrantedAuthority(permAuth));
                        System.out.println("  🔓 PERMISSION: " + permAuth);
                    }
                }
            } else {
                System.out.println("⚠️  AUCUNE PERMISSION pour ce rôle!");
            }
        } else {
            System.out.println("❌ RÔLE EST NULL!");
        }
        System.out.println("📊 TOTAL AUTHORITIES: " + authorities);
        return authorities;
    }

    @Override
    public String getPassword() {
        return motDePasseHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled && !isLocked;
    }
}
