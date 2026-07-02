import { test, expect, Route } from '@playwright/test';

interface Customer {
  id: number;
  nombre: string;
  apellido: string;
  estado: 'ACTIVE' | 'INACTIVE';
  edad: number;
}

const CUSTOMER: Customer = {
  id: 1,
  nombre: 'Juan',
  apellido: 'García',
  estado: 'ACTIVE',
  edad: 30,
};

function mockCustomers(customers: Customer[]) {
  return async (route: Route) => {
    const method = route.request().method();
    const url = route.request().url();

    if (method === 'GET' && url.includes('/active')) {
      await route.fulfill({ json: [...customers] });
    } else if (method === 'GET') {
      const id = Number(url.split('/').pop());
      const found = customers.find(c => c.id === id);
      await route.fulfill({ json: found ?? CUSTOMER });
    } else if (method === 'POST') {
      const body = await route.request().postDataJSON();
      const created = { id: customers.length + 1, ...body };
      customers.push(created);
      await route.fulfill({ status: 201, json: created });
    } else if (method === 'PUT') {
      const id = Number(url.split('/').pop());
      const body = await route.request().postDataJSON();
      const idx = customers.findIndex(c => c.id === id);
      if (idx >= 0) customers[idx] = { ...customers[idx], ...body };
      await route.fulfill({ json: customers[idx >= 0 ? idx : 0] });
    } else if (method === 'DELETE') {
      const id = Number(url.split('/').pop());
      const idx = customers.findIndex(c => c.id === id);
      if (idx >= 0) customers.splice(idx, 1);
      await route.fulfill({ status: 204, body: '' });
    } else {
      await route.continue();
    }
  };
}

test.describe('Customers', () => {
  test('shows empty state when no customers exist', async ({ page }) => {
    await page.route('**/api/v1/customers**', mockCustomers([]));

    await page.goto('/customers');

    await expect(page.getByText('No active customers found.')).toBeVisible();
  });

  test('shows customers in the table', async ({ page }) => {
    await page.route('**/api/v1/customers**', mockCustomers([CUSTOMER]));

    await page.goto('/customers');

    await expect(page.getByText('Juan')).toBeVisible();
    await expect(page.getByText('García')).toBeVisible();
    await expect(page.getByText('30')).toBeVisible();
  });

  test('creates a customer and shows it in the table', async ({ page }) => {
    const customers: Customer[] = [];
    await page.route('**/api/v1/customers**', mockCustomers(customers));

    await page.goto('/customers');
    await expect(page.getByText('No active customers found.')).toBeVisible();

    await page.getByRole('link', { name: /New Customer/i }).click();
    await expect(page).toHaveURL(/\/customers\/new/);

    await page.getByLabel('First Name').fill('Juan');
    await page.getByLabel('Last Name').fill('García');
    await page.getByLabel('Age').fill('30');

    await page.getByRole('button', { name: 'Create' }).click();

    await expect(page).toHaveURL(/\/customers$/);
    await expect(page.getByText('Juan')).toBeVisible();
    await expect(page.getByText('García')).toBeVisible();
  });

  test('edits a customer and shows updated data', async ({ page }) => {
    const customers: Customer[] = [{ ...CUSTOMER }];
    await page.route('**/api/v1/customers**', mockCustomers(customers));

    await page.goto('/customers');
    await expect(page.getByText('Juan')).toBeVisible();

    await page.getByLabel('Edit customer').first().click();
    await expect(page).toHaveURL(/\/customers\/1\/edit/);

    await page.getByLabel('First Name').clear();
    await page.getByLabel('First Name').fill('Carlos');
    await page.getByRole('button', { name: 'Update' }).click();

    await expect(page).toHaveURL(/\/customers$/);
    await expect(page.getByText('Carlos')).toBeVisible();
  });

  test('deletes a customer and shows empty state', async ({ page }) => {
    const customers: Customer[] = [{ ...CUSTOMER }];
    await page.route('**/api/v1/customers**', mockCustomers(customers));

    await page.goto('/customers');
    await expect(page.getByText('Juan')).toBeVisible();

    await page.getByLabel('Delete customer').first().click();

    await expect(page.getByText('No active customers found.')).toBeVisible();
  });

  test('shows validation errors for empty required fields', async ({ page }) => {
    await page.route('**/api/v1/customers**', mockCustomers([]));

    await page.goto('/customers/new');

    await page.getByLabel('First Name').focus();
    await page.getByLabel('First Name').blur();

    await expect(page.getByText('First name is required (2–50 characters)')).toBeVisible();
  });

  test('shows validation error for short name', async ({ page }) => {
    await page.route('**/api/v1/customers**', mockCustomers([]));

    await page.goto('/customers/new');

    await page.getByLabel('First Name').fill('A');
    await page.getByLabel('First Name').blur();

    await expect(page.getByText('First name is required (2–50 characters)')).toBeVisible();
  });
});
