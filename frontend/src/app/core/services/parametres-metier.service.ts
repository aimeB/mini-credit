import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ParametreMetier } from '../../shared/models/parametres-metier.model';
import { API_BASE_URL } from './api.config';

/**
 * Service pour accéder aux paramètres métier du système.
 * PHASE 1: Service de cache simple pour les paramètres.
 * 
 * Cache strategy:
 * - Charge une fois au démarrage
 * - Stocke en mémoire
 * - Permet de recharger via reloadCache()
 */
@Injectable({
  providedIn: 'root'
})
export class ParametresMetierService {

  private readonly API_URL = `${API_BASE_URL}/admin/parametres-metier`;
  
  // Cache des paramètres chargés
  private cache$ = new BehaviorSubject<Map<string, ParametreMetier>>(new Map());
  
  // Flag pour savoir si les paramètres sont chargés
  private isLoaded = false;

  constructor(private http: HttpClient) {
    // Chargement à la demande uniquement: l'écran analyse-risque fonctionne
    // avec les valeurs par défaut si le cache n'est pas préchargé.
  }

  /**
   * Charge les paramètres depuis le serveur et met en cache.
   */
  private chargerParametres(): void {
    if (this.isLoaded) {
      return;
    }

    this.http.get<ParametreMetier[]>(this.API_URL)
      .pipe(
        tap(parametres => {
          const map = new Map<string, ParametreMetier>();
          parametres.forEach(p => map.set(p.cle, p));
          this.cache$.next(map);
          this.isLoaded = true;
        }),
        catchError(error => {
          console.error('Erreur lors du chargement des paramètres', error);
          this.isLoaded = false;
          return of([]);
        })
      )
      .subscribe();
  }

  /**
   * Récupère un paramètre decimal par sa clé.
   */
  getDecimal(cle: string): number {
    const param = this.cache$.getValue().get(cle);
    return param?.valeurDecimale ?? 0;
  }

  /**
   * Récupère un paramètre entier par sa clé.
   */
  getEntier(cle: string): number {
    const param = this.cache$.getValue().get(cle);
    return param?.valeurEntiere ?? 0;
  }

  /**
   * Récupère un paramètre texte par sa clé.
   */
  getTexte(cle: string): string {
    const param = this.cache$.getValue().get(cle);
    return param?.valeurTexte ?? '';
  }

  /**
   * Récupère un paramètre par sa clé sous forme d'Observable.
   */
  getByKey(cle: string): Observable<ParametreMetier | undefined> {
    const param = this.cache$.getValue().get(cle);
    return of(param);
  }

  /**
   * Récupère tous les paramètres actifs.
   */
  getAllActifs(): Observable<ParametreMetier[]> {
    const parametres = Array.from(this.cache$.getValue().values())
      .filter(p => p.actif === true);
    return of(parametres);
  }

  /**
   * Récupère tous les paramètres d'une catégorie.
   */
  getByCategorie(categorie: string): Observable<ParametreMetier[]> {
    const parametres = Array.from(this.cache$.getValue().values())
      .filter(p => p.categorie === categorie);
    return of(parametres);
  }

  /**
   * Recharge le cache depuis le serveur.
   */
  reloadCache(): Observable<string> {
    return this.http.post<string>(`${this.API_URL}/reload-cache`, {})
      .pipe(
        tap(() => {
          this.isLoaded = false;
          this.chargerParametres();
        }),
        catchError(error => {
          console.error('Erreur lors du rechargement du cache', error);
          throw error;
        })
      );
  }

  /**
   * Vérifie l'existence d'une clé de paramètre.
   */
  existsKey(cle: string): boolean {
    return this.cache$.getValue().has(cle);
  }
}
