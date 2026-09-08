package com.mini.credit.dto.caisse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReouvrirSessionControleeRequest {
    private String motif;
    private String commentaire;
}
