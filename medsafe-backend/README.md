# Medication Safety & Interaction Assistant

A secure, evidence-grounded full-stack healthcare web application designed to help patients safely manage prescribed medications, schedule doses, log adherence, detect duplicate active ingredients across brand names, and review verified drug-drug interactions with patient-friendly AI explanations.

---

## ⚠️ Important Healthcare Disclaimers

> [!CAUTION]
> **Not Medical Advice**: The Medication Safety & Interaction Assistant is designed strictly for informational and educational purposes. It does **not** provide medical advice, diagnosis, treatment plans, or prescription recommendations.
>
> - **Never Discontinue or Adjust Medications Independently**: Patients must never stop, start, or alter the dosage of any prescribed medication without directly consulting their prescribing physician or dispensing pharmacist.
> - **Absence of Documented Interaction Is Not Proof of Safety**: When an interaction check returns `No interaction identified in the consulted source`, this indicates only that the consulted reference source (openFDA / RxNorm) does not list a documented pairwise interaction. It does **not** guarantee that the combination is risk-free or medically appropriate.
> - **Evidence Grounding**: Clinical information is sourced strictly from trusted regulatory and clinical databases (NLM RxNorm and FDA Structured Product Labeling via openFDA). Artificial Intelligence is used solely to generate patient-accessible explanations of retrieved, verified evidence and is never a source of clinical truth.

---

## 1. Problem Statement

Polypharmacy—the concurrent use of five or more medications—affects over 40% of older adults and millions of chronic-care patients worldwide. This creates severe clinical risks:
1. **Adverse Drug Events (ADEs)**: Unrecognized drug-drug interactions cause hundreds of thousands of preventable emergency hospitalizations each year.
2. **The "Hidden Duplicate" Trap (Additive Toxicity)**: Patients frequently take multiple over-the-counter (OTC) and prescribed brand-name medications (e.g., *Advil* and *Motrin*) without realizing they share the identical active chemical entity (*ibuprofen*), resulting in accidental toxic overdoses.
3. **Medical AI Hallucination Dangers**: Conversational AI chatbots commonly hallucinate nonexistent drug contraindications or, even worse, provide dangerous false reassurances that lethal combinations are safe.
4. **Poor Adherence & Regimen Confusion**: Complex dosing schedules lead to missed doses, improper meal timings, and poor disease control.

---

## 2. Proposed Solution

**Medication Safety & Interaction Assistant** bridges the gap between official regulatory clinical data and patient comprehension:
- **Prescription & Schedule Management**: Empowers patients to record their complete prescription regimen, schedule daily intake with meal relation contexts, and track adherence (`TAKEN` / `MISSED`).
- **Automated Duplicate Active-Ingredient Detection**: Normalizes brand names to clinical active entities via NLM RxNorm and flags dangerous duplications across the regimen without altering medical orders.
- **Evidence-Grounded Pairwise Interaction Screening**: Screens co-prescribed pairs against official FDA Structured Product Labeling (SPL) via openFDA.
- **Explain-Only AI Layer**: AI is restricted to translating retrieved, verified FDA labeling into plain, patient-accessible language while enforcing strict medical disclaimers.
- **Deterministic Non-Clinical Absence (`NOT_FOUND`)**: Never makes dangerous claims of "complete safety" when an interaction is absent from regulatory sources.

---

## 3. Features

- **Patient Authentication & Security**: Secure registration and login using BCrypt password hashing and signed Bearer JWTs with constant-time HMAC verification.
- **Regimen Tracking**: Store medication name, active ingredient, dosage, frequency, and start/end dates with input validation (`endDate >= startDate`).
- **Intelligent Dose Scheduling**: Associate daily intake times and meal relations (`BEFORE_MEAL`, `AFTER_MEAL`, `WITH_MEAL`, `NO_RESTRICTION`) with each medication.
- **Adherence Logging**: One-click dose recording (`TAKEN` / `MISSED`) with persistent, immutable medication history.
- **Active Ingredient Duplicate Alerts**: Flags overlapping active ingredients across diverse brand names with additive toxicity warnings.
- **Pairwise Interaction Screening**: Evaluates pairwise combinations against official FDA SPL labeling with traceable SPL Set ID references.
- **Evidence-Grounded AI Explanations**: Patient-friendly explanations strictly grounded in retrieved evidence with prominent medical disclaimers.
- **Interactive Safety Dashboard**: Real-time overview of active prescriptions, today's scheduled doses, adherence counts, and active safety alerts.

---

## 4. System Architecture

```
User / Patient
     │
     ▼
React 19 Frontend (Vite, Glassmorphic UI, Port 5173)
     │
     ▼ REST API (Signed Bearer JWT)
Spring Boot 4 Backend (Java 21 LTS, Port 8080)
     │
     ├─────────► MySQL Database (Validated with ddl-auto=validate)
     │
     ▼
RxNorm Normalization (NLM RxNav REST API: RxCUI, Brand-to-Generic, Active Entity)
     │
     ▼
Evidence-Grounded Interaction Engine (Pairwise Cross-Matching & Duplicate Detection)
     │
     ▼
openFDA Drug Labeling (Official Structured Product Labeling / SPL text)
     │
     ▼
Verified Clinical Evidence (FDA SPL Text + Source Citation + SPL Set ID)
     │
     ▼
AI Explanation Layer (DefaultAIExplanationService with Prompt Guardrails)
     │
     ▼
Patient-Friendly Explanation (Plain English, Clinician Advisory, Disclaimers)
```

For full architecture diagrams and flow descriptions, see [ARCHITECTURE.md](file:///C:/Users/aksha/.gemini/antigravity/scratch/medsafe-backend/ARCHITECTURE.md).

---

## 5. Technology Stack

- **Backend**:
  - Java 21 LTS
  - Spring Boot 4.0.8
  - Spring Data JPA
  - Spring Security (Stateless Bearer Authentication)
  - Hibernate ORM 7.x
  - HikariCP Connection Pooling
  - JJWT / HMAC-SHA256 Token Service
- **Database**:
  - MySQL 8.x (`medsafe` database, validated with `spring.jpa.hibernate.ddl-auto=validate`)
- **Frontend**:
  - React 19
  - Vite 6
  - React Router DOM
  - Lucide React Icons
  - Pure CSS Design System (zero heavy UI library dependencies)
- **Clinical Data & External APIs**:
  - **NLM RxNorm REST API**: Concept lookup, brand-to-generic mapping, RxCUI resolution.
  - **openFDA Drug Labeling API**: Structured Product Labeling (SPL) drug interaction section parsing.

---

## 6. Database Schema & Safety

The database schema is strictly validated against MySQL using `spring.jpa.hibernate.ddl-auto=validate` with zero runtime schema modifications:
- `users`: User identity (`user_id`, `name`, `email`, `password_hash`, `created_at`).
- `medications`: Prescription records (`medication_id`, `user_id`, `medicine_name`, `active_ingredient`, `dosage`, `frequency`, `start_date`, `end_date`, `status`, `created_at`).
- `schedules`: Dosing intervals (`schedule_id`, `medication_id`, `scheduled_time`, `meal_relation`).
- `medication_logs`: Adherence events (`log_id`, `schedule_id`, `log_time`, `status`, `notes`).
- `interaction_alerts`: Documented pairwise alerts (`alert_id`, `medication_id_1`, `medication_id_2`, `severity`, `description`, `source_name`, `source_reference`, `checked_at`).
- `drug_information`: Local cache for resolved RxNorm and FDA labeling records (`drug_id`, `drug_name`, `standard_name`, `category`, `external_source`, `external_id`, `updated_at`).

---

## 7. Security Hardening & IDOR Protection

1. **Authentication & Token Integrity**:
   - Passwords hashed using industry-standard **BCrypt** (`$2a$`). Plaintext passwords and password hashes are never exposed in REST responses.
   - Signed Bearer tokens verified via constant-time HMAC-SHA256 comparisons (`MessageDigest.isEqual`) to eliminate timing attack vectors.
   - Diagnostic backdoors (`X-User-Email`) completely removed.
2. **Insecure Direct Object Reference (IDOR) Elimination**:
   - Every protected entity (medications, dosing schedules, adherence logs, interaction alerts) strictly validates ownership against the authenticated user ID (`securityUtils.getCurrentUserId()`).
   - Unauthorized attempts to read, modify, or delete another patient's resources return `401 Unauthorized` / `404 Not Found`.
3. **Data Sanitization & Leakage Prevention**:
   - `GlobalExceptionHandler` intercepts unhandled exceptions, logs technical details server-side via SLF4J, and returns sanitized error JSON responses.
   - Internal stack traces, package paths (`org.springframework`, `com.medsafe`), and raw SQL/Hibernate exceptions are never leaked to clients.
4. **Input Validation**:
   - Bean validation and service-level rules enforce non-blank names, valid status (`ACTIVE`/`INACTIVE`), and `endDate >= startDate`.

---

## 8. Evidence Architecture

The clinical evidence engine enforces rigorous safety rules:
1. **True Pairwise Interaction Verification**:
   - The engine does not mark an interaction as `VERIFIED` merely because a drug label contains general interaction text.
   - It requires explicit bidirectional or reciprocal textual evidence matching the specific co-prescribed medication, its active ingredient, or recognized synonyms.
2. **Deterministic Non-Clinical Absence (`NOT_FOUND`)**:
   - When no documented interaction is located in the queried FDA labeling, the system returns `No interaction identified in the consulted source.` with evidence status `NOT_FOUND`.
   - The system never claims or displays that the combination is "safe" or "free of risk".
3. **Ambiguity Resolution (`AMBIGUOUS`)**:
   - If an entered medicine matches multiple distinct clinical formulations (e.g., Metoprolol Tartrate vs. Metoprolol Succinate), interaction analysis halts with status `AMBIGUOUS`, instructing the patient to confirm the formulation before proceeding.
4. **Duplicate Active Ingredient Detection**:
   - Analyzes active ingredients across diverse brand names (e.g., identifying that both **Advil** and **Motrin** contain **ibuprofen**).
   - Issues clinical warnings against unintended additive dosage and toxicity without modifying or deleting the patient's prescriptions.

---

## 9. AI Safety & Explain-Only Architecture

- **Grounding Mandate**: The AI service (`DefaultAIExplanationService`) receives **only** retrieved, verified clinical evidence from official regulatory sources.
- **Mock Data Rejection**: Mock data (`DEVELOPMENT_MOCK`) or unverified sources are rejected immediately with `400 Bad Request`.
- **Zero Severity Hallucination**: AI accepts the regulatory source severity rating as ground truth and is strictly prohibited from altering or guessing severity levels.
- **Mandatory Medical Disclaimers**: Every AI response embeds a prominent non-negotiable medical disclaimer advising consultation with a healthcare professional.
- **No Discontinuation Commands**: Prompt guardrails prevent the AI from directing the patient to alter doses or discontinue therapies independently.

---

## 10. Setup & Execution Instructions

### Prerequisites
- **Java 21** (JDK 21 LTS)
- **Node.js** (v18+ or v20+) & **npm**
- **MySQL 8.x** running locally on port 3306

### Database Configuration
Ensure MySQL has the database `medsafe` created:
```sql
CREATE DATABASE IF NOT EXISTS medsafe;
```

Configure credentials in `medsafe-backend/src/main/resources/application.properties` if needed:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/medsafe?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=validate
```

### Backend Execution
```powershell
cd C:\Users\aksha\.gemini\antigravity\scratch\medsafe-backend

# Run all 47 tests
.\mvnw.cmd clean test

# Start the Spring Boot application (port 8080)
.\mvnw.cmd spring-boot:run
```

### Frontend Execution
```powershell
cd C:\Users\aksha\.gemini\antigravity\scratch\medsafe-frontend

# Install dependencies
npm install

# Start Vite dev server (port 5173)
npm run dev

# Build production bundle
npm run build
```

---

## 11. Hackathon Demo Instructions (3–5 Minutes)

1. **Launch**: Open `http://localhost:5173` in a web browser.
2. **Register/Login**: Click "Create an account", register a demo patient (e.g. `jane.doe@medsafe.org`, password: `Password@123`).
3. **Add First Medication**:
   - Medicine Name: `Warfarin`
   - Active Ingredient: `warfarin`
   - Dosage: `5mg`
   - Frequency: `Once daily at bedtime`
   - Dates: Jan 1, 2026 to Dec 31, 2026
4. **Add Second Medication**:
   - Medicine Name: `Aspirin`
   - Active Ingredient: `aspirin`
   - Dosage: `81mg`
   - Frequency: `Once daily in morning`
5. **Demonstrate Duplicate Warning**:
   - Add `Advil` (active ingredient: `ibuprofen`) and `Motrin` (active ingredient: `ibuprofen`).
   - Notice the prominent **Duplicate Active Ingredient Alert** banner on the Dashboard warning of unintended additive toxicity.
6. **Screen Pairwise Interactions**:
   - Navigate to **Safety Alerts** and click **Run Interaction Check**.
   - Review the verified interaction alert between **Warfarin** and **Aspirin** showing the source `FDA Drug Labeling (openFDA)` and SPL Set ID reference.
7. **Explain with AI**:
   - Click **Explain with AI** on the Warfarin + Aspirin alert.
   - Review the plain-language explanation of bleeding risks, clinical source attribution, and safety disclaimer warning against stopping therapy without a doctor.
8. **Schedule & Log Dose**:
   - Navigate to **Schedule**, observe scheduled doses, and click **Mark Taken**.
   - Check **Medication History** to verify the recorded adherence log.
9. **Persistence**: Log out and log back in; observe that all medications, schedules, logs, and alerts are persistently maintained.

---

## 12. Automated Testing

The project includes 47 automated tests with 100% pass rate:

| Test Class | Tests | Status | Scope |
|---|---|---|---|
| `MedsafeApplicationTests` | 1 | PASS | Spring context bootstrapping |
| `MedsafeFullFlowIntegrationTest` | 7 | PASS | End-to-end user flow with MockMvc and security |
| `Phase4EvidenceIntegrationTest` | 11 | PASS | RxNorm exact/normalized lookup, openFDA parsing, caching, timeouts |
| `Phase4SafetyAuditTest` | 8 | PASS | Reciprocal pairwise verification audit, NOT_FOUND non-clinical safety |
| `Phase5HardeningSecurityTest` | 20 | PASS | Scenarios A through T (BCrypt, IDOR, Token attacks, Validations, AI Safety) |
| **Total** | **47** | **100% PASS** | **0 Failures, 0 Errors, 0 Skipped** |

To execute tests:
```powershell
.\mvnw.cmd clean test
```

---

## 13. Limitations

1. **Regulatory Label Scope**: OpenFDA Structured Product Labeling (SPL) reflects US FDA-approved drug package inserts. Foreign drug formulations or experimental compounds not registered with the FDA will return `NOT_FOUND` or `SOURCE_UNAVAILABLE`.
2. **Text Parsing Complexity**: Free-text SPL `drug_interactions` sections use natural language variation; while active ingredient and synonym resolution catch documented pairs, unusual or novel compound abbreviations may require manual clinical review.
3. **Not a Clinical Device**: The application is an informational safety assistant and is not FDA Class II/III Software as a Medical Device (SaMD). It must never replace professional pharmacist or physician judgment.

---

## 14. Future Scope

1. **FHIR / EHR Interoperability**: Direct bidirectional synchronization with hospital electronic health record systems (Epic, Cerner) via HL7 FHIR APIs.
2. **Food, Alcohol, & Dietary Supplement Screening**: Expanding the interaction engine to screen against dietary substances (grapefruit juice, St. John's Wort, alcohol).
3. **Pharmacogenomics (PGx) Integration**: Incorporating Clinical Pharmacogenetics Implementation Consortium (CPIC) guidelines to flag gene-drug interactions based on patient metabolic phenotypes.
4. **Caregiver & Family Delegate Portal**: Role-based access control (RBAC) allowing authorized caregivers to assist elderly patients with schedule monitoring and adherence logging.
