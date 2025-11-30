--- START OF FILE entrega5.md ---

# Arquitectura para implementación con ECS, Autoscaling y Colas SQS
NOTA IMPORTANTE: la rama principal de esta entrega 5 se encuentra en el repositorio bajo la nomenclatura de migración a ECS: https://github.com/tatoand97/Sistema-de-Videos-y-Ranking/tree/feature/migration-to-ecs

Los cambios entre la entrega 4 y la entrega 5 representan un cambio fundamental en la estrategia de orquestación. Se abandona la complejidad de Kubernetes (EKS) en favor de Amazon ECS (Elastic Container Service) para simplificar la operación y mejorar la agilidad del escalado, manteniendo la arquitectura asíncrona basada en SQS.

A continuación se realiza una exposición detallada de los cambios, los componentes y el impacto medido en el rendimiento.

## Pruebas de Carga

Para ver el detalle técnico de los resultados y métricas:
[Entrega 5-pruebas](../../capacity-planning/DocPruebasEntrega5/pruebas_de_carga_entrega5.md)

## Componentes por instancias, base de datos y aplicación
![Figura 1 — Comparación de arquitecturas EKS vs ECS](imgs/Entrega5VsEntrega4.png "Figura 1. Comparación de arquitecturas EKS vs ECS")

> **Nota de arquitectura:** El diagrama compara la evolución de la orquestación.
> *   **Izquierda (Entrega 4 - EKS):** Cluster Kubernetes con Control Plane, Nodos EC2 gestionados, Pods, Ingress Controllers y servicios K8s complejos.
> *   **Derecha (Entrega 5 - ECS):** Cluster ECS simplificado. Task Definitions y Services desplegados en instancias EC2 dentro de un Auto Scaling Group. El ALB conecta directamente a las Tasks. Los Workers consumen de SQS de forma nativa.

# Qué muestra cada lado

**Entrega 4 — S3 + EKS + SQS**

*   **Alta complejidad de orquestación:** Cluster Kubernetes con *Control Plane* gestionado. Requiere configuración de Ingress Controllers (NGINX/ALB Ingress), Service Accounts (IRSA) y gestión de versiones de K8s.
*   **Escalamiento en dos capas:** Horizontal Pod Autoscaler (HPA) para los pods y Cluster Autoscaler/Karpenter para los nodos subyacentes.
*   **Gestión de red:** Overlay networks (CNI), Network Policies y mayor overhead en la configuración de seguridad.

**Entrega 5 — S3 + ECS + SQS**

*   **Orquestación simplificada:** Uso de **Amazon ECS**. Los contenedores corren como **Tasks** agrupadas en **Services**.
*   **Integración nativa:** El ALB enruta tráfico directamente a las Tasks sin necesidad de Ingress Controllers intermedios.
*   **Escalamiento reactivo:** Uso de **Service Auto Scaling** basado en métricas directas (CPU, Memoria) y, crucialmente, en la **profundidad de la cola SQS** para los workers, reduciendo la latencia de arranque.
*   **Menor overhead:** Se elimina la gestión del *Control Plane* de Kubernetes; el enfoque se centra en la definición de la tarea y los límites de recursos.

---

# Comparación punto a punto

| Tema | Entrega 4 (EKS + SQS) | Entrega 5 (ECS + SQS) |
| :--- | :--- | :--- |
| **Complejidad Operativa** | **Alta**: Requiere gestión de manifiestos K8s, Helm charts, upgrades de clúster y addons. | **Media/Baja**: Definiciones de Tareas (JSON) y Servicios. Menos piezas móviles que administrar y mantener. |
| **Curva de Aprendizaje** | Abrupta para equipos sin experiencia previa en Kubernetes. | Moderada; conceptos más cercanos a la infraestructura tradicional de AWS. |
| **Costo (Control Plane)** | Pago por hora por el clúster EKS ($0.10/h aprox) + recursos de nodos. | **Sin costo fijo** de orquestación (gratuito); solo se paga por los recursos de cómputo (EC2/Fargate). |
| **Latencia / Performance** | **P95 elevado bajo carga**. Dificultad para ajustar HPA y Cluster Autoscaler rápidamente ante picos agresivos. | **Mejora sustancial**. Reducción del **~59%** en latencia p95 para tráfico interactivo y **~79%** para uploads. Mejor absorción de picos. |
| **Tasa de Éxito** | Baja en escenarios de estrés (~11% interactivo, ~35% upload) por saturación de pods/nodos. | **Mayor estabilidad**. Aumento de tasa de éxito (>130% de mejora en interactivo), aunque limitada por lógica de negocio. |
| **Observabilidad** | Stack complejo (Prometheus, Grafana, o Container Insights con configuración extra). | Integración inmediata con **CloudWatch Container Insights** y Logs. |
| **Escalamiento Workers** | KEDA o scripts custom para escalar pods basados en SQS. | **Target Tracking Scaling** nativo de ECS basado en métricas de CloudWatch (SQS Queue Length). |


# Flujo

**Entrega 4 (EKS)**

1.  Usuario → ALB → Ingress Controller (Pod) → Service K8s → Pod Aplicación.
2.  Pod Aplicación → SQS.
3.  HPA detecta CPU alta → Escala Pods. Si faltan recursos → Cluster Autoscaler pide nodo EC2 (lento).
4.  Pod Worker consume SQS.

**Entrega 5 (ECS)**

1.  Usuario → ALB → **ECS Service** (Target Group) → **Task** (Contenedor).
2.  Task publica mensaje en SQS y responde inmediatamente.
3.  **Alarmas CloudWatch** (basadas en CPU o largo de cola SQS) disparan políticas de **Auto Scaling**.
4.  ECS lanza nuevas **Tasks de Worker** rápidamente (menor "cold start" de infraestructura) para vaciar la cola, procesando videos y actualizando RDS/S3.

# Implicaciones prácticas (lo que gana/pierde tu equipo)

*   **Mejora drástica en Latencia y Estabilidad:** Como evidencian las pruebas de carga, el cambio a ECS permitió reducir la latencia extrema (p95) de ~151s a ~31s en flujos de carga de archivos. El sistema responde con mayor agilidad.
*   **Simplificación Operativa:** El equipo deja de preocuparse por "deprecated APIs" de Kubernetes o la gestión de *taints & tolerations*. La configuración se centraliza en *Task Definitions* y *Auto Scaling Groups*.
*   **Gestión de "Cold Starts":** En ECS es vital ajustar correctamente el *Health Check Grace Period* y los recursos (CPU/RAM) reservados para evitar que las tareas entren en un ciclo de reinicios (flapping) durante el arranque.
*   **Foco en la Aplicación:** Al eliminar la capa de complejidad de EKS, se hizo evidente que los errores restantes (tasa de éxito < 100%) no son de infraestructura, sino de **lógica de aplicación** (validaciones de votos, bloqueos en base de datos).

# Conclusiones

La migración a **Amazon ECS** en la Entrega 5 ha demostrado ser una decisión arquitectónica acertada para el perfil actual del proyecto.

**Entrega 4 (EKS)** ofrecía un ecosistema potente y estandarizado, pero introducía una sobrecarga administrativa y tiempos de aprovisionamiento (nodos + pods) que penalizaban el rendimiento durante los picos de carga abruptos (pruebas de estrés), resultando en latencias p95 inaceptables.

**Entrega 5 (ECS)** ha logrado:
1.  **Reducir la latencia p95 global** en un 59% (interactivo) y 79% (uploads).
2.  **Aumentar la tasa de éxito** global del sistema, haciendo el pipeline de procesamiento de video mucho más robusto.
3.  **Reducir el costo operativo** y la carga cognitiva del equipo de desarrollo.

**Hallazgo Crítico:**
Aunque la infraestructura ECS ha resuelto los problemas de escalabilidad a nivel de cómputo, las pruebas de carga revelan que el **cuello de botella se ha desplazado a la base de datos (RDS) y la lógica de negocio**. Los tiempos de respuesta siguen siendo altos (aunque mejores) debido a consultas ineficientes y bloqueos lógicos, no por falta de capacidad en los contenedores.

**Regla práctica actualizada:**
*   Si buscas orquestación granular, multi-cloud y tienes un equipo de plataforma dedicado → **EKS**.
*   Si buscas integración profunda con AWS, simplicidad operativa y rendimiento "out-of-the-box" para cargas web y workers → **ECS**.

**Próximos pasos:** La infraestructura ya no es el limitante principal. Los esfuerzos deben centrarse en optimización de código, *query tuning* en base de datos y estrategias de caché más agresivas.