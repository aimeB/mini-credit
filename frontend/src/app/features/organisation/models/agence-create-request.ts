export interface AgenceCreateRequest {
  codeAgence: string;   // @NotBlank
  nomAgence: string;    // @NotBlank
  ville: string;        // @NotBlank
  commune: string;      // @NotBlank
  quartier?: string;    // optionnel
  adresse?: string;     // optionnel
  reference?: string;   // optionnel
  telephone?: string;
  actif: boolean;       // @NotNull
  description?: string;
  // email : retiré du flux principal
}
