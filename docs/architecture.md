# Arquitectura — EventPass

Documento vivo con las decisiones de diseño relevantes del backend. Cada sección
está anclada a la User Story que la introdujo.

---

## Visión general — diagrama de componentes

EventPass es un backend Spring Boot 3.2 (Java 17) con arquitectura por capas
clásica (controller → service → repository → JPA) más dos subsistemas
transversales: **seguridad** (filtro JWT delante de la cadena de filtros de
Spring Security) y **pricing dinámico** (patrón Strategy aplicado en la capa de
servicio). La persistencia se delega en Spring Data JPA sobre H2 (perfiles
`dev` y `demo`) y, en producción, sobre cualquier base relacional compatible.

​```mermaid
flowchart LR
    Client["Cliente<br/>(SPA / Postman / Swagger UI)"]

    subgraph Security["Cadena de seguridad"]
        JwtFilter["JwtAuthenticationFilter<br/>(OncePerRequestFilter)"]
        FilterChain["SecurityFilterChain<br/>(stateless · CSRF off)"]
    end

    subgraph Controllers["Capa REST · controller/"]
        AuthC["AuthController<br/>/api/auth/**"]
        EventoC["EventoController<br/>/api/events/**"]
        TicketC["TicketController<br/>/api/tickets/**"]
        UsuarioC["UsuarioController<br/>/api/users/**"]
        EstadC["EstadisticasController<br/>/api/users/me/stats"]
        ResenaC["ResenaController · FavoritoController"]
    end

    subgraph Services["Capa de aplicación · service/"]
        EventoS["EventoService"]
        TicketS["TicketService"]
        UsuarioS["UsuarioService"]
        PwdS["PasswordRecoveryService"]
        ResenaS["ResenaService"]
    end

    subgraph Pricing["Subsistema pricing · pricing/<br/>(patrón Strategy)"]
        Ctx["PricingContext<br/>(Context)"]
        Strat["PricingStrategy<br/>(interface)"]
        Early["EarlyBirdPricing<br/>×1.00"]
        Reg["RegularPricing<br/>×1.25"]
        Last["LastMinutePricing<br/>×1.50"]
    end

    subgraph Repos["Capa de persistencia · repository/<br/>(Spring Data JPA)"]
        EventoR["EventoRepository"]
        TicketR["TicketRepository"]
        UsuarioR["UsuarioRepository"]
        TokenR["TokenRecuperacionRepository"]
    end

    subgraph Model["Dominio · model/"]
        EventoE["Evento<br/>@Version"]
        TicketE["Ticket"]
        UsuarioE["Usuario"]
        TokenE["TokenRecuperacion"]
    end

    DB[("Base de datos<br/>H2 / SQL")]

    Client -->|HTTP| JwtFilter
    JwtFilter --> FilterChain
    FilterChain --> AuthC & EventoC & TicketC & UsuarioC & EstadC & ResenaC

    AuthC --> UsuarioS & PwdS
    EventoC --> EventoS
    TicketC --> TicketS
    UsuarioC --> UsuarioS
    EstadC --> EventoS & TicketS
    ResenaC --> ResenaS

    EventoS --> EventoR
    EventoS --> Ctx
    TicketS --> TicketR & EventoR & UsuarioR
    TicketS --> Ctx
    UsuarioS --> UsuarioR
    PwdS --> UsuarioR & TokenR

    Ctx --> Strat
    Strat -.implementa.-> Early & Reg & Last

    EventoR --> EventoE
    TicketR --> TicketE
    UsuarioR --> UsuarioE
    TokenR --> TokenE

    EventoE --> DB
    TicketE --> DB
    UsuarioE --> DB
    TokenE --> DB

    classDef sec fill:#fde4e4,stroke:#b91c1c
    classDef pricing fill:#e8f0fe,stroke:#1d4ed8
    classDef repo fill:#ecfccb,stroke:#4d7c0f
    class JwtFilter,FilterChain sec
    class Ctx,Strat,Early,Reg,Last pricing
    class EventoR,TicketR,UsuarioR,TokenR repo
​```

**Lectura rápida.** Toda petición HTTP pasa primero por
`JwtAuthenticationFilter`, que rellena el `SecurityContext` si hay un Bearer
token válido. Después, `SecurityFilterChain` decide si la ruta requiere
autenticación o un rol concreto. Los controladores delegan en la capa de
servicio, que aplica las reglas de negocio (validaciones, autorización
contextual, transacciones) y persiste mediante repositorios Spring Data. El
cálculo de precio se desacopla en el subsistema `pricing/`, que los servicios
invocan exclusivamente a través de `PricingContext`.

---

## Pricing dinámico — patrón Strategy (US-17)

### Motivación

El precio que paga un asistente no puede ser un dato estático del evento:
queremos premiar la compra anticipada y recoger valor cuando la demanda es
alta. A la vez, queremos poder añadir políticas nuevas (descuentos por código
promocional, precios escalonados por bloque horario, etc.) sin tocar los
servicios que ya consumen el cálculo.

### Diseño

Aplicamos el patrón **Strategy** (GoF) en el paquete
`com.ProyectoProcesosSoftware.pricing`:

- **Strategy** — `PricingStrategy` (interface) define el contrato común
  `BigDecimal calcularPrecio(BigDecimal precioBase, int entradasVendidas, int aforoMaximo)`.
- **ConcreteStrategy** — `EarlyBirdPricing`, `RegularPricing` y
  `LastMinutePricing`. Cada una es un bean Spring con un `@Qualifier` propio.
- **Context** — `PricingContext` recibe las tres estrategias por
  constructor, decide cuál aplicar en cada llamada en función del porcentaje
  de ocupación del evento, y expone dos métodos a los clientes:
  `calcularPrecio(...)` y `nombreEstrategia(...)`.

Los servicios (`EventoService`, `TicketService`) **solo dependen de
`PricingContext`**: nunca instancian ni nombran las estrategias concretas.
Añadir una nueva política es crear un `PricingStrategy` más, registrarlo como
bean y enchufarlo en el contexto — sin tocar ningún servicio.

### Tabla de rangos y multiplicadores

| Estrategia | Rango de ocupación (`entradasVendidas / aforoMaximo`) | Multiplicador | Implementación |
|---|---|---|---|
| **EarlyBird**  | `[0 %, 50 %]` (y caso defensivo `aforoMaximo ≤ 0`) | `× 1.00` (sin recargo) | `EarlyBirdPricing`  |
| **Regular**    | `(50 %, 80 %]`                                      | `× 1.25` (+25 %)       | `RegularPricing`    |
| **LastMinute** | `(80 %, 100 %]`                                     | `× 1.50` (+50 %)       | `LastMinutePricing` |

Los multiplicadores se aplican con `BigDecimal.multiply(...)` y se redondean a
2 decimales mediante `RoundingMode.HALF_UP` para garantizar compatibilidad
monetaria con la pasarela de pago.

### Trazabilidad para el cliente

`PricingContext.nombreEstrategia(...)` devuelve la etiqueta textual
(`"EarlyBird" / "Regular" / "LastMinute"`) y se incluye en el DTO de
respuesta de ticket y de detalle de evento, para que la UI pueda explicar al
usuario por qué se le cobra un determinado precio sin acoplarse a la
implementación interna.

### Inmutabilidad histórica

`TicketService.comprarEntrada` fija el `precioFinal` en el ticket **en el
momento de la compra**. Si la estrategia activa cambia después (porque el
evento se llena), el precio ya pagado no se recalcula. Para los listados de
"Mis entradas" se devuelve la estrategia *vigente ahora* sólo a efectos
informativos.

### Referencias en el código

- Interfaz: `src/main/java/com/ProyectoProcesosSoftware/pricing/PricingStrategy.java`.
- Contexto: `src/main/java/com/ProyectoProcesosSoftware/pricing/PricingContext.java`.
- Estrategias: `EarlyBirdPricing.java`, `RegularPricing.java`, `LastMinutePricing.java`.
- Tests: `src/test/java/com/ProyectoProcesosSoftware/PricingStrategyTest.java`.

---

## Autenticación — JWT + SecurityFilterChain (US-09)

### Decisión

EventPass es un backend **stateless**: cada petición se autentica por sí
misma mediante un token **JWT firmado con HS256**. No se mantiene sesión en
servidor (`SessionCreationPolicy.STATELESS`) y CSRF se desactiva porque no
hay cookies de sesión que proteger.

### Flujo

​```
┌──────────┐   POST /api/auth/login (email + password)
│ Cliente  │ ─────────────────────────────────────────────┐
└────┬─────┘                                              ▼
     │                                          ┌──────────────────┐
     │                                          │  AuthController  │
     │                                          │   .login(...)    │
     │                                          └────────┬─────────┘
     │                                                   │
     │  ① BCrypt.matches(plain, hash)                    │
     │  ② JwtService.generarToken(userId, email, rol)    │
     │  ③ devuelve JwtResponseDTO { token, id, email, rol }
     │ ◀─────────────────────────────────────────────────┘
     │
     │  Petición posterior:
     │  GET /api/tickets/my   con   Authorization: Bearer <token>
     │ ─────────────────────────────────────────────┐
     ▼                                              ▼
┌──────────────────────────┐               ┌────────────────────┐
│ JwtAuthenticationFilter  │ ─ valida ──▶  │ SecurityContext    │
│ (OncePerRequestFilter)   │   y rellena   │ (principal=userId, │
└────────────┬─────────────┘               │  authority=ROLE_X) │
             │                             └────────────────────┘
             ▼
┌────────────────────────────┐
│   SecurityFilterChain      │  ─ aplica reglas authorizeHttpRequests
└────────────┬───────────────┘
             ▼
        Controller
​```

### Composición de la cadena

`SecurityConfig.filterChain(HttpSecurity)`:

- `csrf().disable()` y `sessionCreationPolicy(STATELESS)` — sin sesión, sin
  CSRF token.
- `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`
  — el filtro JWT se inserta **antes** del filtro de usuario/contraseña
  estándar, de modo que la autenticación por token ocurre antes de cualquier
  intento de form-login.
- `authorizeHttpRequests(...)` declara las reglas por endpoint. Las rutas
  públicas (recursos estáticos, `/api/auth/**`, `POST /api/users`,
  `GET /api/events/**`, Swagger UI, H2 console) van con `permitAll()`. Las
  rutas privadas exigen autenticación o rol concreto (`hasRole("ASISTENTE")`,
  `hasRole("ORGANIZADOR")`).
- `headers(frame -> disable())` — necesario sólo para que la consola H2 se
  pueda servir dentro de un iframe en entornos `dev`/`demo`.

### `JwtAuthenticationFilter`

Extiende `OncePerRequestFilter` y, en cada petición:

1. Lee la cabecera `Authorization`. Si no empieza por `"Bearer "`, deja
   pasar la petición sin autenticar (los endpoints `permitAll()` siguen
   funcionando; los privados serán rechazados después por la cadena).
2. Si hay token, lo entrega a `JwtService.validarToken(...)`. Si es
   válido, extrae `userId` y `rol` de las claims y monta un
   `UsernamePasswordAuthenticationToken` con:
   - **principal** = `userId` (no el email, para evitar mostrar PII en logs
     y para que los servicios trabajen con el `Long` que ya usan en BD).
   - **authorities** = `ROLE_<rol>` (compatible con `hasRole("X")` de Spring,
     que añade el prefijo `ROLE_` automáticamente).
3. Inyecta la autenticación en el `SecurityContextHolder` y delega en el
   siguiente filtro.

### `JwtService`

Construido sobre **jjwt 0.12**. Detalles relevantes:

- Clave HMAC simétrica generada con `Keys.hmacShaKeyFor(secretKey.getBytes(UTF_8))`.
- El **secreto** se inyecta desde `application-{profile}.properties` con
  `@Value("${jwt.secret}")`. En `prod` se sobrescribe vía variable de
  entorno (`${JWT_SECRET:fallback}`) para no dejarlo en el repositorio.
- La **expiración** es `${jwt.expiration}` milisegundos (`86 400 000` = 24 h
  por defecto en todos los perfiles).
- Claims del token: `sub = userId`, `email`, `rol`, `iat`, `exp`.
- `validarToken(...)` no lanza: captura `JwtException` y `IllegalArgumentException`
  y devuelve `false`, de modo que un token corrupto o caducado se trata
  como "no autenticado" y el endpoint privado responderá `401`/`403`
  conforme a las reglas de la cadena.

### Integración con Swagger / OpenAPI (T-19.8)

`OpenApiConfig` registra un `SecurityScheme` tipo `HTTP / bearer / JWT` y lo
añade como `SecurityRequirement` global. En la Swagger UI aparece el botón
**Authorize**, donde basta con pegar `Bearer <token>` para que todas las
peticiones interactivas adjunten la cabecera automáticamente.

### Referencias en el código

- `src/main/java/com/ProyectoProcesosSoftware/security/SecurityConfig.java`
- `src/main/java/com/ProyectoProcesosSoftware/security/JwtAuthenticationFilter.java`
- `src/main/java/com/ProyectoProcesosSoftware/security/JwtService.java`
- `src/main/java/com/ProyectoProcesosSoftware/controller/AuthController.java`
- `src/main/java/com/ProyectoProcesosSoftware/config/OpenApiConfig.java`

---

## Concurrencia en compras de entradas (US-18)

### Problema

Una compra de entrada en `TicketService.comprarEntrada` ejecuta esta secuencia:

1. Lee el `Evento` (incluyendo `entradasVendidas`).
2. Valida que `entradasVendidas < aforoMaximo`.
3. Incrementa `entradasVendidas` en memoria.
4. Guarda el evento.
5. Guarda el ticket.

Bajo carga concurrente, dos hilos pueden ejecutar los pasos 1–3 con el mismo
estado de partida (por ejemplo `entradasVendidas = 99` sobre un aforo de 100)
antes de que cualquiera persista. Ambos guardarían el evento con
`entradasVendidas = 100` y crearían sendos tickets: hemos vendido **101**
entradas para un aforo de 100. Es una "lost update" clásica.

### Solución elegida: bloqueo optimista (`@Version`)

`Evento` lleva un campo `@Version private Long version`. Hibernate añade un
`WHERE version = ?` al `UPDATE` y, si la fila ya fue actualizada por otro hilo,
el `UPDATE` no afecta filas y Spring lanza
`ObjectOptimisticLockingFailureException`.

`TicketService.comprarEntrada`:

- Hace `eventoRepository.saveAndFlush(evento)` para forzar el flush dentro del
  método (sin esto la excepción saltaría al final del `@Transactional`, fuera
  del try/catch).
- Captura `ObjectOptimisticLockingFailureException` y la traduce a
  `BusinessRuleException("La plaza acaba de ser ocupada por otro usuario, inténtalo de nuevo")`.
- `GlobalExceptionHandler` ya mapea `BusinessRuleException → 409 CONFLICT`.

Resultado: en una carrera por la última plaza, el primero recibe `201` y el
segundo recibe `409` con un mensaje accionable.

### Trade-off: optimista vs pesimista

| Estrategia | Pros | Contras |
|---|---|---|
| **Optimista (`@Version`)** | Sin locks de fila. Throughput alto cuando las colisiones son raras. Sencillo de implementar. | El cliente del segundo hilo debe reintentar manualmente. Mal encaje si las colisiones son la norma. |
| Pesimista (`SELECT ... FOR UPDATE`) | El segundo hilo se bloquea hasta que el primero termine y luego ve el estado actualizado (sin error). | Throughput cae con concurrencia alta. Riesgo de deadlocks. Requiere que la transacción dure poco. |

Para EventPass las colisiones reales son poco frecuentes (sólo ocurren cuando
varios usuarios pujan a la vez por las últimas plazas de un evento popular). El
coste de un reintento manual del usuario es asumible y el coste de implementación
es mínimo, así que **optimista** gana.

### Referencias en el código

- Entidad: `src/main/java/com/ProyectoProcesosSoftware/model/Evento.java` (campo `@Version`).
- Lógica de compra: `src/main/java/com/ProyectoProcesosSoftware/service/TicketService.java#comprarEntrada`.
- Test unitario: `src/test/java/com/ProyectoProcesosSoftware/service/TicketServiceTest.java#comprar_colisionConcurrencia_lanzaBusinessRuleException`.
- Test de integración: `src/integrationTest/java/com/ProyectoProcesosSoftware/integration/TicketConcurrencyIT.java` (20 hilos contra 19 plazas) — pendiente en T-18.4.