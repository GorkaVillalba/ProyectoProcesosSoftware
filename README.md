# EventPass - Plataforma de Eventos y Entradas
![CI Pipeline](https://github.com/GorkaVillalba/ProyectoProcesosSoftware/actions/workflows/ci.yml/badge.svg)

## Descripción
EventPass es una plataforma web que permite a organizadores crear y gestionar eventos,
y a asistentes comprar entradas con precio dinámico según ocupación.

## Stack Tecnológico
- **Backend:** Java 17 + Spring Boot 3.2
- **Persistencia:** JPA/Hibernate + MySQL (prod) / H2 (dev)
- **Seguridad:** Spring Security + JWT
- **Testing:** JUnit 5 + Mockito
- **CI/CD:** GitHub Actions
- **Contenedores:** Docker + Docker Compose

## Requisitos
- Java 17+
- Gradle 8+
- Docker y Docker Compose (para producción)

## Ejecución Local (H2)
```bash
./gradlew bootRun
```
La app arranca en http://localhost:8080
Consola H2: http://localhost:8080/h2-console

## Ejecución con Docker (MySQL)
```bash
docker-compose up --build
```
## Tests y cobertura

Ejecutar tests unitarios:
```bash
./gradlew test
```

Generar el reporte de cobertura JaCoCo (HTML + XML):
```bash
./gradlew test jacocoTestReport
```
- HTML: `build/reports/jacoco/test/html/index.html`
- XML:  `build/reports/jacoco/test/jacocoTestReport.xml`

Verificar el umbral mínimo de cobertura (85% de líneas sobre packages de negocio):
```bash
./gradlew jacocoTestCoverageVerification
```

Se excluyen del cálculo: `config`, `dto`, `exception`, `security`, `model` y la clase `ProyectoApplication`.


## Endpoints Principales
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | /api/users | Registro |
| POST | /api/auth/login | Login (JWT) |
| GET | /api/users/{id} | Ver perfil |
| PUT | /api/users/{id} | Editar perfil |
| DELETE | /api/users/{id} | Dar de baja |
| POST | /api/events | Crear evento |
| GET | /api/events | Listar eventos |
| GET | /api/events/{id} | Detalle evento |
| PUT | /api/events/{id} | Editar evento |
| DELETE | /api/events/{id} | Eliminar evento |

## Equipo SCRUM
| Rol | Persona |
|-----|---------|
| Product Owner | [MikelOyarzabal] |
| Scrum Master | [Alvaroogaarcia] |
| Desarrolladores | [Asiersanchez10] |
| Desarrolladores | [imZesk] |
| Desarrolladores | [jukossound] |
| Desarrolladores | [Benat27] |
| Desarrolladores | [GorkaVillalba] |

