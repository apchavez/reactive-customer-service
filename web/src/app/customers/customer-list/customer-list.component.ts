import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CustomerService } from '../../core/services/customer.service';
import { Customer } from '../../core/models/customer.model';

@Component({
  selector: 'app-customer-list',
  imports: [
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './customer-list.component.html',
  styleUrl: './customer-list.component.scss'
})
export class CustomerListComponent implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  customers = signal<Customer[]>([]);
  loading = signal(false);
  readonly displayedColumns = ['id', 'nombre', 'apellido', 'estado', 'edad', 'actions'];

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.customerService.getActive().subscribe({
      next: data => {
        this.customers.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Error loading customers', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  edit(id: number): void {
    this.router.navigate(['/customers', id, 'edit']);
  }

  delete(id: number): void {
    this.customerService.delete(id).subscribe({
      next: () => {
        this.snackBar.open('Customer deleted', 'Close', { duration: 2000 });
        this.load();
      },
      error: () => this.snackBar.open('Error deleting customer', 'Close', { duration: 3000 })
    });
  }
}
