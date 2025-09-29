# Plan de Pruebas de Carga (AJUSTADO a Pruebas de Humo) – ANB Rising Stars Showcase

**Versión:** 2025-09-29 04:20  
**Objetivo:** Reducir la desviación entre lo planificado y lo observado en humo, y preparar el terreno para cargas progresivas estables.

## 1. Entorno e infraestructura
- **Generador de carga (local):** i7‑12700H, 16 GB RAM, Windows 11 Pro 22H2.
- **Topología de la solución (AWS):** Front (t3.small), Back (t3.small), Worker (t3.large), RabbitMQ (t3.small),
  Redis‑EC2 (t3.small, sin IP pública), MinIO (t3.small), Bastion (t3.micro).
- **Observación metodológica:** El uso de cliente local puede sesgar los resultados (CPU/IO/red del cliente).
  Para pruebas de confirmación se recomienda un cliente dedicado EC2 (p. ej., c6i.xlarge).

## 2. Métricas y criterios (conservadores tras humo)
- **Web (interactivo):** foco en **p95** por endpoint; objetivo inicial ≤ 1000 ms.
- **Upload (ingestión multipart):** objetivo inicial **p95 ≤ 5 s** (1200 ms era el deseable; se mantiene como meta a mediano plazo).
- **Tasa de éxito:** ≥ 95% por paso en ventanas de 10 s.
- **RPS / Concurrencia:** escalar por etapas manteniendo estabilidad (sin queues crecientes ni spikes de p95).

## 3. Escenarios AJUSTADOS (etapas y duraciones)
### 3.1 TG‑Interactivo (login → listar → votar×3 → ranking)
- **Etapa A:** rate(**1/seg**) → random_arrivals(**2 min**)
- **Etapa B:** rate(**2/seg**) → random_arrivals(**3 min**)
- **Etapa C:** rate(**4/seg**) → random_arrivals(**4 min**)
- **Etapa D:** rate(**6/seg**) → random_arrivals(**3 min**)
- **Etapa E (descenso):** rate(**3/seg**) → random_arrivals(**2 min**)

> Justificación: las pruebas de humo y los resultados observados bajo carga mostraron degradación en pasos de voto y p95 altos.
> Este escalado conservador busca aislar cuellos de la API/DB conservando éxito≥95% en cada etapa.

### 3.2 TG‑Upload (login → upload multipart 30–100 MB)
- **Etapa A:** rate(**0.2/seg**) → random_arrivals(**3 min**)  *(12 req/min)*
- **Etapa B:** rate(**0.5/seg**) → random_arrivals(**5 min**)  *(30 req/min)*
- **Etapa C:** rate(**0.8/seg**) → random_arrivals(**5 min**)  *(48 req/min)*
- **Etapa D:** rate(**1.0/seg**) → random_arrivals(**3 min**)  *(60 req/min)*
- **Etapa E (descenso):** rate(**0.5/seg**) → random_arrivals(**3 min**)

> Justificación: en humo y carga se observaron p95 muy elevados en upload. Estas etapas priorizan estabilidad y éxito≥95%.
> Si se cumplen, el siguiente ciclo subirá a 1.2–1.5 rps con confirmación en EC2.

## 4. Recolección de evidencias
- **Ventanas:** agregación cada 10 s (RPS, p95, éxito%).
- **Condición de validez por ventana:** n≥20 (Upload) / n≥50 (Interactivo).
- **Artefactos:** CSV crudos, CSV agregados, dashboard HTML y este plan.

## 5. Criterios de avance
- Promocionar a plan **“intermedio”** si: éxito≥95% y p95 en metas por ≥80% de ventanas válidas. Mantener si no.
