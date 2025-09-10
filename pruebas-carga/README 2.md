# Sistema de Videos y Ranking

[![Ver el video corto en YouTube](https://img.youtube.com/vi/LyWnp02K7Fs/maxresdefault.jpg)](https://www.youtube.com/watch?v=LyWnp02K7Fs)

[![Video Demo](https://img.shields.io/badge/🎥_Video_Demo-YouTube-red?style=for-the-badge&logo=youtube)](https://youtu.be/eRjMitL2NRw)

> **🎬 [Ver Demo del Sistema en Acción](https://youtu.be/eRjMitL2NRw)** - Demostración completa del funcionamiento del sistema

[![Ver el video en YouTube](https://img.youtube.com/vi/eRjMitL2NRw/maxresdefault.jpg)](https://www.youtube.com/watch?v=eRjMitL2NRw)



Sistema distribuido para procesamiento de videos con arquitectura de microservicios, implementando patrones de mensajería asíncrona y observabilidad completa.

## 👥 Equipo de Desarrollo
 
- **Ricardo Andres Leyva Osorio** - r.leyva@uniandes.edu.co - Developer
- **Edda Camila Rodriguez Mojica** - ec.rodriguez@uniandes.edu.co - Developer
- **Cristian David Paredes Bravo** - c.paredesb@uniandes.edu.co - Developer
- **Andrea Carolina Cely Duarte** - a.celyd@uniandes.edu.co - Developer
- **Juan Carlos Martinez Muñoz** - jc.martinezm1@uniandes.edu.co - Developer

## Integración con sonar cloud
https://sonarcloud.io/organizations/tatoand97/projects

![Evidencia SonarCloud](Evidencia-sonar-cloud.png)



## 🏗️ Arquitectura del Sistema

### Vista General de la Arquitectura

```mermaid
graph TB
    subgraph "Frontend Layer"
        UI[Web UI/Client]
    end
    
    subgraph "API Gateway"
        NGINX[Nginx Reverse Proxy]
    end
    
    subgraph "Core Services"
        API[API Service]
    end
    
    subgraph "Message Broker"
        RMQ[RabbitMQ]
        RMQ_DLX[Dead Letter Exchange]
        RMQ_DLQ[Dead Letter Queue]
    end
    
    subgraph "Worker Services"
        WK1[EditVideo Worker]
        WK2[TrimVideo Worker]
        WK3[Watermarking Worker]
        WK4[AudioRemoval Worker]
        WK5[AdminCache Worker]
        WK6[StatesMachine Worker]
        WK7[GossipOpenClose Worker]
    end
    
    subgraph "Storage Layer"
        PG[(PostgreSQL)]
        MINIO[MinIO Object Storage]
        REDIS[(Redis Cache)]
    end
    
    subgraph "Observability Stack"
        PROM[Prometheus]
        GRAF[Grafana]
        LOKI[Loki]
        PROMTAIL[Promtail]
        JAEGER[Jaeger]
    end
    
    UI --> NGINX
    NGINX --> API
    NGINX --> GRAF
    NGINX --> JAEGER
    NGINX --> MINIO
    NGINX --> RMQ
    
    API --> RMQ
    API --> PG
    API --> REDIS
    API --> MINIO
    
    RMQ --> WK1
    RMQ --> WK2
    RMQ --> WK3
    RMQ --> WK4
    RMQ --> WK5
    RMQ --> WK6
    RMQ --> WK7
    
    RMQ_DLX --> RMQ_DLQ
    
    WK1 --> PG
    WK1 --> MINIO
    WK2 --> PG
    WK2 --> MINIO
    WK3 --> PG
    WK3 --> MINIO
    WK4 --> PG
    WK4 --> MINIO
    WK5 --> REDIS
    WK6 --> PG
    WK7 --> PG
    
    PROM --> RMQ
    PROM --> LOKI
    GRAF --> PROM
    GRAF --> LOKI
    PROMTAIL --> LOKI
    
    style API fill:#e1f5fe
    style WK1 fill:#f3e5f5
    style WK2 fill:#f3e5f5
    style WK3 fill:#f3e5f5
    style WK4 fill:#f3e5f5
    style WK5 fill:#f3e5f5
    style WK6 fill:#f3e5f5
    style WK7 fill:#f3e5f5
```

### Arquitectura de Microservicios

```mermaid
C4Context
    title Diagrama de Contexto - Sistema de Videos y Ranking
    
    Person(user, "Usuario", "Sube y procesa videos")
    System(videoSystem, "Sistema de Videos", "Procesa videos con múltiples operaciones")
    
    System_Ext(storage, "MinIO Storage", "Almacenamiento de objetos")
    System_Ext(database, "PostgreSQL", "Base de datos relacional")
    System_Ext(cache, "Redis", "Cache en memoria")
    System_Ext(monitoring, "Stack Observabilidad", "Prometheus, Grafana, Loki, Jaeger")
    
    Rel(user, videoSystem, "Usa")
    Rel(videoSystem, storage, "Almacena videos")
    Rel(videoSystem, database, "Persiste metadatos")
    Rel(videoSystem, cache, "Cache de datos")
    Rel(videoSystem, monitoring, "Métricas y logs")
```

### Diagrama de Componentes

```mermaid
graph LR
    subgraph "Video Processing Pipeline"
        A[Video Upload] --> B[Queue Message]
        B --> C{Worker Type}
        
        C -->|Edit| D[EditVideo Worker]
        C -->|Trim| E[TrimVideo Worker]
        C -->|Watermark| F[Watermarking Worker]
        C -->|Audio| G[AudioRemoval Worker]
        
        D --> H[Processed Video]
        E --> H
        F --> H
        G --> H
        
        H --> I[Update Database]
        H --> J[Store in MinIO]
    end
    
    subgraph "State Management"
        K[StatesMachine Worker] --> L[State Updates]
        L --> M[Database]
    end
    
    subgraph "Cache Management"
        N[AdminCache Worker] --> O[Redis Updates]
    end
    
    subgraph "Communication"
        P[GossipOpenClose Worker] --> Q[Service Discovery]
    end
```

## 🔄 Diagramas de Secuencia

### Procesamiento de Video

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant RabbitMQ
    participant Worker
    participant MinIO
    participant PostgreSQL
    participant Redis
    
    Client->>API: POST /upload-video
    API->>MinIO: Store raw video
    API->>PostgreSQL: Create video record
    API->>RabbitMQ: Publish processing message
    API->>Client: Return job ID
    
    RabbitMQ->>Worker: Consume message
    Worker->>MinIO: Download raw video
    Worker->>Worker: Process video
    Worker->>MinIO: Upload processed video
    Worker->>PostgreSQL: Update video status
    Worker->>Redis: Cache result
    Worker->>RabbitMQ: Ack message
    
    Note over Worker: If processing fails
    Worker->>RabbitMQ: Nack message
    RabbitMQ->>RabbitMQ: Retry or DLQ
```

### Manejo de Errores y Dead Letter Queue

```mermaid
sequenceDiagram
    participant RabbitMQ
    participant Worker
    participant DLX
    participant DLQ
    participant Monitor
    
    RabbitMQ->>Worker: Deliver message
    Worker->>Worker: Process fails
    Worker->>RabbitMQ: Nack message
    
    alt Retry attempts < Max
        RabbitMQ->>RabbitMQ: Requeue message
        RabbitMQ->>Worker: Redeliver message
    else Max retries reached
        RabbitMQ->>DLX: Route to Dead Letter Exchange
        DLX->>DLQ: Store in Dead Letter Queue
        DLQ->>Monitor: Alert failed message
    end
```

### Cache Management

```mermaid
sequenceDiagram
    participant API
    participant Redis
    participant AdminCache
    participant PostgreSQL
    
    API->>Redis: GET cached data
    alt Cache hit
        Redis->>API: Return cached data
    else Cache miss
        API->>PostgreSQL: Query database
        PostgreSQL->>API: Return data
        API->>Redis: Store in cache
    end
    
    Note over AdminCache: Background cache refresh
    AdminCache->>PostgreSQL: Query updated data
    AdminCache->>Redis: Update cache
```

## 🛠️ Tecnologías y Componentes

### Core Services
- **API Service**: Servicio principal REST API
- **Workers**: 7 workers especializados para diferentes tareas

### Message Broker
- **RabbitMQ**: Sistema de mensajería con dead letter queues
- **Exchanges**: `orders.dlx` para manejo de errores
- **Queues**: `orders` principal, `orders.dlq` para mensajes fallidos

### Storage
- **PostgreSQL**: Base de datos principal
- **MinIO**: Almacenamiento de objetos para videos
- **Redis**: Cache en memoria

### Observability
- **Prometheus**: Métricas y monitoreo
- **Grafana**: Dashboards y visualización
- **Loki**: Agregación de logs
- **Promtail**: Recolección de logs
- **Jaeger**: Distributed tracing

### Infrastructure
- **Nginx**: Reverse proxy y load balancer
- **Docker**: Containerización
- **Docker Compose**: Orquestación local

## 🚀 Workers Especializados

### 1. EditVideo Worker
- **Función**: Edición general de videos
- **Operaciones**: Cortes, filtros, efectos
- **Storage**: MinIO para videos, PostgreSQL para metadatos

### 2. TrimVideo Worker
- **Función**: Recorte de videos
- **Operaciones**: Corte por tiempo, segmentación
- **Optimización**: Procesamiento eficiente de segmentos

### 3. Watermarking Worker
- **Función**: Aplicación de marcas de agua
- **Operaciones**: Overlay de imágenes/texto
- **Configuración**: Posición, transparencia, tamaño

### 4. AudioRemoval Worker
- **Función**: Eliminación de audio
- **Operaciones**: Extracción de pistas de audio
- **Output**: Videos sin audio

### 5. AdminCache Worker
- **Función**: Gestión de cache administrativo
- **Operaciones**: Actualización de cache, limpieza
- **Target**: Redis cache management

### 6. StatesMachine Worker
- **Función**: Gestión de estados del sistema
- **Operaciones**: Transiciones de estado, validaciones
- **Persistence**: PostgreSQL

### 7. GossipOpenClose Worker
- **Función**: Comunicación entre servicios
- **Operaciones**: Service discovery, health checks
- **Protocol**: Gossip protocol implementation

## 🗄️ Estructura de Base de Datos

### Diagrama Entidad-Relación

```mermaid
erDiagram
    VIDEOS {
        uuid id PK
        varchar filename
        bigint size
        decimal duration
        varchar status
        varchar original_path
        varchar processed_path
        timestamp created_at
        timestamp updated_at
        uuid user_id FK
    }
    
    JOBS {
        uuid id PK
        uuid video_id FK
        varchar operation
        varchar status
        varchar worker_type
        integer attempts
        text error_message
        json parameters
        timestamp created_at
        timestamp started_at
        timestamp completed_at
    }
    
    USERS {
        uuid id PK
        varchar username
        varchar email
        varchar password_hash
        timestamp created_at
        timestamp last_login
    }
    
    VIDEO_RANKINGS {
        uuid id PK
        uuid video_id FK
        integer score
        integer views
        integer likes
        integer dislikes
        timestamp updated_at
    }
    
    PROCESSING_LOGS {
        uuid id PK
        uuid job_id FK
        varchar level
        text message
        json metadata
        timestamp created_at
    }
    
    SERVICE_STATES {
        uuid id PK
        varchar service_name
        varchar state
        json configuration
        timestamp updated_at
    }
    
    VIDEOS ||--o{ JOBS : "has"
    USERS ||--o{ VIDEOS : "owns"
    VIDEOS ||--|| VIDEO_RANKINGS : "ranked"
    JOBS ||--o{ PROCESSING_LOGS : "logs"
```

### Definiciones de Tablas

#### Videos
```sql
CREATE TABLE videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    duration DECIMAL(10,2),
    status VARCHAR(50) DEFAULT 'pending',
    original_path VARCHAR(500),
    processed_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id UUID REFERENCES users(id)
);

CREATE INDEX idx_videos_status ON videos(status);
CREATE INDEX idx_videos_user_id ON videos(user_id);
CREATE INDEX idx_videos_created_at ON videos(created_at);
```

#### Jobs
```sql
CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID NOT NULL REFERENCES videos(id),
    operation VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'pending',
    worker_type VARCHAR(50),
    attempts INTEGER DEFAULT 0,
    error_message TEXT,
    parameters JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_jobs_video_id ON jobs(video_id);
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_worker_type ON jobs(worker_type);
CREATE INDEX idx_jobs_created_at ON jobs(created_at);
```

#### Users
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

#### Video Rankings
```sql
CREATE TABLE video_rankings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID UNIQUE NOT NULL REFERENCES videos(id),
    score INTEGER DEFAULT 0,
    views INTEGER DEFAULT 0,
    likes INTEGER DEFAULT 0,
    dislikes INTEGER DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rankings_score ON video_rankings(score DESC);
CREATE INDEX idx_rankings_views ON video_rankings(views DESC);
```

#### Processing Logs
```sql
CREATE TABLE processing_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jobs(id),
    level VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_logs_job_id ON processing_logs(job_id);
CREATE INDEX idx_logs_level ON processing_logs(level);
CREATE INDEX idx_logs_created_at ON processing_logs(created_at);
```

#### Service States
```sql
CREATE TABLE service_states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name VARCHAR(100) UNIQUE NOT NULL,
    state VARCHAR(50) NOT NULL,
    configuration JSONB,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_states_service_name ON service_states(service_name);
```

### Enums y Constraints

```sql
-- Video status enum
ALTER TABLE videos ADD CONSTRAINT check_video_status 
    CHECK (status IN ('pending', 'processing', 'completed', 'failed', 'cancelled'));

-- Job status enum
ALTER TABLE jobs ADD CONSTRAINT check_job_status 
    CHECK (status IN ('pending', 'processing', 'completed', 'failed', 'retrying'));

-- Job operation enum
ALTER TABLE jobs ADD CONSTRAINT check_job_operation 
    CHECK (operation IN ('edit', 'trim', 'watermark', 'audio_removal'));

-- Worker type enum
ALTER TABLE jobs ADD CONSTRAINT check_worker_type 
    CHECK (worker_type IN ('editvideo', 'trimvideo', 'watermarking', 'audioremoval'));

-- Log level enum
ALTER TABLE processing_logs ADD CONSTRAINT check_log_level 
    CHECK (level IN ('DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL'));
```

### Triggers y Funciones

```sql
-- Update timestamp trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_videos_updated_at BEFORE UPDATE ON videos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rankings_updated_at BEFORE UPDATE ON video_rankings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_states_updated_at BEFORE UPDATE ON service_states
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### Vistas Útiles

```sql
-- Vista de videos con información de jobs
CREATE VIEW video_processing_status AS
SELECT 
    v.id,
    v.filename,
    v.status as video_status,
    COUNT(j.id) as total_jobs,
    COUNT(CASE WHEN j.status = 'completed' THEN 1 END) as completed_jobs,
    COUNT(CASE WHEN j.status = 'failed' THEN 1 END) as failed_jobs,
    MAX(j.completed_at) as last_processed
FROM videos v
LEFT JOIN jobs j ON v.id = j.video_id
GROUP BY v.id, v.filename, v.status;

-- Vista de ranking de videos
CREATE VIEW top_videos AS
SELECT 
    v.id,
    v.filename,
    vr.score,
    vr.views,
    vr.likes,
    vr.dislikes,
    (vr.likes::float / NULLIF(vr.likes + vr.dislikes, 0)) as like_ratio
FROM videos v
JOIN video_rankings vr ON v.id = vr.video_id
WHERE v.status = 'completed'
ORDER BY vr.score DESC, vr.views DESC;
```

## 📋 API Documentation

### Swagger/OpenAPI Specification

```yaml
openapi: 3.0.3
info:
  title: Sistema de Videos y Ranking API
  version: "1.0.0"
  description: API para procesamiento distribuido de videos con arquitectura de microservicios
servers:
  - url: http://localhost:8080
    description: Desarrollo local
  - url: https://api.videorank.com
    description: Producción

paths:
  /health:
    get:
      summary: Health check del sistema
      tags: [Health]
      responses:
        '200':
          description: Sistema saludable
          content:
            application/json:
              schema:
                type: object
                properties:
                  status:
                    type: string
                    enum: [healthy, degraded, unhealthy]
                  services:
                    type: object
                    properties:
                      database: { type: string }
                      redis: { type: string }
                      rabbitmq: { type: string }
                      minio: { type: string }

  /metrics:
    get:
      summary: Métricas del sistema
      tags: [Health]
      responses:
        '200':
          description: Métricas en formato Prometheus
          content:
            text/plain:
              schema:
                type: string

  /users:
    post:
      summary: Crear nuevo usuario
      tags: [Users]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [username, email, password]
              properties:
                username: { type: string, minLength: 3 }
                email: { type: string, format: email }
                password: { type: string, minLength: 8 }
      responses:
        '201':
          description: Usuario creado exitosamente
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'

  /videos:
    post:
      summary: Subir video para procesamiento
      tags: [Videos]
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              required: [file, operation]
              properties:
                file:
                  type: string
                  format: binary
                operation:
                  type: string
                  enum: [edit, trim, watermark, audio_removal]
                parameters:
                  type: object
      responses:
        '202':
          description: Video aceptado para procesamiento
          content:
            application/json:
              schema:
                type: object
                properties:
                  job_id: { type: string, format: uuid }
                  video_id: { type: string, format: uuid }
                  status: { type: string }
    get:
      summary: Listar videos
      tags: [Videos]
      parameters:
        - name: status
          in: query
          schema:
            type: string
            enum: [pending, processing, completed, failed, cancelled]
        - name: limit
          in: query
          schema: { type: integer, default: 20 }
        - name: offset
          in: query
          schema: { type: integer, default: 0 }
      responses:
        '200':
          description: Lista de videos
          content:
            application/json:
              schema:
                type: object
                properties:
                  videos:
                    type: array
                    items:
                      $ref: '#/components/schemas/Video'
                  total: { type: integer }

  /videos/{id}:
    get:
      summary: Obtener video por ID
      tags: [Videos]
      parameters:
        - name: id
          in: path
          required: true
          schema: { type: string, format: uuid }
      responses:
        '200':
          description: Detalles del video
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Video'
    delete:
      summary: Eliminar video
      tags: [Videos]
      parameters:
        - name: id
          in: path
          required: true
          schema: { type: string, format: uuid }
      responses:
        '204':
          description: Video eliminado exitosamente

  /videos/{id}/status:
    get:
      summary: Estado del procesamiento
      tags: [Videos]
      parameters:
        - name: id
          in: path
          required: true
          schema: { type: string, format: uuid }
      responses:
        '200':
          description: Estado actual del procesamiento
          content:
            application/json:
              schema:
                type: object
                properties:
                  video_id: { type: string, format: uuid }
                  status:
                    type: string
                    enum: [pending, processing, completed, failed, cancelled]
                  progress: { type: integer, minimum: 0, maximum: 100 }
                  message: { type: string }
                  current_operation: { type: string }

  /jobs:
    get:
      summary: Listar trabajos de procesamiento
      tags: [Jobs]
      parameters:
        - name: status
          in: query
          schema:
            type: string
            enum: [pending, processing, completed, failed, retrying]
        - name: worker_type
          in: query
          schema:
            type: string
            enum: [editvideo, trimvideo, watermarking, audioremoval]
      responses:
        '200':
          description: Lista de trabajos
          content:
            application/json:
              schema:
                type: object
                properties:
                  jobs:
                    type: array
                    items:
                      $ref: '#/components/schemas/Job'
                  total: { type: integer }

  /jobs/{id}/retry:
    post:
      summary: Reintentar trabajo fallido
      tags: [Jobs]
      parameters:
        - name: id
          in: path
          required: true
          schema: { type: string, format: uuid }
      responses:
        '202':
          description: Trabajo reencolado para reintento
          content:
            application/json:
              schema:
                type: object
                properties:
                  job_id: { type: string, format: uuid }
                  status: { type: string }
                  message: { type: string }

  /rankings:
    get:
      summary: Obtener ranking de videos
      tags: [Rankings]
      parameters:
        - name: limit
          in: query
          schema: { type: integer, default: 10 }
        - name: offset
          in: query
          schema: { type: integer, default: 0 }
      responses:
        '200':
          description: Ranking de videos
          content:
            application/json:
              schema:
                type: object
                properties:
                  rankings:
                    type: array
                    items:
                      $ref: '#/components/schemas/VideoRanking'
                  total: { type: integer }

  /rankings/{video_id}/vote:
    post:
      summary: Votar por un video
      tags: [Rankings]
      parameters:
        - name: video_id
          in: path
          required: true
          schema: { type: string, format: uuid }
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [vote_type]
              properties:
                vote_type:
                  type: string
                  enum: [like, dislike]
      responses:
        '200':
          description: Voto registrado exitosamente
          content:
            application/json:
              schema:
                type: object
                properties:
                  message: { type: string }
                  current_score: { type: integer }

components:
  schemas:
    User:
      type: object
      properties:
        id: { type: string, format: uuid }
        username: { type: string }
        email: { type: string, format: email }
        created_at: { type: string, format: date-time }
        last_login: { type: string, format: date-time }

    Video:
      type: object
      properties:
        id: { type: string, format: uuid }
        filename: { type: string }
        size: { type: integer, format: int64 }
        duration: { type: number, format: float }
        status:
          type: string
          enum: [pending, processing, completed, failed, cancelled]
        original_path: { type: string }
        processed_path: { type: string }
        created_at: { type: string, format: date-time }
        updated_at: { type: string, format: date-time }
        user_id: { type: string, format: uuid }
        download_url: { type: string, format: uri }

    Job:
      type: object
      properties:
        id: { type: string, format: uuid }
        video_id: { type: string, format: uuid }
        operation:
          type: string
          enum: [edit, trim, watermark, audio_removal]
        status:
          type: string
          enum: [pending, processing, completed, failed, retrying]
        worker_type:
          type: string
          enum: [editvideo, trimvideo, watermarking, audioremoval]
        attempts: { type: integer, minimum: 0 }
        error_message: { type: string }
        parameters: { type: object }
        created_at: { type: string, format: date-time }
        started_at: { type: string, format: date-time }
        completed_at: { type: string, format: date-time }

    VideoRanking:
      type: object
      properties:
        video_id: { type: string, format: uuid }
        filename: { type: string }
        score: { type: integer }
        views: { type: integer }
        likes: { type: integer }
        dislikes: { type: integer }
        rank: { type: integer }
        like_ratio: { type: number, format: float }
        updated_at: { type: string, format: date-time }

    Error:
      type: object
      properties:
        error: { type: string }
        message: { type: string }
        code: { type: integer }
        timestamp: { type: string, format: date-time }
```

### Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/videos` | Subir video para procesamiento |
| GET | `/videos` | Listar todos los videos |
| GET | `/videos/{id}` | Obtener detalles de un video |
| GET | `/videos/{id}/status` | Estado del procesamiento |
| GET | `/health` | Health check del sistema |
| GET | `/metrics` | Métricas para Prometheus |

### Códigos de Respuesta

- **200**: Operación exitosa
- **202**: Aceptado para procesamiento
- **400**: Error en la petición
- **404**: Recurso no encontrado
- **500**: Error interno del servidor
- **503**: Servicio no disponible

## 📊 Monitoreo y Observabilidad

### Métricas (Prometheus)
- **RabbitMQ**: Queue depth, message rates, consumer lag
- **Loki**: Log ingestion rates, query performance
- **System**: CPU, memory, disk usage
- **Application**: Processing times, error rates

### Logs (Loki + Promtail)
- **Centralized logging**: Todos los servicios
- **Log aggregation**: Promtail recolecta de containers
- **Query interface**: Grafana para búsqueda

### Tracing (Jaeger)
- **Distributed tracing**: Seguimiento de requests
- **Performance analysis**: Latencia entre servicios
- **Error tracking**: Identificación de fallos

### Dashboards (Grafana)
- **System overview**: Estado general del sistema
- **Worker performance**: Métricas por worker
- **Queue monitoring**: Estado de colas RabbitMQ
- **Error tracking**: Análisis de errores

## 🔧 Configuración y Despliegue

### Variables de Entorno
```bash
# Database
POSTGRES_USER=app_user
POSTGRES_PASSWORD=app_password
POSTGRES_DB=videorank
DATABASE_URL=postgres://app_user:app_password@postgres:5432/videorank

# MinIO
MINIO_ROOT_USER=minio
MINIO_ROOT_PASSWORD=minio12345
MINIO_ENDPOINT=minio:9000

# RabbitMQ
RABBITMQ_DEFAULT_USER=admin
RABBITMQ_DEFAULT_PASS=admin
RABBITMQ_URL=amqp://admin:admin@rabbitmq:5672/

# Workers
MAX_RETRIES=3
MAX_QUEUE_LENGTH=1000
```

### Puertos de Servicios
- **Nginx Proxy**: 8080-8084
- **PostgreSQL**: 5432
- **Redis**: 6379
- **RabbitMQ**: 5672 (AMQP), 15672 (UI)
- **MinIO**: 9000 (API), 9001 (Console)
- **Prometheus**: 9090
- **Grafana**: 3000
- **Loki**: 3100
- **Jaeger**: 16686 (UI), 4317/4318 (OTLP)

### Comandos de Despliegue
```bash
# Iniciar todos los servicios
docker-compose up -d

# Ver logs de servicios
docker-compose logs -f [service-name]

# Escalar workers
docker-compose up -d --scale editvideo-worker=3

# Detener servicios
docker-compose down
```

## 🔒 Seguridad y Resilencia

### Message Reliability
- **Dead Letter Queues**: Manejo de mensajes fallidos
- **Message TTL**: 60 segundos timeout
- **Retry Logic**: Máximo 3 reintentos
- **Acknowledgments**: Confirmación manual de procesamiento

### Data Persistence
- **Volume Mounting**: Datos persistentes en volumes
- **Backup Strategy**: Snapshots automáticos
- **Health Checks**: Monitoreo de servicios críticos

### Network Security
- **Internal Networks**: Comunicación entre containers
- **Reverse Proxy**: Nginx como punto de entrada único
- **Service Isolation**: Containers aislados

## 📈 Escalabilidad

### Horizontal Scaling
- **Worker Scaling**: Múltiples instancias por worker type
- **Load Balancing**: Nginx distribuye carga
- **Queue Distribution**: RabbitMQ maneja distribución

### Performance Optimization
- **Caching Strategy**: Redis para datos frecuentes
- **Async Processing**: Workers procesan en background
- **Resource Limits**: Containers con límites definidos

## 🧪 Testing

### Estructura de Tests
```
tests/
├── unit/
│   ├── application/
│   └── infrastructure/
└── mocks/
```

### Coverage Reports
- **Coverage files**: `coverage.out` por servicio
- **HTML Reports**: `coverage.html` disponible
- **CI Integration**: Tests automáticos en pipeline

## 📚 Documentación Adicional

- **API Documentation**: Swagger/OpenAPI specs
- **Worker Specifications**: Documentación detallada por worker
- **Deployment Guide**: Guía de despliegue en producción
- **Troubleshooting**: Guía de resolución de problemas
- **Performance Tuning**: Optimización de rendimiento

---
Video land VideoRank – Frontend
================================

Resumen
- React + Vite + TypeScript.
- Consumo de la API basada en la colección Postman incluida (auth, videos, rankings, ubicación).
- Se entrega como build estático servido por Nginx en el mismo `docker-compose` del backend (puerto 8081).

Variables de entorno
- `VITE_API_BASE_URL` (default `http://localhost:8080`)

Desarrollo local
1) Instalar dependencias: `npm install`
2) Ejecutar dev server: `npm run dev`
3) App: http://localhost:5173

Cómo levantar el Backend/API
- El frontend asume la API en `http://localhost:8080` (nginx → api).
- Desde la raíz del repo, levanta los servicios necesarios:

  1) Infra de soporte: `docker compose up -d postgres redis rabbitmq minio minio-buckets`
  2) Migraciones (one-off): `docker compose up --build migrate && docker compose rm -f migrate`
  3) API + Nginx: `docker compose up -d api nginx`

- Verificación: `curl http://localhost:8080/health` → `{ "status": "ok" }`

Build de producción
1) `npm run build` → genera `frontend/dist`
2) Con `docker-compose up -d nginx` la app queda servida en http://localhost:8081

Estructura de features principales
- Autenticación (login, registro, logout; token guardado en localStorage).
- Videos (listar mis videos, detalle, subir multipart, eliminar).
- Público (listar videos públicos, votar video).
- Rankings (paginado y filtro por ciudad).

Rutas
- `/` públicos + votar
- `/rankings` listados
- `/login`, `/register`
- `/profile` (protegida)
- `/videos` (protegida)
- `/upload` (protegida)
- `/videos/:id` (protegida)

Notas
- El backend define CORS abierto (`*`) en `docker-compose.yml`, por lo que el dev server funciona sin proxy adicional.
- Para el despliegue, sólo es necesario construir (`dist/`) y dejar que Nginx lo sirva en el puerto 8081.


---

**Versión**: 1.0.0  
**Última actualización**: 2025  
**Mantenido por**: Equipo de Desarrollo
