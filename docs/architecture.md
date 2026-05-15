# Arquitectura — EventPass

Documento vivo con las decisiones de diseño relevantes del backend. Cada sección
está anclada a la User Story que la introdujo.

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