# ANB Rising Stars Showcase — Plan de Pruebas de Carga y Análisis de Capacidad

> **Versión:** 1.0 — 2025-09-07  
> **Proyecto:** ANB Rising Stars Showcase  
> **Responsable:** Equipo QA/DevOps  
> **Documento:** `plan_de_pruebas.md`

---

## 1. Lugar y formato de entrega
- **Repositorio:** `./capacity-planning/plan_de_pruebas.md`  
- **Colecciones Postman:** `./collections/…` (con ejecución por `newman`)  
- **Evidencias y artefactos:** `./capacity-planning/evidencias/` (gráficos, reportes HTML/CSV, perfiles de CPU/memoria)  
- **Notas:** mantener estructura consistente para entregas futuras; adjuntar *readme* corto con pasos de reproducción.

> Este formato sigue las directrices de la entrega del curso y consolida el análisis de capacidad, escenarios, métricas y recomendaciones.

---

## 2. Análisis de capacidad
**Objetivo:** validar que la plataforma soporte de forma confiable la concurrencia de usuarios para:  
1) flujo interactivo de autenticación, listado y votación; 2) flujo intensivo de *upload* y procesamiento asíncrono de videos (estandarización 30 s, 720p, 16:9, sin audio, marca de agua).  
El análisis considera **latencia**, **throughput**, **utilización** (CPU, memoria, disco, I/O), **errores** y **estabilidad** bajo distintos perfiles de carga.

**Supuestos de tráfico (hipótesis para la primera iteración):**
- Hora pico con picos de **200–300 usuarios concurrentes** navegando y votando.
- Jornadas de subida masiva en ventanas cortas (p. ej., **100 jugadores** subiendo en paralelo).
- Ratio *mix* aproximado en hora pico: 60% `GET /api/public/videos`, 20% `POST /api/public/videos/{id}/vote`, 15% `GET /api/public/rankings`, 5% autenticación.
- *Workers* de procesamiento con cola dedicada y *backoff* de reintentos; tareas idempotentes.

**Riesgos/Cuellos de botella potenciales:**
- **I/O de archivos** en *upload* y en *transcoding*.  
- **CPU** en *workers* de video (ffmpeg) y en DB por validaciones de voto único.  
- **Contención** en almacenamiento (disco local/volumen Docker) y en *broker* (Redis/RabbitMQ).  
- **N+1/consultas costosas** en listados y rankings si no hay *caching* / vistas materializadas.

---

## 3. Definición de métricas
**Capacidad de procesamiento**
- *Throughput* (req/min o req/s) por endpoint crítico: `GET /api/public/videos`, `POST /api/public/videos/{id}/vote`, `POST /api/videos/upload`.
- *Jobs/min* procesados por *workers* (pipeline de video).

**Tiempo de respuesta**
- Percentiles **p50/p90/p95/p99** por endpoint (objetivo principal: **p95**).

**Utilización y salud**
- **CPU** y **RAM** por servicio (API, *workers*, DB, broker).  
- **I/O** (disco y red), *file descriptors*, *goroutines*, *GC* (Go).  
- **Errores** (4xx/5xx), *timeouts*, reintentos, *dead-letter queue*.  
- **Cola**: tamaño, *age*, *throughput*, *latency* de *broker*.

---

## 4. Respuestas a las preguntas orientadoras
- **¿Cuál es la carga objetivo?** Soportar **200 usuarios concurrentes** con holgura y picos de **300** para lectura/voto; y **100 uploads** paralelos ocasionales.  
- **¿Dónde está el cuello de botella?** Probable en *workers* (CPU/ffmpeg) y DB (unicidad de voto; ranking si no se cachea).  
- **¿Qué pasa al escalar?** API y *workers* escalan horizontalmente; DB necesita índices adecuados y *pool* de conexiones; *broker* requiere *tuning*.  
- **¿Qué degradación aceptamos?** Hasta **p95 < 500 ms** en lectura/voto a 200 CCU y **p95 < 900 ms** a 300 CCU; *upload* con **p95 < 1.2 s** (excluyendo procesamiento asíncrono).  
- **¿Qué proteger?** Idempotencia de `vote`, *rate-limit*, *circuit breakers*, caché para `rankings`, *DLQ* y *retry* con *backoff*.

---

## 5. Consideraciones importantes
- Validar **idempotencia** en `POST /api/public/videos/{id}/vote` (una vez por usuario por video).  
- **Caching** del ranking (TTL 1–5 min) o **vista materializada** para evitar *hot paths* en DB.  
- *Uploads* directos a almacenamiento con *signed URLs* y *scan* de tipo/tamaño.  
- *Workers* con **concurrencia limitada** según núcleos/CPU y *cgroups*; colas separadas para *upload* y *transcoding*.  
- Seguridad: JWT con expiración corta; proteger endpoints privados tras *proxy* Nginx.  
- Observabilidad desde el primer día.

---

## 6. Herramienta de pruebas de carga
**Elección:** **Locust** (Python).  
**Justificación:**
- Modela **flujos de usuario** (login → listar → votar) con estados y *wait times* realistas.
- Fácil manejo de **tokens JWT**, **variables** y **parametrización** (IDs de videos, ciudades).
- Métricas por tarea y percentiles; integración con **Prometheus** *pushgateway*.
- Escalable: *master/workers* distribuidos para mayores volúmenes.  
Alternativas: JMeter (GUI y ecosistema), Gatling (alto rendimiento), ab (simple, no JWT).

---

## 7. Entorno de pruebas
**Restricción de la entrega:** sin nube pública.  
**Topología recomendada (VirtualBox/VMs locales):**
- **Generador de carga (Locust):** Ubuntu 24.04, 4 vCPU, 8 GB RAM.  
- **Servidor aplicación (Docker Compose):** Ubuntu 24.04, 8 vCPU, 16 GB RAM.  
  - Servicios: API (Go + Gin/Echo), Nginx (reverse proxy), Redis/RabbitMQ (broker), *workers* (ffmpeg), PostgreSQL.  
- **Red:** puenteado entre VMs (1 Gbps virtual); latencia ~0.3–1 ms.

> Para extrapolar a nube, equivalentes aproximados: *m5.large* para API y *c6i.2xlarge* para *workers*. (Sólo referencia; no usar en esta fase).

---

## 8. Criterios de aceptación (ANB)
- **Voto:** `POST /api/public/videos/{id}/vote` → **p95 < 300 ms**, tasa de error < 0.1%.  
- **Listado público:** `GET /api/public/videos` → **p95 < 350 ms** a 300 CCU.  
- **Ranking:** `GET /api/public/rankings` → **p95 < 400 ms** con caché activa.  
- **Upload:** `POST /api/videos/upload` → **≥ 50 req/min** sostenidos; **p95 < 1.2 s** (solo ingestión).  
- **Utilización servidor app:** CPU **< 80%** sostenida a **200 CCU**.  
- **Workers de video:** tiempo medio de *transcoding* **≤ 30 s** por clip (30 s a 720p) con 4 *workers* paralelos.  
- **Integridad de voto único:** 0 violaciones en pruebas masivas.

---

## 9. Escenarios de prueba (rutas críticas)

### Escenario 1 — Interactivo/Web (login → explorar → votar)
**Objetivo:** validar UX y latencias en navegación pública y voto único.
**Flujo:**
1. `POST /api/auth/login` (JWT)  
2. `GET /api/public/videos?limit=…`  
3. Seleccionar **3 videos distintos** aleatorios.  
4. `POST /api/public/videos/{id}/vote` ×3 (una vez por cada video).  
5. `GET /api/public/rankings?city={ciudad}`  

```mermaid
flowchart TD
  A[Login (POST /auth/login)] --> B[Listar públicos (GET /public/videos)]
  B --> C[Votar Video 1 (POST /public/videos/<built-in function id>/vote)]
  C --> D[Votar Video 2]
  D --> E[Votar Video 3]
  E --> F[Ranking por ciudad (GET /public/rankings?city=…)]
```

**Validaciones:** códigos 200, unicidad de voto, p95 por paso, *think time* 1–3 s.

---

### Escenario 2 — Carga/Asíncrono (subidas concurrentes)
**Objetivo:** someter *upload* y *pipeline* asíncrono a concurrencia alta.
**Flujo:**
1. `POST /api/auth/login` por cada jugador.  
2. `POST /api/videos/upload` (20–60 s, ≤100 MB).  
3. Verificar respuesta 201 y `task_id`.  
4. *Opcional:* *polling* con `GET /api/videos/{video_id}` (hasta *status* `processed`).

```mermaid
flowchart TD
  A[Login por jugador] --> B[Upload de video (POST /videos/upload)]
  B --> C[Tarea en cola (broker)]
  C --> D[Worker procesa (ffmpeg)]
  D --> E[Status=processed y URL disponible]
```

**Carga:** **100 jugadores** subiendo en paralelo; repetir en lotes.  
**Validaciones:** tasa de éxito ≥ 99.5%, latencias de ingestión, *queue depth*, tiempos de *transcoding*.

---

## 10. Estrategia y configuración de pruebas
**Etapas:**  
1. **Humo (5–10 usuarios, 5 min):** validar scripts, datos y *smoke* de endpoints.  
2. **Carga progresiva (ramp-up):** 10 → 50 → 100 → 200 → **300 CCU**, 10–15 min por escalón.  
3. **Estrés:** incrementar hasta que **p95 > 1 s** o errores > 1%; registrar punto de inflexión.  
4. **Soak (estabilidad, 60–120 min a 200 CCU):** detectar *leaks* y degradación.  

**Parámetros de configuración (Locust):**
- `spawn_rate`: proporcional al escalón (p. ej., 10 usuarios/s al inicio).  
- *Think time* 1–3 s en escenario interactivo; sin *think time* en *upload* (para *worst case*).  
- Datos: *fixtures* de usuarios y catálogo de videos/ciudades (CSV/JSON).

**Monitoreo y *profiling*:**  
- **Grafana + Prometheus** (node_exporter, postgres_exporter, redis_exporter).  
- **cAdvisor** para contenedores; **Asynq/worker dashboard**.  
- *Logs* centralizados (Loki/ELK).  
- *pprof* en API Go (CPU/mem/perf), *pg_stat_statements* en PostgreSQL.

---

## 11. Tabla resumen (escenarios & resultados esperados)

| Escenario | Objetivo | Resultado esperado |
|---|---|---|
| Interactivo/Web | Latencia y unicidad de voto con 200–300 CCU | p95: login < 250 ms; listar < 350 ms; voto < 300 ms; ranking < 400 ms; errores < 0.1% |
| Carga/Asíncrono | Sostenibilidad de *upload* + *pipeline* | ≥ 50 req/min en *upload*; éxito ≥ 99.5%; *transcoding* medio ≤ 30 s/job con 4 *workers* |

---

## 12. Ejemplos de gráficos (resultados preliminares **simulados**)

**Throughput vs. Usuarios**  
![Throughput vs Usuarios](/mnt/data/throughput_vs_users.png)

**Tiempo de Respuesta p95 vs. Carga**  
![p95 vs Carga](/mnt/data/tiempo_respuesta_vs_carga.png)

---

## 13. Anexos
- *Especificación funcional del proyecto, endpoints y restricciones de infraestructura de la entrega.*
- *Colecciones de Postman y scripts de Locust (workload mix y escenarios).*

**Notas finales:** Este plan será iterado con resultados reales para refinar límites de capacidad, ajustar índices y *pooling*, y dimensionar *workers* de video y almacenamiento.

