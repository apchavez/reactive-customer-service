export type CustomerState = 'ACTIVE' | 'INACTIVE';

export interface Customer {
  id: number;
  nombre: string;
  apellido: string;
  estado: CustomerState;
  edad: number;
}

export interface CustomerRequest {
  nombre: string;
  apellido: string;
  estado: CustomerState;
  edad: number;
}
