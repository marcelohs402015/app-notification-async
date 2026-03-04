# Aplicação de Notificação Assíncrona entre Usuários

> Aplicação fullstack de **envio de notificações entre usuários em tempo real**, construída com Clean Architecture, mensageria assíncrona via RabbitMQ e push via SSE.

---

## Índice

- [Sobre](#sobre)
- [Arquitetura](#arquitetura)
- [Fluxo de Notificação](#fluxo-de-notificação)
- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Testes](#testes)
- [Pré-requisitos](#pré-requisitos)
- [Como Executar](#como-executar)
- [API](#api)
- [Schema do Banco de Dados](#schema-do-banco-de-dados)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Decisões Arquiteturais](#decisões-arquiteturais)

---

## Sobre

Permite que usuários autenticados enviem notificações tipadas entre si em tempo real. Cada notificação é persistida no PostgreSQL, roteada de forma assíncrona pelo RabbitMQ e entregue instantaneamente ao destinatário via **Server-Sent Events (SSE)**, sem necessidade de polling.

**Funcionalidades:**
- Cadastro e autenticação de usuários com JWT
- Envio de notificações tipadas (`INFO`, `WARNING`, `SUCCESS`, `ERROR`)
- Entrega em tempo real via SSE (sem WebSocket)
- Histórico de notificações paginado
- Marcação de notificações como lidas
- Contador de notificações não lidas

---

## Arquitetura

O backend segue **Clean Architecture** com camadas concêntricas e regra de dependência estrita: camadas externas dependem das internas, nunca o contrário.

```
┌──────────────────────────────────────────────────────────────────┐
│                           Frontend                               │
│          React 18 · TypeScript · Vite · TailwindCSS             │
│               Zustand (estado) · Axios (HTTP)                   │
│                    EventSource (SSE stream)                      │
└───────────────────────────┬──────────────────────────────────────┘
                            │ HTTP / SSE
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│                    Nginx (reverse proxy)                         │
│                   /api/* → backend:8080                         │
└───────────────────────────┬──────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│                    Backend — Clean Architecture                  │
│           Spring Boot 3.3 · Java 21 · Virtual Threads           │
│                                                                  │
│  presentation/adapter/   →   application/port/input/            │
│  (Controllers + DTOs)        (Interfaces dos Use Cases)         │
│                                      │                          │
│                              application/usecase/               │
│                              (Implementações)                   │
│                                      │                          │
│                              application/port/output/           │
│                              (Contratos de infraestrutura)      │
│                                      │                          │
│                              infrastructure/                    │
│                              (Adapters: JPA, RabbitMQ, JWT...)  │
│                                      │                          │
│                              domain/entity/                     │
│                              (POJOs puros, zero dependências)   │
└───────────────┬──────────────────────────┬───────────────────────┘
                │                          │
                ▼                          ▼
      ┌──────────────────┐      ┌─────────────────────┐
      │   PostgreSQL 16  │      │     RabbitMQ 3       │
      │   Flyway         │      │  notification        │
      │   users          │      │  .exchange           │
      │   notifications  │      │  (TopicExchange)     │
      └──────────────────┘      └─────────────────────┘
```

### Camadas e responsabilidades

| Camada | Responsabilidade |
|---|---|
| `domain/entity` | Entidades de domínio puras — `User`, `Notification`, `NotificationType`. Zero dependências externas |
| `application/port/input` | Interfaces dos use cases. Os controllers chamam estas interfaces, nunca a implementação diretamente |
| `application/port/output` | Contratos que a infraestrutura deve satisfazer — repositórios, publisher, encoder, JWT |
| `application/usecase` | Implementações dos casos de uso. Dependem apenas de `domain` e `port/output` |
| `infrastructure` | Adapters que implementam `port/output` — JPA, RabbitMQ, Spring Security, AOP |
| `presentation/adapter` | Controllers HTTP, DTOs de request/response |

---

## Fluxo de Notificação

```
  [Remetente]
      │
      │  POST /api/notifications/send
      ▼
  NotificationController
      │  chama SendNotificationPort (interface)
      ▼
  SendNotificationUseCase
      │
      ├─── persiste via NotificationRepositoryPort → NotificationRepositoryAdapter → PostgreSQL
      │
      └─── publica via NotificationPublisherPort → NotificationPublisherAdapter → RabbitMQ
                │  exchange: notification.exchange
                │  routing key: notification.<recipientId>
                ▼
          NotificationListenerManager
          (fila dinâmica: notification.user.<uuid>, criada ao conectar no SSE)
                │
                ▼
          SseEmitterRegistry.sendToUser()
                │  evento SSE: name="notification"
                ▼
  [Destinatário — Browser]
          useSSE hook (EventSource)
                │
                ├─── useNotificationStore (Zustand) → atualiza UI
                └─── react-hot-toast → exibe toast
```

---

## Tecnologias

### Backend

| Tecnologia | Versão | Função |
|---|---|---|
| Java | 21 | Linguagem (Virtual Threads habilitadas) |
| Spring Boot | 3.3.4 | Framework principal |
| Spring Security | 6.x | Autenticação JWT stateless |
| Spring AMQP | 3.x | Integração com RabbitMQ |
| Spring Data JPA | 3.x | Persistência (camada de infraestrutura) |
| Spring AOP | 3.x | Cross-cutting concerns (performance logging) |
| Flyway | 10.x | Migrations de banco — única fonte de verdade do schema |
| PostgreSQL | 16 | Banco de dados principal |
| RabbitMQ | 3 | Broker de mensagens assíncronas |
| jjwt | 0.12.6 | Geração e validação de JWT |
| Lombok | latest | Redução de boilerplate (apenas em camadas não-domínio) |

### Frontend

| Tecnologia | Versão | Função |
|---|---|---|
| React | 18.3 | Framework de UI |
| TypeScript | 5.5 | Tipagem estática |
| Vite | 5.4 | Bundler e dev server |
| TailwindCSS | 3.4 | Estilização |
| Zustand | 5.0 | Gerenciamento de estado global |
| Axios | 1.7 | Cliente HTTP com interceptors JWT |
| React Router | 6.26 | Roteamento client-side |
| Framer Motion | 11.5 | Animações |
| react-hot-toast | 2.4 | Notificações toast |
| lucide-react | 0.441 | Ícones |

---

## Estrutura do Projeto

```
app-notification-async/
├── backend/
│   ├── src/main/java/com/appnotification/
│   │   ├── domain/
│   │   │   └── entity/                    # User · Notification · NotificationType
│   │   │                                  # POJOs puros — zero dependências externas
│   │   ├── application/
│   │   │   ├── port/
│   │   │   │   ├── input/
│   │   │   │   │   ├── auth/              # RegisterUserPort · LoginUserPort
│   │   │   │   │   ├── notification/      # SendNotificationPort · GetNotificationsPort
│   │   │   │   │   │                      # MarkNotificationAsReadPort · CountUnreadNotificationsPort
│   │   │   │   │   └── user/              # ListUsersPort
│   │   │   │   └── output/                # UserRepositoryPort · NotificationRepositoryPort
│   │   │   │                              # NotificationPublisherPort · PasswordEncoderPort
│   │   │   │                              # TokenGeneratorPort · SsePort
│   │   │   └── usecase/
│   │   │       ├── auth/                  # RegisterUserUseCase · LoginUserUseCase
│   │   │       ├── notification/          # SendNotificationUseCase · GetNotificationsUseCase
│   │   │       │                          # MarkNotificationAsReadUseCase · CountUnreadNotificationsUseCase
│   │   │       └── user/                  # ListUsersUseCase
│   │   ├── infrastructure/
│   │   │   ├── persistence/
│   │   │   │   ├── entity/                # UserJpaEntity · NotificationJpaEntity
│   │   │   │   ├── repository/            # UserJpaRepository · NotificationJpaRepository
│   │   │   │   └── adapter/               # UserRepositoryAdapter · NotificationRepositoryAdapter
│   │   │   ├── messaging/                 # NotificationPublisherAdapter · NotificationListenerManager
│   │   │   │                              # NotificationMessage
│   │   │   ├── security/                  # JwtService · JwtAuthFilter
│   │   │   │                              # PasswordEncoderAdapter · TokenGeneratorAdapter
│   │   │   ├── sse/                       # SseEmitterRegistry
│   │   │   ├── aop/                       # @LogExecutionTime · ExecutionTimeAspect
│   │   │   └── config/                    # RabbitMQConfig · SecurityConfig
│   │   ├── presentation/
│   │   │   └── adapter/
│   │   │       ├── controller/            # AuthController · NotificationController
│   │   │       │                          # UserController · GlobalExceptionHandler
│   │   │       └── dto/                   # RegisterRequest · LoginRequest · AuthResponse
│   │   │                                  # SendNotificationRequest · NotificationResponse
│   │   │                                  # UserResponse · PageResponse
│   │   └── AppNotificationApplication.java
│   ├── src/main/resources/
│   │   ├── db/migration/                  # V1__create_users_table.sql
│   │   │                                  # V2__create_notifications_table.sql
│   │   └── application.yml
│   ├── src/test/java/com/appnotification/
│   │   ├── application/service/           # AuthServiceTest · NotificationServiceTest · UserServiceTest
│   │   └── infrastructure/security/       # JwtServiceTest
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/                    # notification-bell · notification-list
│   │   │                                  # send-notification-form · user-card · protected-route
│   │   ├── hooks/                         # use-sse
│   │   ├── lib/                           # api-client · date-utils
│   │   ├── pages/                         # login-page · register-page · dashboard-page
│   │   ├── store/                         # auth-store · notification-store
│   │   ├── types/                         # index.ts
│   │   └── App.tsx
│   ├── nginx.conf
│   ├── Dockerfile
│   └── package.json
└── docker-compose.yml
```

---

## Testes

Testes unitários implementados no backend com **JUnit 5 + Mockito** — sem Spring context, execução em ~6s.

| Classe de Teste | Testes | Cenários |
|---|---|---|
| `AuthServiceTest` | 6 | Registro único, email duplicado, username duplicado, login válido, usuário não encontrado, senha incorreta |
| `NotificationServiceTest` | 8 | Envio + persistência + publicação, sender inexistente, listagem paginada, página vazia, markAsRead válido, notificação não encontrada, acesso negado, contagem de não lidas |
| `UserServiceTest` | 4 | Lista exceto self, lista vazia, findById encontrado, findById inexistente |
| `JwtServiceTest` | 6 | Geração de token, extração de userId, extração de email, token válido, token expirado, token malformado |

```bash
cd backend
mvn test
```

---

## Pré-requisitos

Para executar via Docker (recomendado):
- [Docker](https://www.docker.com/) 24+
- [Docker Compose](https://docs.docker.com/compose/) v2+

Para desenvolvimento local:
- Java 21+
- Maven 3.9+
- Node.js 20+
- PostgreSQL 16
- RabbitMQ 3

---

## Como Executar

### Com Docker Compose (recomendado)

```bash
docker compose up --build
```

Aguarde todos os serviços subirem. Os healthchecks do PostgreSQL e RabbitMQ são verificados automaticamente antes do backend iniciar.

| Serviço | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| RabbitMQ Management | http://localhost:15672 |

> Credenciais RabbitMQ Management: `appuser` / `apppassword`

---

### Desenvolvimento Local

**1. Suba apenas a infraestrutura:**

```bash
docker compose up postgres rabbitmq
```

**2. Execute o backend:**

```bash
cd backend
mvn spring-boot:run
```

**3. Execute o frontend:**

```bash
cd frontend
npm install
npm run dev
```

| Serviço | URL |
|---|---|
| Frontend (dev) | http://localhost:5173 |
| Backend API | http://localhost:8080 |

---

## API

> Todos os endpoints, exceto `/api/auth/**`, exigem o header: `Authorization: Bearer <token>`

### Autenticação — `/api/auth`

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/auth/register` | Cadastra novo usuário |
| `POST` | `/api/auth/login` | Autentica e retorna JWT |

**Cadastro:**
```json
{
  "username": "joao",
  "email": "joao@email.com",
  "password": "senha123"
}
```

**Login:**
```json
{
  "email": "joao@email.com",
  "password": "senha123"
}
```

**Resposta (ambos):**
```json
{
  "token": "<jwt>",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "joao",
    "email": "joao@email.com",
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

---

### Usuários — `/api/users`

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/users` | Lista todos os usuários (exceto o próprio) |
| `GET` | `/api/users/me` | Retorna o usuário autenticado |

---

### Notificações — `/api/notifications`

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/notifications/send` | Envia notificação para um usuário |
| `GET` | `/api/notifications` | Lista notificações recebidas (paginado) |
| `PATCH` | `/api/notifications/{id}/read` | Marca notificação como lida |
| `GET` | `/api/notifications/unread-count` | Retorna contagem de não lidas |
| `GET` | `/api/notifications/stream` | Abre stream SSE para recebimento em tempo real |

**Envio de notificação:**
```json
{
  "recipientId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Olá, tudo bem?",
  "type": "INFO"
}
```

> Tipos disponíveis: `INFO` · `WARNING` · `SUCCESS` · `ERROR`

**Resposta de notificação:**
```json
{
  "id": "...",
  "sender": { "id": "...", "username": "maria" },
  "message": "Olá, tudo bem?",
  "type": "INFO",
  "read": false,
  "createdAt": "2024-01-01T10:00:00Z"
}
```

**Resposta paginada (`GET /api/notifications`):**
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "last": false
}
```

---

## Schema do Banco de Dados

Gerenciado pelo **Flyway**. O Hibernate opera apenas em modo `validate` — nunca altera o schema.

```sql
-- V1: Usuários
CREATE TABLE users (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- V2: Notificações
CREATE TABLE notifications (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id    UUID         NOT NULL REFERENCES users(id),
    recipient_id UUID         NOT NULL REFERENCES users(id),
    message      VARCHAR(500) NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient_id ON notifications(recipient_id);
CREATE INDEX idx_notifications_created_at   ON notifications(created_at DESC);
```

---

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/app_notification` | URL JDBC do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `appuser` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | `apppassword` | Senha do banco |
| `SPRING_RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `SPRING_RABBITMQ_PORT` | `5672` | Porta AMQP |
| `SPRING_RABBITMQ_USERNAME` | `appuser` | Usuário do RabbitMQ |
| `SPRING_RABBITMQ_PASSWORD` | `apppassword` | Senha do RabbitMQ |
| `JWT_SECRET` | _(ver application.yml)_ | Chave secreta HS256 (mínimo 256-bit) |
| `JWT_EXPIRATION_MS` | `86400000` | Tempo de expiração do JWT em ms (24h) |

---

## Decisões Arquiteturais

**Clean Architecture**
O backend é organizado em camadas concêntricas com regra de dependência estrita. A camada `domain` contém POJOs puros sem nenhuma anotação de framework. Os use cases dependem apenas de interfaces (`port/output`), nunca de implementações concretas — JPA, RabbitMQ, BCrypt e JWT são detalhes de infraestrutura, substituíveis sem tocar na lógica de negócio.

**Ports de Input e Output**
Controllers chamam interfaces (`RegisterUserPort`, `SendNotificationPort`...) — nunca os use cases diretamente. Isso mantém a presentation desacoplada da application e facilita testes unitários sem Spring context.

**Entidades de Domínio como POJOs**
`User` e `Notification` são classes Java simples sem `@Entity`, sem Lombok, sem Spring. O comportamento de domínio (`notification.markAsRead()`, `notification.belongsTo(userId)`) vive na entidade, não nos serviços. As entidades JPA (`UserJpaEntity`, `NotificationJpaEntity`) existem separadamente na camada de infraestrutura.

**Virtual Threads (Java 21)**
Habilitados via `spring.threads.virtual.enabled: true`. Cada thread de requisição do Tomcat torna-se uma Virtual Thread, maximizando throughput em operações I/O-bound sem configuração adicional de pool.

**Filas RabbitMQ Dinâmicas por Usuário**
Uma fila dedicada (`notification.user.<uuid>`) é criada apenas quando o usuário estabelece a conexão SSE. Isso evita acúmulo ilimitado de filas para usuários offline.

**AOP para Cross-cutting Concerns**
O `ExecutionTimeAspect` intercepta todos os beans `@Service` via `within(@Service *)`. A lógica de negócio nunca é poluída com código de monitoramento — alinhado com o princípio de Separação de Responsabilidades.

**SSE em vez de WebSocket**
Para push unidirecional (servidor → cliente), o SSE é mais simples, usa HTTP padrão e funciona nativamente em proxies HTTP/1.1. O Nginx está configurado com `proxy_buffering off` e `chunked_transfer_encoding on` para garantir o streaming correto.
