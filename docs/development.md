# Guía de desarrollo local — EventPass

Esta guía recoge todo lo que necesitas para clonar el proyecto, compilarlo,
ejecutarlo, probarlo y ver la cobertura en tu máquina, sin depender de Docker
ni de la base de datos de producción.

Para una descripción de la arquitectura y de las decisiones de diseño, consulta
[`architecture.md`](architecture.md). Para la visión general del producto y
los enlaces a CI, Sonar y Javadoc publicado, consulta el `README.md` en la
raíz del repositorio.

---

## 1 · Prerrequisitos

| Herramienta | Versión recomendada | Cómo comprobarla | Notas |
|---|---|---|---|
| **JDK** | 17 (LTS) | `java -version` y `javac -version` | El proyecto declara `sourceCompatibility = '17'`. Cualquier JDK ≥ 17 vale; con JDK 11 no compila. |
| **Gradle Wrapper** | el que ya viene en el repo (`./gradlew`) | `./gradlew --version` | No es necesario instalar Gradle a mano: usa siempre el wrapper. |
| **Git** | reciente | `git --version` | |
| **Docker + Docker Compose** *(opcional)* | Docker Desktop arrancado | `docker --version` | Solo necesario para levantar el stack con MySQL (perfil `prod`). Para el día a día de desarrollo basta con H2 en memoria. |
| **IDE** *(opcional)* | IntelliJ IDEA / VS Code con extensión Java | — | Cualquier IDE Java moderno detecta el `build.gradle` directamente. |

> **Windows + OneDrive:** si clonas el repo dentro de una carpeta sincronizada
> con OneDrive es habitual que Gradle falle al limpiar `build/` con
> `IOException: Unable to delete directory`. Para evitarlo, clónalo fuera de
> OneDrive (p. ej. `C:\dev\ProyectoProcesosSoftware`) o pausa la sincronización
> mientras compilas.

### Clonado

```bash
git clone https://github.com/GorkaVillalba/ProyectoProcesosSoftware.git
cd ProyectoProcesosSoftware
```

En Windows usa `gradlew.bat` en todos los comandos siguientes en lugar de
`./gradlew`.

---

## 2 · Comandos Gradle

Todos los comandos se ejecutan desde la raíz del proyecto. El wrapper se
encarga de descargar la distribución correcta de Gradle la primera vez.

### Comandos básicos

| Comando | Qué hace |
|---|---|
| `./gradlew bootRun` | Arranca la aplicación Spring Boot en `http://localhost:8080` con el perfil por defecto (`dev`). |
| `./gradlew build` | Compila, ejecuta tests unitarios, genera el report Jacoco y empaqueta el JAR en `build/libs/`. |
| `./gradlew clean` | Borra el directorio `build/`. Útil si Gradle muestra resultados raros tras un cambio grande. |
| `./gradlew tasks` | Lista todas las tareas disponibles agrupadas por categoría. |

### Tests

El `build.gradle` separa los tests en tres categorías (T-24) para que la suite
unitaria sea rápida y el resto se pueda lanzar bajo demanda:

| Comando | Qué ejecuta | Patrón de fichero | Fuente |
|---|---|---|---|
| `./gradlew test` | Tests unitarios (rápidos, sin Spring). Finaliza generando el report Jacoco. | `**/*Test.class` | `src/test/java/` |
| `./gradlew integrationTest` | Tests de integración con Spring (más lentos, levantan contexto). Arrancan con `spring.profiles.active=dev`. | `**/*IT.class` | `src/integrationTest/java/` |
| `./gradlew performanceTest` | Tests de rendimiento y concurrencia (los más lentos). | `**/*PerfTest.class` | `src/performanceTest/java/` |

Lanzar un test concreto:

```bash
./gradlew test --tests "com.ProyectoProcesosSoftware.service.TicketServiceTest"
./gradlew test --tests "*TicketService*.comprar_*"
```

### Documentación y análisis

| Comando | Qué genera | Dónde queda |
|---|---|---|
| `./gradlew javadoc` | Javadoc del proyecto (UTF-8, sin lint estricto). | `build/docs/javadoc/index.html` |
| `./gradlew jacocoTestReport` | Report HTML + XML de cobertura. Se ejecuta automáticamente al final de `./gradlew test`. | `build/reports/jacoco/test/html/index.html` |
| `./gradlew jacocoTestCoverageVerification` | Falla si la cobertura de líneas baja del **90 %**. | — |
| `./gradlew sonar` | Análisis SonarCloud (requiere `SONAR_TOKEN` configurado). Depende de `test` y `jacocoTestReport`. | Publica en sonarcloud.io |
| `./gradlew generateOpenApiDocs` | Levanta la app, recoge el OpenAPI y lo guarda en disco. | `build/openapi.json` |

---

## 3 · Perfiles Spring

La aplicación define tres perfiles en `src/main/resources/`. El activo por
defecto es **`dev`** (lo fija `application.properties`).

| Perfil | Fichero | Base de datos | DDL | Datos iniciales | JWT secret | Pensado para |
|---|---|---|---|---|---|---|
| **`dev`** *(por defecto)* | `application-dev.properties` | H2 en memoria `jdbc:h2:mem:testdb` | `create-drop` | — (vacía al arrancar) | hard-coded (256 bits) | desarrollo local cotidiano |
| **`demo`** | `application-demo.properties` | H2 en memoria `jdbc:h2:mem:eventpass-demo` | `create-drop` | `data-demo.sql` (cargado en arranque) | hard-coded `demo-secret-...` | demos y prueba de UI con datos plausibles |
| **`prod`** | `application-prod.properties` | MySQL `jdbc:mysql://localhost:3306/eventpass` | `update` | — | variable de entorno `JWT_SECRET` (con fallback) | despliegue real (Docker / servidor) |

### Cómo activar un perfil

Cualquiera de estas tres formas vale; elige según convenga:

```bash
# 1) Argumento Spring Boot al arrancar
./gradlew bootRun --args='--spring.profiles.active=demo'

# 2) Variable de entorno (bash / zsh)
SPRING_PROFILES_ACTIVE=demo ./gradlew bootRun

# 2-bis) Variable de entorno (PowerShell)
$env:SPRING_PROFILES_ACTIVE="demo"; ./gradlew bootRun

# 3) Propiedad de sistema JVM
./gradlew bootRun -Dspring.profiles.active=demo
```

Si arrancas con el perfil `prod` necesitas además exportar:

```bash
export DB_USERNAME=root           # opcional, por defecto "root"
export DB_PASSWORD=mi_password    # opcional, por defecto "root"
export JWT_SECRET=$(openssl rand -hex 64)
```

Sin estas variables, `prod` funcionará con los fallbacks del fichero, pero
**no son seguros** para entornos públicos.

---

## 4 · Acceso a la consola H2

H2 console está habilitada en los perfiles `dev` y `demo`
(`spring.h2.console.enabled=true`) y `SecurityConfig` la deja accesible sin
autenticación (`/h2-console/**` con `permitAll()` y `frame options` deshabilitado
para que el iframe interno funcione). En el perfil `prod` está **deshabilitada**
porque la base de datos no es H2.

### Pasos

1. Arranca la app con `dev` o `demo`:
```bash
   ./gradlew bootRun                                # perfil dev
   ./gradlew bootRun --args='--spring.profiles.active=demo'
```
2. Abre en el navegador:
```
   http://localhost:8080/h2-console
```
3. Rellena el formulario de conexión:

   | Campo | Perfil `dev` | Perfil `demo` |
   |---|---|---|
   | **Driver class** | `org.h2.Driver` *(auto)* | `org.h2.Driver` *(auto)* |
   | **JDBC URL** | `jdbc:h2:mem:testdb` | `jdbc:h2:mem:eventpass-demo` |
   | **User Name** | `sa` | `sa` |
   | **Password** | *(vacía)* | *(vacía)* |

4. Pulsa **Connect**. Verás las tablas `USUARIO`, `EVENTO`, `TICKET`,
   `TOKEN_RECUPERACION`, etc. que JPA crea al arrancar.

> Como las URLs son `jdbc:h2:mem:*`, la base de datos **se pierde al parar la
> app**. Esto es deliberado para desarrollo. En `demo` se vuelve a cargar
> `data-demo.sql` en cada arranque.

---

## 5 · Cómo ver la cobertura

El proyecto usa **JaCoCo 0.8.11** y publica los resultados también en
SonarCloud (a través del XML generado).

### Generar y abrir el report

```bash
./gradlew test                 # ./gradlew test ya invoca jacocoTestReport
# o, explícitamente:
./gradlew jacocoTestReport
```

Después abre en el navegador:

```
build/reports/jacoco/test/html/index.html
```

Verás métricas por paquete y por clase con desglose por instrucciones,
ramas, líneas, métodos y complejidad ciclomática.

### Qué se excluye del cálculo

El `build.gradle` excluye de la cobertura los paquetes/clases sin lógica
significativa (consistente con la configuración de SonarCloud):

- `ProyectoApplication.*`
- `config/**`
- `dto/**`
- `exception/**`
- `security/**`
- `model/**`

Estas exclusiones también se aplican a `jacocoTestCoverageVerification`.

### Comprobar el umbral del 90 %

```bash
./gradlew jacocoTestCoverageVerification
```

Falla la build si la cobertura de líneas del bundle (paquetes no excluidos)
baja del **90 %**. Útil para detectar regresiones antes de abrir un PR.

### Cobertura en SonarCloud

El report XML que consume Sonar está en
`build/reports/jacoco/test/jacocoTestReport.xml`. La tarea `./gradlew sonar`
lo sube a SonarCloud, donde puedes consultar tendencias y "new code
coverage" por PR:

<https://sonarcloud.io/summary/new_code?id=GorkaVillalba_ProyectoProcesosSoftware>

---

## 6 · Atajos útiles

| Tarea | Comando |
|---|---|
| Arrancar la app | `./gradlew bootRun` |
| Build completo + tests + cobertura | `./gradlew build` |
| Solo tests unitarios (rápido) | `./gradlew test` |
| Tests de integración | `./gradlew integrationTest` |
| Tests de rendimiento | `./gradlew performanceTest` |
| Generar Javadoc | `./gradlew javadoc` |
| Abrir Swagger UI (app arrancada) | <http://localhost:8080/swagger-ui/index.html> |
| Abrir consola H2 (app arrancada) | <http://localhost:8080/h2-console> |
| Abrir cobertura local (tras `./gradlew test`) | `build/reports/jacoco/test/html/index.html` |
| Limpiar build | `./gradlew clean` |
| Parar daemons de Gradle | `./gradlew --stop` |