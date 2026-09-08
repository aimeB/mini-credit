/**
 * PATCH 9 — Payload pour résoudre un écart (POST /{id}/resoudre).
 * La raison documente la solution appliquée (traçabilité 3N).
 */
export interface EcartCaisseResoudreRequest {
  raison: string;
}
