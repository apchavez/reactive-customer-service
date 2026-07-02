import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { CustomerFormComponent } from './customer-form.component';
import { CustomerService } from '../../core/services/customer.service';
import { vi } from 'vitest';

describe('CustomerFormComponent', () => {
  let fixture: ComponentFixture<CustomerFormComponent>;
  let component: CustomerFormComponent;

  const customerServiceMock = {
    getById: vi.fn(),
    create: vi.fn().mockReturnValue(of({ id: 1, nombre: 'Alex', apellido: 'Prieto', estado: 'ACTIVE', edad: 30 })),
    update: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [CustomerFormComponent],
      providers: [
        provideRouter([]),
        provideAnimations(),
        { provide: CustomerService, useValue: customerServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should be in create mode when no id param', () => {
    expect(component.isEdit).toBe(false);
    expect(component.customerId()).toBeNull();
  });

  it('should have invalid form when empty', () => {
    component.form.reset();
    expect(component.form.invalid).toBe(true);
  });

  it('should have valid form with correct values', () => {
    component.form.patchValue({ nombre: 'Alex', apellido: 'Prieto', estado: 'ACTIVE', edad: 30 });
    expect(component.form.valid).toBe(true);
  });
});
