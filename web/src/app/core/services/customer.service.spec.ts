import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { CustomerService } from './customer.service';
import { Customer, CustomerRequest } from '../models/customer.model';

describe('CustomerService', () => {
  let service: CustomerService;
  let httpMock: HttpTestingController;

  const mockCustomer: Customer = { id: 1, nombre: 'Alex', apellido: 'Prieto', estado: 'ACTIVE', edad: 30 };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(CustomerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should get active customers', () => {
    service.getActive().subscribe(customers => {
      expect(customers).toEqual([mockCustomer]);
    });
    const req = httpMock.expectOne(r => r.url.endsWith('/active'));
    expect(req.request.method).toBe('GET');
    req.flush([mockCustomer]);
  });

  it('should get customer by id', () => {
    service.getById(1).subscribe(c => expect(c).toEqual(mockCustomer));
    const req = httpMock.expectOne(r => r.url.endsWith('/1'));
    expect(req.request.method).toBe('GET');
    req.flush(mockCustomer);
  });

  it('should create a customer', () => {
    const request: CustomerRequest = { nombre: 'Alex', apellido: 'Prieto', estado: 'ACTIVE', edad: 30 };
    service.create(request).subscribe(c => expect(c).toEqual(mockCustomer));
    const req = httpMock.expectOne(r => r.method === 'POST');
    req.flush(mockCustomer);
  });

  it('should update a customer', () => {
    const request: CustomerRequest = { nombre: 'Alexander', apellido: 'Prieto', estado: 'ACTIVE', edad: 31 };
    const updated: Customer = { id: 1, ...request };
    service.update(1, request).subscribe(c => expect(c).toEqual(updated));
    const req = httpMock.expectOne(r => r.method === 'PUT' && r.url.endsWith('/1'));
    req.flush(updated);
  });

  it('should delete a customer', () => {
    service.delete(1).subscribe();
    const req = httpMock.expectOne(r => r.method === 'DELETE' && r.url.endsWith('/1'));
    req.flush(null);
  });
});
