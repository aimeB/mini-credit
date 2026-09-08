export interface ParametreMetier {
  id?: number;
  cle: string;
  libelle: string;
  typeParametre: 'DECIMAL' | 'ENTIER' | 'TEXTE';
  categorie: 'TAUX_INTERET' | 'FRAIS' | 'SEUIL' | 'LIMITE' | 'GENERAL';
  valeurDecimale?: number;
  valeurEntiere?: number;
  valeurTexte?: string;
  unite?: string;
  description?: string;
  valeurParDefaut?: string;
  actif?: boolean;
  modifiable?: boolean;
  modifiePar?: string;

  /**
   * Retourne la valeur du paramètre selon son type.
   */
  getValeur?(): any;
}
