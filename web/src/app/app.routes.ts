import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'customers', pathMatch: 'full' },
  {
    path: 'customers',
    loadComponent: () =>
      import('./customers/customer-list/customer-list.component').then(m => m.CustomerListComponent)
  },
  {
    path: 'customers/new',
    loadComponent: () =>
      import('./customers/customer-form/customer-form.component').then(m => m.CustomerFormComponent)
  },
  {
    path: 'customers/:id/edit',
    loadComponent: () =>
      import('./customers/customer-form/customer-form.component').then(m => m.CustomerFormComponent)
  },
  { path: '**', redirectTo: 'customers' }
];
