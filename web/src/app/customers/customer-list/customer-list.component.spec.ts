import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { CustomerListComponent } from './customer-list.component';
import { CustomerService } from '../../core/services/customer.service';
import { Customer } from '../../core/models/customer.model';
import { vi } from 'vitest';

describe('CustomerListComponent', () => {
  let fixture: ComponentFixture<CustomerListComponent>;
  let component: CustomerListComponent;

  const mockCustomers: Customer[] = [
    { id: 1, nombre: 'Alex', apellido: 'Prieto', estado: 'ACTIVE', edad: 30 },
    { id: 2, nombre: 'Ana', apellido: 'Lopez', estado: 'ACTIVE', edad: 25 }
  ];

  const customerServiceMock = {
    getActive: vi.fn().mockReturnValue(of(mockCustomers)),
    delete: vi.fn().mockReturnValue(of(undefined))
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    customerServiceMock.getActive.mockReturnValue(of(mockCustomers));

    await TestBed.configureTestingModule({
      imports: [CustomerListComponent],
      providers: [
        provideRouter([]),
        provideAnimations(),
        { provide: CustomerService, useValue: customerServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load active customers on init', () => {
    expect(customerServiceMock.getActive).toHaveBeenCalled();
    expect(component.customers()).toEqual(mockCustomers);
  });

  it('should not be loading after data loads', () => {
    expect(component.loading()).toBe(false);
  });
});
