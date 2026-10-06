# ESIBuy — Plataforma de Comercio Electrónico Seguro

**ESIBuy** es una plataforma web de comercio electrónico desarrollada como proyecto integrado universitario. El sistema adopta una arquitectura de **Monolito Modular** en el backend utilizando **Java con Spring Boot**, un frontend en **Angular** y **MongoDB Atlas** como motor de base de datos no relacional.

El desarrollo se rige por metodologías ágiles (**SCRUM**), desarrollo guiado por pruebas (**TDD**), controles estrictos de seguridad bajo la norma **ISO/IEC 27001:2022** y pautas **CCN-CERT BP/28**, así como integración continua controlada mediante **Azure DevOps** y **SonarQube**.

---

## Pila Tecnológica (Tech Stack)

* **Backend:** Java 17 / 21, Spring Boot 3 (Spring Data MongoDB, Spring Security, Spring Validation).


* **Frontend:** Angular 17+ (TypeScript, RxJS, HTML5, SCSS/CSS).


* **Base de Datos:** MongoDB Atlas (Persistencia NoSQL documental BSON).


* **Seguridad:** Argon2id / Bcrypt + Pepper, Cookies HttpOnly/Secure/SameSite, 3FA (TOTP + Email OTP), RBAC.


* **Testing & Calidad:** JUnit 5, Mockito, SonarQube / SonarLint (Cobertura mínimo 60%).


* **DevOps & IA:** Azure DevOps (Repos, Boards, Pipelines), GitHub Copilot (asistido por *Skills/Prompts* documentados).

---

## Estructura del Repositorio

El proyecto utiliza un enfoque de **Monorepo** organizado por dominios lógicos:

esibuy/
├── esibuy-frontend/              # Proyecto Frontend en Angular CLI
├── esibuy-backend/               # Proyecto Backend en Spring Boot (Maven)
│   └── src/main/java/es/uclm/esibuy/
│       ├── EsibuyApplication.java
│       ├── common/               # Configuraciones globales, excepciones y logging JSON
│       ├── security/             # Spring Security, Cookies, RBAC, 3FA y Rate Limiting
│       ├── users/                # Dominio de Usuarios y Back-office de Administración
│       └── products/             # Dominio de Catálogo de Vendedores e Inventario
├── docs/                         # Análisis de Riesgos (MARISMA), Skills de IA, Manuales
├── scripts/                      # Colecciones de pruebas API (Bruno/Postman) y despliegue
└── .gitignore                    # Exclusiones de artefactos, node_modules y secretos

---

## Guía de Inicio Rápido (Desarrollo Local)

### Prerrequisitos

* Java JDK 17 o superior.
* Node.js (v18+) y Angular CLI (`npm install -g @angular/cli`).
* Cuenta / Clúster activo en MongoDB Atlas.

### 1. Configuración de Variables de Entorno (Backend)

No subas credenciales reales al repositorio. Crea un archivo `application-local.properties` en `esibuy-backend/src/main/resources/` (ignorado por Git) o exporta la variable en tu entorno local:

spring.data.mongodb.uri=${MONGO_URI:mongodb+srv://:@cluster.mongodb.net/esibuy}
security.pepper=${SECURITY_PEPPER:ClaveSecretaGlobalPepper123!}

### 2. Ejecución del Backend (Spring Boot)

cd esibuy-backend
mvn clean compile
mvn spring-boot:run

El servidor backend se iniciará por defecto en `https://localhost:8080`.

### 3. Ejecución del Frontend (Angular)

cd esibuy-frontend
npm install
ng serve

Accede a la aplicación desde tu navegador en `http://localhost:4200`.

---

## Flujo de Trabajo en Git (GitFlow Simplificado)

1. **`main`**: Código estable y entregable de cada Sprint.


2. **`develop`**: Rama de integración continua.


3. **`feature/HDU-XX-descripcion`**: Ramas de trabajo para cada Historia de Usuario asociadas al tablero de Azure DevOps.


4. **Pull Requests:** Requerida revisión de código y paso satisfactorio del pipeline de integración antes del merge a `develop`.



---

## 🤖 Uso Responsable de IA Generativa

El uso de **GitHub Copilot** se encuentra guiado y acotado mediante especificaciones y *skills* previas documentadas en la carpeta `/docs`. Todo código o prueba generada por IA es revisado, probado y adaptado bajo la responsabilidad directa del equipo.
