export interface Usuario {
  id: number;
  nombre: string;
  apellido: string | null;
  email: string;
  zonaHoraria: string;
  roles: string[];
}

export interface TokenResponse {
  token: string;
  expira: string;
  usuario: Usuario;
}
