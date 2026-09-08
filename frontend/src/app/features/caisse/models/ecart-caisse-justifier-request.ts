/**
 * PATCH 9 — Payload pour justifier un écart (POST /{id}/justifier).
 *
 * NOTE TECHNIQUE : La validation 10 caractères minimum est une contrainte technique provisoire.
 * Elle n'est pas définie dans les documents 3N fournis. La règle métier 3N
 * impose uniquement qu'une justification soit obligatoire en cas d'écart.
 */
export interface EcartCaisseJustifierRequest {
  justification: string;
}
