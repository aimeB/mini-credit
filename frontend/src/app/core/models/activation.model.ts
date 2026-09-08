export interface ActivationCodeDTO {
  code: string;
  memberName?: string;
  phoneNumber?: string;
  message?: string;
  instructions?: string;
  activationLink?: string;
}

export interface ActivationRequest {
  activationCode: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ActivationResponse {
  message: string;
  token: string;
  tokenType: string;
  username: string;
  expiresIn: number;
}

export interface MembreActivationResponseDTO {
  membre: any; // MembreResponse
  activationCode: ActivationCodeDTO;
  credentials?: {
    username?: string;
    email?: string;
    temporaryPassword?: string;
    message?: string;
  };
  message: string;
}
