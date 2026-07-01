# Customer Service — Angular Frontend

Angular 21 SPA for the Customer Service platform. Provides a Material Design UI to create, view, update, and delete customers via the Spring Boot WebFlux API.

## Tech Stack

| Category | Technology |
|---|---|
| Framework | Angular 21 (standalone components) |
| UI Library | Angular Material (M3 theme) |
| HTTP | Angular HttpClient + RxJS |
| Forms | Angular Reactive Forms |
| Tests | Vitest + Angular TestBed |
| Build | Angular CLI, Docker multi-stage (nginx) |

## Getting Started

```bash
npm install
npm start
```

Opens at `http://localhost:4200`. Connects to the API at `http://localhost:8080` by default.

To point to a different backend, edit `src/environments/environment.ts`:

```ts
export const environment = {
  production: false,
  apiUrl: 'http://your-api-host:8080'
};
```

For production builds the equivalent file is `src/environments/environment.prod.ts`.

## Running Tests

```bash
npm test
```

| Suite | File | What it covers |
|---|---|---|
| App bootstrap | `app.spec.ts` | Root component creation and title |
| HTTP service | `customer.service.spec.ts` | HttpClient calls, request/response mapping |
| Customer list | `customer-list.component.spec.ts` | Table rendering, loading state |
| Customer form | `customer-form.component.spec.ts` | Form validation, create/edit modes |

## Production Build

```bash
npm run build
```

Output goes to `dist/`. The `Dockerfile` uses a multi-stage build (Node → nginx) and serves the compiled assets on port 80.

## Project Structure

```
src/app/
├── core/
│   ├── models/        customer.model.ts
│   └── services/      customer.service.ts
└── customers/
    ├── customer-list/ list view with Material table
    └── customer-form/ create / edit dialog
```
