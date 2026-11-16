# TaskNest — Backend

REST API for **TaskNest**, a personal task management application built with **Spring Boot 4**, **Spring Security**, and **MongoDB**.

- **Live API:** https://tasknest-backend-oglm.onrender.com
- **Frontend Repository:** https://github.com/ammaribnmohd/tasknest-frontend

---

## Features

- **Session-Based Authentication & CSRF Protection**
  - HttpOnly session cookie (`TASKFLOW_SESSION`) with configurable `SameSite` and `Secure` flags
  - Double-submit / header-exposed `XSRF-TOKEN` CSRF protection for SPA clients
  - BCrypt password hashing
- **Email OTP Verification & Password Reset**
  - 6-digit OTP generation hashed with HMAC before persistence in MongoDB
  - Email delivery via Brevo SMTP
  - Rate limiting on login, OTP verification, and OTP requests
- **Task CRUD API**
  - User-scoped tasks with title, description, due date, priority (`HIGH`, `MEDIUM`, `LOW`), and status
  - Input validation & centralized JSON error handling
- **API Documentation**
  - Interactive Swagger UI via `springdoc-openapi` (`/swagger-ui/index.html`)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 27 |
| Framework | Spring Boot 4.1.2 (WebMVC, Security, Validation, Mail, Data MongoDB) |
| Database | MongoDB / MongoDB Atlas |
| API Docs | springdoc-openapi 2.6.0 |
| Email | Brevo SMTP |
| Containerization | Multi-stage Docker (`eclipse-temurin:27-jdk` / `27-jre`) |
| Hosting | Render |

---

## API Endpoints

### Authentication (`/api/auth`)
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register account & send verification OTP | No |
| `POST` | `/api/auth/verify-email` | Verify email with OTP & start session | No |
| `POST` | `/api/auth/resend-verification` | Resend verification OTP | No |
| `POST` | `/api/auth/login` | Authenticate & create session | No |
| `POST` | `/api/auth/logout` | Invalidate current session | Yes |
| `GET` | `/api/auth/me` | Get currently authenticated user | Yes |
| `POST` | `/api/auth/forgot-password` | Request password reset OTP | No |
| `POST` | `/api/auth/verify-reset-otp` | Validate password reset OTP | No |
| `POST` | `/api/auth/reset-password` | Reset password with verified OTP | No |

### Tasks (`/api/tasks`)
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/tasks` | List all tasks for current user | Yes |
| `GET` | `/api/tasks/{id}` | Get a single task by ID | Yes |
| `POST` | `/api/tasks` | Create a new task | Yes |
| `PUT` | `/api/tasks/{id}` | Update task details | Yes |
| `PATCH` | `/api/tasks/{id}/status` | Update task status | Yes |
| `DELETE` | `/api/tasks/{id}` | Delete a task | Yes |

---

## Local Development

### Prerequisites
- Java 27
- MongoDB running locally on `127.0.0.1:27017` (or a MongoDB Atlas URI)
- Brevo SMTP credentials

### Setup & Run
1. Copy `.env.example` to `.env` in the project root and fill in your credentials:
   ```env
   MAIL_USERNAME=your-brevo-smtp-login@smtp-brevo.com
   MAIL_PASSWORD=your-generated-brevo-smtp-key
   MAIL_FROM=your-verified-sender@example.com
   OTP_HMAC_SECRET=change-me-to-a-long-random-string
   ```
2. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
3. The server starts on `http://localhost:8080`.

---

## Environment Variables (Production)

| Variable | Description | Example / Default |
|---|---|---|
| `PORT` | Server port | `8080` (auto-set by Render) |
| `MONGODB_URI` | MongoDB connection string | `mongodb+srv://...` |
| `MAIL_USERNAME` | Brevo SMTP username | — |
| `MAIL_PASSWORD` | Brevo SMTP key | — |
| `MAIL_FROM` | Verified sender email | — |
| `MAIL_PORT` | SMTP port (`2525` recommended on Render free tier) | `587` |
| `OTP_HMAC_SECRET` | Secret key for hashing OTPs (48+ chars) | — |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origin(s), comma-separated | `https://tasknest-frontend-tau.vercel.app` |
| `SESSION_COOKIE_SECURE` | Set `true` in HTTPS production | `false` |
| `SESSION_COOKIE_SAME_SITE` | Cookie `SameSite` policy (`lax` or `none`) | `lax` |

---

## Docker Deployment (Render)

The repository includes a multi-stage `Dockerfile` and `.dockerignore`. Connect the repository on Render with **Language: Docker**, configure the environment variables above, and deploy.
