import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { API_BASE_URL } from '../../../core/services/api.config';
import { 
  DemandeRetraitEpargneResponse,
  CreateDemandeRetraitEpargneRequest,
  RejectDemandeRetraitEpargneRequest
} from '../models/demande-retrait-epargne';

/**
 * Service HTTP pour la gestion des demandes de retrait épargne (PHASE 6B.3)
 * 
 * Endpoints utilisés:
 * - POST /api/demandes-retrait-epargne - Créer demande
 * - GET /api/demandes-retrait-epargne/{id} - Détail demande
 * - GET /api/demandes-retrait-epargne/compte/{compteId} - Liste par compte
 * - GET /api/demandes-retrait-epargne/validation/en-attente - Demandes à valider
 * - POST /api/demandes-retrait-epargne/{id}/valider - Valider demande
 * - POST /api/demandes-retrait-epargne/{id}/rejeter - Rejeter demande
 * - POST /api/demandes-retrait-epargne/{id}/decaisser - Payer retrait
 * - POST /api/demandes-retrait-epargne/{id}/annuler - Annuler demande
 */
@Injectable({ providedIn: 'root' })
export class DemandeRetraitEpargneService {
  
  private http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/demandes-retrait-epargne`;

  /**
   * Crée une nouvelle demande de retrait épargne
   * Utilisateur: Membre ou Agent Terrain
   */
  creerDemande(
    compteEpargneId: number, 
    montantDemande: number, 
    fraisRetrait: number = 0,
    observation?: string
  ): Observable<DemandeRetraitEpargneResponse> {
    const req: CreateDemandeRetraitEpargneRequest = {
      compteEpargneId,
      montantDemande,
      fraisRetrait,
      observation
    };

    let params = new HttpParams()
      .set('compteEpargneId', String(req.compteEpargneId))
      .set('montant', String(req.montantDemande))
      .set('fraisRetrait', String(req.fraisRetrait ?? 0));

    if (req.observation) {
      params = params.set('observation', req.observation);
    }

    return this.http.post<DemandeRetraitEpargneResponse>(
      `${this.baseUrl}`,
      null,
      { params }
    ).pipe(map(response => this.normalizeResponse(response)));
  }

  /**
   * Récupère une demande par ID
   * Utilisateur: Tous (avec permissions appropriées)
   */
  getById(id: number): Observable<DemandeRetraitEpargneResponse> {
    return this.http.get<DemandeRetraitEpargneResponse>(
      `${this.baseUrl}/${id}`
    ).pipe(map(response => this.normalizeResponse(response)));
  }

  /**
   * Récupère les demandes d'un compte épargne
   * Utilisateur: Membre propriétaire du compte ou Controleur/Caissier
   */
  getByCompteEpargne(compteEpargneId: number): Observable<DemandeRetraitEpargneResponse[]> {
    return this.http.get<DemandeRetraitEpargneResponse[]>(
      `${this.baseUrl}/compte/${compteEpargneId}`
    ).pipe(map(responses => responses.map(response => this.normalizeResponse(response))));
  }

  /**
   * Récupère les demandes en attente de validation du CONTROLEUR
   * Utilisateur: Controleur uniquement
   */
  getEnAttenteValidation(): Observable<DemandeRetraitEpargneResponse[]> {
    return this.http.get<DemandeRetraitEpargneResponse[]>(
      `${this.baseUrl}/validation/en-attente`
    ).pipe(map(responses => responses.map(response => this.normalizeResponse(response))));
  }

  /**
   * Valide une demande de retrait (approuve ou rejette)
   * Utilisateur: Controleur uniquement
   * Vérifications backend: soldeDisponible >= montantDemande
   */
  validerDemande(id: number): Observable<DemandeRetraitEpargneResponse> {
    return this.http.post<DemandeRetraitEpargneResponse>(
      `${this.baseUrl}/${id}/valider`,
      {}
    ).pipe(map(response => this.normalizeResponse(response)));
  }

  /**
   * Rejette une demande avec motif
   * Utilisateur: Controleur uniquement
   */
  rejeterDemande(id: number, motif: string): Observable<DemandeRetraitEpargneResponse> {
    const req: RejectDemandeRetraitEpargneRequest = { motif };
    const params = new HttpParams().set('motif', req.motif);
    return this.http.post<DemandeRetraitEpargneResponse>(
      `${this.baseUrl}/${id}/rejeter`,
      null,
      { params }
    ).pipe(map(response => this.normalizeResponse(response)));
  }

  /**
   * Décaisse (paie) un retrait validé
   * Utilisateur: Caissier uniquement
   * Effet: 
   * - Crée OperationEpargne type RETRAIT (historisation compte épargne)
   * - Crée OperationCaisse type SORTIE (impacte caisse)
   * - Réduit soldeDisponible du compte
   * - Passe statut à DECAISSEE
   */
  decaisserRetrait(id: number): Observable<DemandeRetraitEpargneResponse> {
    return this.http.post<DemandeRetraitEpargneResponse>(
      `${this.baseUrl}/${id}/decaisser`,
      {}
    ).pipe(map(response => this.normalizeResponse(response)));
  }

  /**
   * Annule une demande de retrait
   * Utilisateur: Membre ou Admin
   * Contrainte: Impossible d'annuler si statut == DECAISSEE
   */
  annulerDemande(id: number): Observable<DemandeRetraitEpargneResponse> {
    return this.http.post<DemandeRetraitEpargneResponse>(
      `${this.baseUrl}/${id}/annuler`,
      {}
    ).pipe(map(response => this.normalizeResponse(response)));
  }

  /**
   * Récupère la liste de toutes les demandes (optional, selon backend)
   */
  getAll(statut?: string): Observable<DemandeRetraitEpargneResponse[]> {
    let params = new HttpParams();
    if (statut) {
      params = params.set('statut', statut);
    }

    return this.http.get<DemandeRetraitEpargneResponse[]>(
      `${this.baseUrl}`,
      { params }
    ).pipe(map(responses => responses.map(response => this.normalizeResponse(response))));
  }

  private normalizeResponse(raw: any): DemandeRetraitEpargneResponse {
    const montantDemande = raw.montantDemande ?? raw.montant_demande;
    const fraisRetrait = raw.fraisRetrait ?? raw.frais_retrait ?? 0;
    const montantTotalDebite = raw.montantTotalDebite ?? raw.montant_total_debite ?? (Number(montantDemande || 0) + Number(fraisRetrait || 0));
    return {
      ...raw,
      id: raw.id,
      compteEpargneId: raw.compteEpargneId ?? raw.compte_epargne_id,
      referenceRetrait: raw.referenceRetrait ?? raw.reference_retrait ?? this.buildFallbackReference(raw),
      numeroCompte: raw.numeroCompte ?? raw.numero_compte,
      compteEpargneNumero: raw.compteEpargneNumero ?? raw.compte_epargne_numero ?? raw.numeroCompte ?? raw.numero_compte,
      membreId: raw.membreId ?? raw.membre_id,
      membreNom: raw.membreNom ?? raw.membre_nom,
      montantDemande,
      fraisRetrait,
      tauxCommissionRetrait: raw.tauxCommissionRetrait ?? raw.taux_commission_retrait ?? 0,
      montantTotalDebite,
      montantRemisAuMembre: raw.montantRemisAuMembre ?? raw.montant_remis_au_membre ?? montantDemande,
      soldeDisponible: raw.soldeDisponible ?? raw.solde_disponible,
      soldeBloque: raw.soldeBloque ?? raw.solde_bloque,
      operationCaisseSortieId: raw.operationCaisseSortieId ?? raw.operation_caisse_sortie_id,
      operationCaisseFraisId: raw.operationCaisseFraisId ?? raw.operation_caisse_frais_id,
      statut: raw.statut,
      dateDemande: raw.dateDemande ?? raw.date_demande,
      createdAt: raw.createdAt ?? raw.created_at,
      motifRejet: raw.motifRejet ?? raw.motif_rejet,
      valideParId: raw.valideParId ?? raw.valide_par_id,
      valideParNom: raw.valideParNom ?? raw.valide_par_nom,
      dateValidation: raw.dateValidation ?? raw.date_validation,
      observation: raw.observation
    };
  }

  private buildFallbackReference(raw: any): string | undefined {
    if (!raw?.id) {
      return undefined;
    }

    const rawDate = raw.dateDemande ?? raw.date_demande ?? raw.createdAt ?? raw.created_at;
    const year = rawDate ? new Date(rawDate).getFullYear() : new Date().getFullYear();
    return `RET-${year}-${String(raw.id).padStart(4, '0')}`;
  }
}
