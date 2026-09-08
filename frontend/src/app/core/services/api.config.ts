/**
 * Configuration de l'URL API de base.
 * En local : Angular appelle directement Spring Boot sur le port 8080.
 * En production : les appels passent par Nginx via /api.
 */
function getAPIBaseUrl(): string {
  const hostname = window.location.hostname;

  const isLocalhost =
    hostname === 'localhost' ||
    hostname === '127.0.0.1';

  if (isLocalhost) {
    return 'http://localhost:8080/api';
  }

  return '/api';
}

export const API_BASE_URL = getAPIBaseUrl();