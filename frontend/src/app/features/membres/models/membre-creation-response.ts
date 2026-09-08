import { MembreResponse } from './membre-response';

export interface ActivationCodeDTO {
  code: string;
  memberName?: string;
  phoneNumber?: string;
  message?: string;
  instructions?: string;
  activationLink?: string;
}

export interface UtilisateurCredentialsDTO {
  username: string;
  temporaryPassword?: string;
  email?: string;
  message?: string;
}

export interface MembreActivationResponseDTO {
  membre: MembreResponse;
  activationCode: ActivationCodeDTO;
  credentials?: UtilisateurCredentialsDTO;
  message: string;
}

export interface MembreCreationResponseDTO {
  membre: MembreResponse;
  credentials: UtilisateurCredentialsDTO;
  message: string;
}
