import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { CustomerService } from '../../core/services/customer.service';
import { CustomerRequest } from '../../core/models/customer.model';

@Component({
  selector: 'app-customer-form',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatIconModule
  ],
  templateUrl: './customer-form.component.html',
  styleUrl: './customer-form.component.scss'
})
export class CustomerFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly customerService = inject(CustomerService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);

  customerId = signal<number | null>(null);
  loading = signal(false);
  submitting = signal(false);

  form = this.fb.group({
    nombre:   ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    apellido: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    estado:   ['ACTIVE' as 'ACTIVE' | 'INACTIVE', Validators.required],
    edad:     [null as number | null, [Validators.required, Validators.min(0), Validators.max(150)]]
  });

  get isEdit(): boolean { return this.customerId() !== null; }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.customerId.set(+id);
      this.loading.set(true);
      this.customerService.getById(+id).subscribe({
        next: c => { this.form.patchValue(c); this.loading.set(false); },
        error: () => {
          this.snackBar.open('Customer not found', 'Close', { duration: 3000 });
          this.router.navigate(['/customers']);
        }
      });
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    const value = this.form.value as CustomerRequest;
    const op = this.isEdit
      ? this.customerService.update(this.customerId()!, value)
      : this.customerService.create(value);
    op.subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Customer updated' : 'Customer created', 'Close', { duration: 2000 });
        this.router.navigate(['/customers']);
      },
      error: () => {
        this.snackBar.open('Error saving customer', 'Close', { duration: 3000 });
        this.submitting.set(false);
      }
    });
  }
}
