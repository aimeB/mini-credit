package com.mini.credit.entity.referentiel;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_role", nullable = false, unique = true, length = 50)
    private String codeRole;

    @Column(nullable = false, length = 100)
    private String libelle;

    private String description;
}
