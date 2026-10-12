# AgroVerse — Smart Farming Platform

AgroVerse is an enterprise-grade digital agriculture platform designed for Indian farmers. It unifies AI-powered crop disease detection, government scheme discovery, community knowledge sharing, peer equipment marketplace, and localized weather intelligence into a responsive, secure web application.

---

## 🌾 System Architecture

```
                       +-----------------------------+
                       |   AgroVerse Frontend (SPA)  |
                       | Vanilla JS / CSS3 / HTML5   |
                       |       Port: 3001            |
                       +--------------+--------------+
                                      |
                     HTTP / REST (JWT Bearer Auth)
                                      |
                                      v
                       +-----------------------------+
                       |   Spring Boot 3.2 Backend   |
                       |       Java 17 / 23          |
                       |       Port: 8080            |
                       +--------------+--------------+
                                      |
       +------------------------------+------------------------------+
       |                              |                              |
       v                              v                              v
+---------------+              +---------------+              +---------------+
| In-Process    |              | Flyway V1     |              | Groq LLaMA-3  |
| ONNX Runtime  |              | PostgreSQL DB |              | Agro-Chatbot  |
| Models (.onnx)|              | (Supabase/H2) |              | & OpenWeather |
+---------------+              +---------------+              +---------------+
```

---

## 🚀 Quick Start Guide

### Prerequisites
1. **Java JDK 17 or higher** (e.g. OpenJDK 17/21/23)
2. **Apache Maven 3.8+**
3. **Python 3.8+** (for frontend static SPA serving)

### 1. Launching Locally (Development Mode)

From the project root:
```bash
# Option A: Single Batch Launcher (Windows)
start_agroverse.bat

# Option B: Manual Command Line
# Terminal 1 - Backend
cd backend
mvn spring-boot:run

# Terminal 2 - Frontend
cd frontend
python server.py
```

- **Frontend Application**: `http://localhost:3001`
- **Backend API**: `http://localhost:8080`
- **Health Check**: `http://localhost:8080/api/health`

---

## 🔒 Security Architecture

1. **Authentication Strategy**:
   - Cryptographically signed **HMAC-SHA256 JWT tokens** (`JwtTokenProvider.java`).
   - Token payload encodes user ID, email, unique farmer ID (`AGV-YYYY-XXXX`), and granted role (`ROLE_FARMER`).
   - Automatic rejection of tampered, malformed, or expired tokens.
2. **Route Authorization**:
   - Public: `/api/auth/register`, `/api/auth/login`, `/api/health`, `/api/schemes`.
   - Protected: `/api/profile/me`, `/api/crop-detection/analyze`, `/api/community/**`, `/api/marketplace/**`.
   - Strict server-side ownership enforcement prevents cross-user resource tampering.
3. **CORS & Preflight**:
   - Environment-driven `agroverse.cors.allowed-origins`.
   - Removed wildcard `*` with credentials conflict to ensure full W3C compliance.
4. **Credential Isolation**:
   - All sensitive keys (JWT secret, Groq API key, OpenWeather API key, PostgreSQL credentials) are loaded via environment variables or `.env`.
   - Committed API keys and hardcoded credentials removed.

---

## 🗄️ Database & Data Integrity

- **Local Development**: In-memory H2 database with automatic schema creation and 26 pre-seeded government schemes.
- **Production Deployment**: Supabase PostgreSQL database governed by versioned Flyway migrations (`V1__init_schema.sql`).
- **Production Profile Activation**:
  ```bash
  export SPRING_PROFILES_ACTIVE=prod
  export SPRING_DATASOURCE_URL=jdbc:postgresql://<SUPABASE_HOST>:5432/postgres
  export SPRING_DATASOURCE_USERNAME=postgres
  export SPRING_DATASOURCE_PASSWORD=<DB_PASSWORD>
  mvn spring-boot:run
  ```

---

## 🔬 In-Process ONNX Inference Engine

AgroVerse executes crop diagnostic models directly within the Java Spring Boot process using `com.microsoft.onnxruntime:onnxruntime`:
- **Variety Classifier**: `crop_variety_model.onnx` (128x128x3 normalized RGB tensor)
- **Disease Classifier**: `crop_disease_model.onnx`
- **Safety Compliance**: Central Insecticides Board & Registration Committee (CIBRC) advisory notices accompany chemical treatment recommendations.
- **Certainty Scoring**: High confidence (>70%), Moderate (40–70%), and Low confidence (<40% prompts photo retake).

---

## 🧪 Automated Test Suite

Run the full verification test suite:
```bash
cd backend
mvn test
```
- **AuthAndSecurityTest**: Validates JWT signing, tampered token rejection, registration workflow, duplicate email prevention, short password rejection, and JSON password masking.
- **CropDetectionInferenceTest**: Tests in-process ONNX inference with RGB tensors, confidence thresholding, and treatment lookup.
- **CommunityAndMarketplaceTest**: Validates community post feed, like actions, and equipment listing CRUD.
- **ExternalIntegrationsTest**: Verifies health check status reporting and missing key graceful degradation.
