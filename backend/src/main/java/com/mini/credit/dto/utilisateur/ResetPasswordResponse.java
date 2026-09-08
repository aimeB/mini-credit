package com.mini.credit.dto.utilisateur;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordResponse {
    private String username;
    private String temporaryPassword;
    private Boolean passwordChangeRequired;
    private String advisoryMessage;
}
