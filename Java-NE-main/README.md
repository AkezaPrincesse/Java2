# WASAC & REG Utility Billing System

A Spring Boot REST API for managing water (WASAC) and electricity (REG) utility billing in Rwanda.

## Tech Stack

- **Java 21**, Spring Boot 3.3.5, Maven
- **PostgreSQL** with Flyway migrations
- **Spring Security** — stateless JWT (BCrypt cost 12)
- **OpenAPI / Swagger UI** at `/api/v1/swagger-ui/index.html`
- **OpenPDF** for bill and receipt PDF generation
- **Thymeleaf** + JavaMailSender for email notifications

## Prerequisites

- Java 21+
- PostgreSQL 14+
- Maven 3.9+

## Setup

1. **Create the database:**
   ```sql
   CREATE DATABASE utility_billing;
   ```

2. **Configure credentials** in `src/main/resources/application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/utility_billing
       username: postgres
       password: YOUR_PASSWORD
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

Flyway will auto-run all migrations (V1–V9) and seed default data including the admin account.

## Default Admin Account

| Field    | Value                    |
|----------|--------------------------|
| Email    | `admin@wasac-reg.rw`     |
| Password | `Admin@1234!`            |

> The admin must change their password on first login (forced password change is disabled for the seed admin by default).

## API Base URL

```
http://localhost:8080/api/v1
```

## Roles & Permissions

| Role       | Key Permissions                                                            |
|------------|----------------------------------------------------------------------------|
| `ADMIN`    | Full access — user management, billing, payments, customers, meters        |
| `FINANCE`  | Generate & approve bills, process payments, view reports                   |
| `OPERATOR` | Manage customers, meters, meter readings; view bills and payments          |
| `MANAGER`  | Read-only access to bills, customers, meters, reports                      |
| `CUSTOMER` | View own bills (`/bills/my-bills`), own payments (`/payments/my-payments`) |

## Complete Billing Flow

Follow these steps in Swagger UI to complete an end-to-end transaction:

### 1. Login
`POST /auth/login`
```json
{ "email": "admin@wasac-reg.rw", "password": "Admin@1234!" }
```
Copy the `accessToken` and click **Authorize** in Swagger.

### 2. Create a Customer
`POST /customers`
```json
{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "0780000001",
  "nationalId": "1199000000000001",
  "address": "KG 123 St, Kigali"
}
```

### 3. Create a Meter
`POST /meters`
```json
{
  "customerId": 1,
  "meterNumber": "MTR-001",
  "meterType": "WATER",
  "location": "Main entrance"
}
```

### 4. Record a Meter Reading
`POST /meter-readings`
```json
{
  "meterId": 1,
  "currentReading": 150.0,
  "readingYear": 2024,
  "readingMonth": 1,
  "readingDate": "2024-01-31"
}
```

### 5. Generate Bills
`POST /bills/generate`
```json
{ "billingYear": 2024, "billingMonth": 1 }
```

### 6. Approve a Bill
`POST /bills/approve`
```json
{ "billNumber": "WB-202401-XXXXXX", "notes": "Approved" }
```

### 7. Process Payment
`POST /payments`
```json
{
  "billNumber": "WB-202401-XXXXXX",
  "amount": 5000.00,
  "paymentMethod": "CASH",
  "paidBy": "John Doe"
}
```

### 8. Download PDF Receipt
`GET /payments/receipt/{receiptNumber}/pdf`

## Key Endpoints

### Authentication
| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register` | Self-register as CUSTOMER |
| POST | `/auth/login` | Login and get JWT |
| POST | `/auth/refresh` | Refresh access token |
| POST | `/auth/change-password` | Change password |
| POST | `/auth/forgot-password` | Request OTP reset |

### User Management (ADMIN)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/users` | Create staff account |
| GET | `/users` | List all users |
| GET | `/users/search?keyword=` | Search users |
| PATCH | `/users/{id}/role` | Change user role |
| PATCH | `/users/{id}/activate` | Activate account |
| PATCH | `/users/{id}/deactivate` | Deactivate account |

### Customers
| Method | Path | Description |
|--------|------|-------------|
| POST | `/customers` | Create customer |
| GET | `/customers` | List customers |
| GET | `/customers/search?keyword=` | Search customers |
| GET | `/customers/{id}` | Get customer |
| PUT | `/customers/{id}` | Update customer |

### Meters
| Method | Path | Description |
|--------|------|-------------|
| POST | `/meters` | Create meter |
| GET | `/meters` | List meters |
| GET | `/meters/{id}` | Get meter |
| PATCH | `/meters/{id}/activate` | Activate meter |
| PATCH | `/meters/{id}/deactivate` | Deactivate meter |

### Billing
| Method | Path | Description |
|--------|------|-------------|
| POST | `/bills/generate` | Generate monthly bills |
| POST | `/bills/approve` | Approve a bill |
| POST | `/bills/reject` | Reject a bill |
| GET | `/bills` | List all bills |
| GET | `/bills/search?keyword=` | Search bills |
| GET | `/bills/my-bills` | Customer's own bills |
| GET | `/bills/number/{billNumber}/pdf` | Download bill PDF |
| PATCH | `/bills/{id}/cancel` | Cancel a bill |

### Payments
| Method | Path | Description |
|--------|------|-------------|
| POST | `/payments` | Process payment |
| GET | `/payments` | List all payments |
| GET | `/payments/search?keyword=` | Search payments |
| GET | `/payments/my-payments` | Customer's own payments |
| GET | `/payments/receipt/{receiptNumber}/pdf` | Download receipt PDF |

## Business Rules

- Bills cannot be generated for future billing periods.
- A meter reading is required before a bill can be generated.
- Only `PENDING` bills can be approved or rejected.
- Only `APPROVED` bills can be paid.
- `PAID` bills cannot be cancelled.
- Retroactive bills use the tariff that was active at the end of the billing period (not today's tariff).
- Payment amount cannot exceed the remaining balance.
- Deactivated customers are skipped during bill generation.

## Environment Variables (Optional Overrides)

```yaml
JWT_SECRET: your-256-bit-secret
MAIL_HOST: smtp.gmail.com
MAIL_USERNAME: your@gmail.com
MAIL_PASSWORD: your-app-password
```
