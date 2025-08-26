# SWAPI Challenge · Java 8 · Spring Boot 2.7 · GraphQL

Aplicación **gateway** que integra con **[SWAPI](https://www.swapi.tech/documentation)** para listar **People, Films, Starships y Vehicles**. 
Incluye:

- **GraphQL** como interfaz unificada (`POST /graphql`).
- **Paginación** y **filtros** por `name`/`id` (en **Films** se mapea a `title`).
- **Autenticación JWT** con **Spring Security** (login/register).
- **Cache** con **Caffeine**.
- **Resiliencia** con **Resilience4j** (Retry/CircuitBreaker).
- **Observabilidad** con **Spring Boot Actuator** + **Correlation-Id**.
- **H2** (opcional) para usuarios en memoria.
- **Colección Postman** lista para usar: `postman/SWAPI_Challenge.postman_collection.json`.

---

## Tabla de contenidos
- [Stack](#stack)
- [Requisitos](#requisitos)
- [Configuración](#configuración)
- [Cómo correr](#cómo-correr)
- [Autenticación y seguridad](#autenticación-y-seguridad)
- [API (GraphQL)](#api-graphql)
  - [Paginación y filtros](#paginación-y-filtros)
  - [Ejemplos de queries](#ejemplos-de-queries)
- [Uso con Postman](#uso-con-postman)
- [Cache & Resiliencia](#cache--resiliencia)
- [Observabilidad](#observabilidad)
- [Estructura de paquetes](#estructura-de-paquetes)
- [Tests](#tests)
- [Despliegue](#despliegue)
- [Troubleshooting](#troubleshooting)

---

## Stack
- **Java 8**, **Spring Boot 2.7.18**
- **spring-boot-starter-graphql**, **spring-boot-starter-web**
- **spring-boot-starter-security** + **jjwt**
- **spring-boot-starter-cache** + **Caffeine**
- **resilience4j-spring-boot2** (Retry / CircuitBreaker)
- **Actuator/Micrometer**
- **H2** (opcional), **Lombok**
- **Tests**: `spring-graphql-test`, `wiremock-jre8`, `spring-security-test`, `junit`

---

## Requisitos
- **Java 8**
- **Maven 3.8+**
- (Opcional) **Docker**

---

## Configuración

Archivo recomendado `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

swapi:
  base-url: https://www.swapi.tech/api

security:
  jwt:
    secret: change-me-in-prod   # SECRET para firmar JWT
    expiration: 3600            # segundos

spring:
  cache:
    type: caffeine
  # H2 en memoria (opcional para usuarios)
  h2:
    console:
      enabled: true
      path: /h2-console
  datasource:
    url: jdbc:h2:mem:swapi;DB_CLOSE_DELAY=-1;MODE=LEGACY
    driverClassName: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false

resilience4j:
  circuitbreaker:
    instances:
      swapi:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
  retry:
    instances:
      swapi:
        maxAttempts: 3
        waitDuration: 300ms
```

> Ajustá `security.jwt.secret` para entornos reales. Si no usás H2, remové `datasource`/`h2`.

---

## Cómo correr

```bash
# 1) Compilar
mvn clean package

# 2) Ejecutar
java -jar target/challenge-0.0.1-SNAPSHOT.jar
# La app queda en http://localhost:8080
```

**Docker (opcional):**
```bash
docker build -t swapi-challenge .
docker run -p 8080:8080 --name swapi swapi-challenge
```

---

## Autenticación y seguridad

- **Registro**: `POST /auth/register`  → cuerpo: `{"username":"...", "password":"..."}`
- **Login**: `POST /auth/login` → devuelve `{ token, expiresAtEpochMillis }`
- **Acceso**: enviar `Authorization: Bearer <token>` en todas las requests (incluido `/graphql`).
- **Protección**:
  - `/auth/**` y `/actuator/health` → **abiertos**
  - `/graphql` → **protegido**
- **Passwords**: BCrypt.  
- **Roles**: convención `ROLE_USER` (o `.roles("USER")` en el builder).

---

## API (GraphQL)

**Endpoint único:** `POST /graphql`

### Paginación y filtros
- Todas las operaciones de listado aceptan `page` (1-based), `size`, `name` y `id`.
- **Regla de filtros:** si viene `id`, se devuelve una “página” de 1 elemento (el detalle).  
  Si además viene `name`, se aplica **AND** (el nombre/título debe contener el texto, *case-insensitive*).
- **Films:** SWAPI filtra por **`title`**, la capa de servicio lo mapea desde el argumento `name` por consistencia.

### Ejemplos de queries

**People (paginado + name):**
```graphql
query ($page:Int,$size:Int,$name:String){
  people(page:$page, size:$size, name:$name){
    totalPages totalRecords
    results { id name }
  }
}
```
Variables:
```json
{ "page": 1, "size": 10, "name": "sky" }
```

**People (id + name = AND):**
```graphql
query ($id:ID,$name:String){
  people(id:$id, name:$name){
    totalRecords
    results { id name }
  }
}
```
Variables:
```json
{ "id": "1", "name": "luke" }
```

**Films (usa title como filtro en SWAPI):**
```graphql
query($page:Int,$size:Int,$name:String){
  films(page:$page,size:$size,name:$name){
    totalPages totalRecords
    results { id title releaseDate }
  }
}
```
Variables:
```json
{ "page": 1, "size": 10, "name": "hope" }
```

**Starships / Vehicles (análogos):**
```graphql
query($page:Int,$size:Int,$name:String,$id:ID){
  starships(page:$page,size:$size,name:$name,id:$id){
    totalPages totalRecords results { id name model }
  }
}
```

---

## Uso con Postman

1. **Importar** la colección: `postman/SWAPI_Challenge.postman_collection.json`  
   (o el archivo exportado junto al proyecto).
2. En la **colección**, pestaña **Variables**:
   - `base_url`: `http://localhost:8080` (o URL de tu deploy)
   - `username` / `password`: credenciales de prueba (p.ej. `demo`/`demo`)
3. **Authorization** (a nivel colección): *Bearer Token* con `{{token}}` (ya viene configurado).
4. **Pre-request Script** (a nivel colección):
   - hace **login** automático (si falla, intenta **register** y reintenta login),
   - guarda `{{token}}` y lo añade al header `Authorization`,
   - genera `X-Correlation-Id: {{cid}}`.
5. Ejecutar las requests en las carpetas **People / Films / Starships / Vehicle**.  
   Podés usar la **pestaña GraphQL** (query a la izquierda, **Variables** a la derecha).

> No hace falta tocar headers manuales: la colección los gestiona.

---

## Cache & Resiliencia

- **Caffeine**: cachea listados y detalles (TTL y tamaños configurables por tipo).
- **Resilience4j**:
  - `Retry (swapi)` ante fallos transitorios.
  - `CircuitBreaker (swapi)` para evitar cascada de errores si SWAPI falla.
- Beneficios: **menor latencia**, **menos llamadas** a SWAPI y **tolerancia a fallos**.

---

## Observabilidad

- **Actuator**: `GET /actuator/health`
- **Correlation ID**: enviar `X-Correlation-Id` (la colección Postman lo hace). Útil para trazar en logs.
- **Micrometer**: si añadís un registry (Prometheus, etc.), se exponen métricas de HTTP, cache y circuit breaker.

---

## Estructura de paquetes

```
com.swapi.challenge
├─ config/               # Cache (Caffeine), RestTemplate, Security, Resilience
├─ security/             # JWT, filtros, UserDetailsService
├─ swapi/                # Integración HTTP con SWAPI (servicio + DTOs/mapas)
│  └─ service/SwapiService.java
├─ graphql/
│  └─ query/             # Resolvers GraphQL (people/films/starships/vehicles)
└─ ...                   # controladores auth, modelos, etc.
```

---

## Tests

- **Unitarias**: lógica de mapeo y servicios.
- **Integración**: GraphQL + SWAPI stubeado con **WireMock** (`wiremock-jre8`).  
- Ejecutar:
```bash
mvn test
```

---

## Despliegue

- **Render / Fly.io / Heroku** (gratuitos):
  - setear envs: `security.jwt.secret`, `swapi.base-url` (si variara), etc.
  - publicar el jar y mapear puerto `8080`.
- Actualizar `base_url` en la **colección Postman** con la URL pública.

---

## Troubleshooting

- **401/403**: falta o expiró el token → reloguear (la colección lo hace).  
- **5xx/Service Unavailable**: CircuitBreaker abierto o SWAPI caído → revisar logs.  
- **Nada en `totalPages/totalRecords`**: recordar que SWAPI puede devolver `result` o `results`; el resolver ya lo contempla, pero verificá inputs de filtros.
