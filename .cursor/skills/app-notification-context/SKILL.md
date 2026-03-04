---
name: app-notification-context
description: Contexto completo do projeto App Notification Async. Use este skill em toda interação neste projeto para obter contexto de arquitetura, stack, estrutura de pacotes, fluxo de notificação, decisões técnicas e padrões de código adotados.
---

# App Notification Async — Contexto do Projeto

## Propósito

Aplicação fullstack de **envio de notificações entre usuários em tempo real**. Um usuário autenticado envia uma notificação para outro; a entrega é assíncrona via RabbitMQ e o push ao browser é feito via SSE.

---

## Stack

### Backend
- Java 21 — Virtual Threads habilitados: `spring.threads.virtual.enabled: true`
- Spring Boot 3.3.4
- Spring Security 6 — JWT stateless (jjwt 0.12.6)
- Spring Data JPA — PostgreSQL 16 (camada de infraestrutura, não domínio)
- Spring AMQP — RabbitMQ 3 (TopicExchange, filas dinâmicas por usuário)
- Spring AOP — AspectJ (cross-cutting concerns)
- Flyway 10 — único responsável pelo schema (`ddl-auto: validate`)
- Lombok: apenas em `infrastructure` e `presentation` — nunca em `domain`

### Frontend
- React 18 + TypeScript 5.5 + Vite 5.4
- TailwindCSS 3.4
- Zustand 5.0 (estado global)
- Axios 1.7 (cliente HTTP com interceptors JWT)
- React Router 6.26
- Framer Motion 11.5
- react-hot-toast 2.4
- lucide-react 0.441

### Infraestrutura
- Docker Compose: `postgres`, `rabbitmq`, `backend`, `frontend`
- Nginx como reverse proxy: `/api/*` → `backend:8080`, suporte a SSE (`proxy_buffering off`)

---

## Arquitetura — Clean Architecture (Camadas Concêntricas)

**Regra de dependência:** cada camada só conhece as internas. `domain` não conhece nada. `application` conhece apenas `domain`. `infrastructure` e `presentation` conhecem `application` e `domain`. Nunca o contrário.

```
domain/
  └── entity/
        User               — POJO puro, sem anotações de framework
        Notification       — métodos: markAsRead(), belongsTo(UUID)
        NotificationType   — enum: INFO, WARNING, SUCCESS, ERROR

application/
  ├── port/
  │   ├── input/           — interfaces que os controllers chamam
  │   │   ├── auth/          RegisterUserPort (Command/Result), LoginUserPort (Command/Result)
  │   │   ├── notification/  SendNotificationPort, GetNotificationsPort,
  │   │   │                  MarkNotificationAsReadPort, CountUnreadNotificationsPort
  │   │   └── user/          ListUsersPort
  │   └── output/          — contratos que a infraestrutura implementa
  │         UserRepositoryPort, NotificationRepositoryPort,
  │         NotificationPublisherPort, PasswordEncoderPort,
  │         TokenGeneratorPort, SsePort
  └── usecase/             — implementam port/input, dependem de port/output
      ├── auth/              RegisterUserUseCase, LoginUserUseCase
      ├── notification/      SendNotificationUseCase, GetNotificationsUseCase,
      │                      MarkNotificationAsReadUseCase, CountUnreadNotificationsUseCase
      └── user/              ListUsersUseCase

infrastructure/            — implementam port/output
  ├── persistence/
  │   ├── entity/            UserJpaEntity, NotificationJpaEntity  (@Entity com Lombok)
  │   ├── repository/        UserJpaRepository, NotificationJpaRepository (Spring Data)
  │   └── adapter/           UserRepositoryAdapter, NotificationRepositoryAdapter
  ├── messaging/             NotificationPublisherAdapter, NotificationListenerManager,
  │                          NotificationMessage (record — payload para RabbitMQ)
  ├── security/              JwtService, JwtAuthFilter,
  │                          PasswordEncoderAdapter, TokenGeneratorAdapter
  ├── sse/                   SseEmitterRegistry
  ├── aop/                   @LogExecutionTime + ExecutionTimeAspect
  └── config/                RabbitMQConfig, SecurityConfig

presentation/
  └── adapter/
      ├── controller/        AuthController, NotificationController,
      │                      UserController, GlobalExceptionHandler
      └── dto/               RegisterRequest, LoginRequest, AuthResponse,
                             SendNotificationRequest, NotificationResponse,
                             UserResponse, PageResponse  (todos records Java)
```

---

## Fluxo de Notificação

```
POST /api/notifications/send
  → NotificationController
  → SendNotificationPort.execute(Command)
  → SendNotificationUseCase
      ├── userRepository.findById(senderId)      via UserRepositoryPort
      ├── userRepository.findById(recipientId)   via UserRepositoryPort
      ├── notificationRepository.save()          via NotificationRepositoryPort
      └── notificationPublisher.publish()        via NotificationPublisherPort
              → NotificationPublisherAdapter → RabbitMQ
                  exchange: notification.exchange
                  routing key: notification.<recipientId>
                  payload: NotificationMessage (record)
  → NotificationListenerManager
      (fila dinâmica: notification.user.<uuid>, criada no SSE connect)
  → SseEmitterRegistry.sendToUser(userId, NotificationMessage)
      evento SSE: name="notification"
  → Browser: useSSE hook (EventSource)
      → useNotificationStore (Zustand) → UI
      → react-hot-toast → toast
```

---

## Banco de Dados

```sql
-- V1
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- V2
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id),
    recipient_id UUID NOT NULL REFERENCES users(id),
    message VARCHAR(500) NOT NULL,
    type VARCHAR(20) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_recipient_id ON notifications(recipient_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
```

---

## API Endpoints

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Cadastro |
| POST | `/api/auth/login` | Public | Login → JWT |
| GET | `/api/users` | Bearer | Lista usuários (exceto self) |
| GET | `/api/users/me` | Bearer | Usuário autenticado |
| POST | `/api/notifications/send` | Bearer | Envia notificação |
| GET | `/api/notifications` | Bearer | Histórico paginado |
| PATCH | `/api/notifications/{id}/read` | Bearer | Marca como lida |
| GET | `/api/notifications/unread-count` | Bearer | Contagem não lidas |
| GET | `/api/notifications/stream` | Bearer (query param) | SSE stream |

---

## Testes

24 testes unitários — JUnit 5 + Mockito, sem Spring context.

| Classe | Testes | Foco |
|---|---|---|
| `AuthServiceTest` | 6 | RegisterUserUseCase + LoginUserUseCase |
| `NotificationServiceTest` | 8 | Send, Get, MarkAsRead, CountUnread |
| `UserServiceTest` | 4 | ListUsersUseCase |
| `JwtServiceTest` | 6 | Geração, extração, validação de token |

---

## Padrões de Código

### Backend
- `domain/entity`: POJOs puros — sem `@Entity`, sem Lombok, sem Spring
- DTOs na `presentation/adapter/dto`: sempre `record` Java
- Use cases: `@Transactional(readOnly = true)` em queries, `@Transactional` em mutations
- Ports de input: `Command`/`Query` e `Result` como records internos da interface
- AOP: lógica de infraestrutura (timing, logging) nunca dentro de use cases
- Virtual Threads: não criar `ExecutorService` customizado — já configurado globalmente
- Lombok: apenas em `infrastructure` (JPA entities) e `usecase`/`controller` para injeção

### Frontend
- kebab-case para nomes de arquivo (`notification-bell.tsx`)
- `const` em vez de `function`
- Prefixo `handle` em event handlers (`handleClick`, `handleSubmit`)
- Tailwind para toda estilização — sem CSS inline
- Early returns para reduzir aninhamento
- Acessibilidade: `aria-label`, `tabIndex`, handlers de teclado

---

## Débitos Técnicos Conhecidos

1. **Múltiplas sessões por usuário** — `SseEmitterRegistry` usa `Map<UUID, SseEmitter>`: segunda aba sobrescreve a primeira.
2. **`stopListening` nunca chamado** — fila RabbitMQ e container ficam ativos após SSE fechar. O `onCompletion` do emitter deveria acionar `NotificationListenerManager.stopListening(userId)`.
3. **Exceções genéricas** — `IllegalArgumentException` usada como exceção de domínio. Evoluir para `ResourceNotFoundException` e `DomainException` customizadas no `domain`.
4. **Frontend sem testes** — nenhum teste unitário implementado (Vitest + Testing Library).
5. **SSE token via query param** — limitação nativa da API `EventSource` (não suporta headers customizados).

---

## Variáveis de Ambiente

| Variável | Padrão |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/app_notification` |
| `SPRING_DATASOURCE_USERNAME` | `appuser` |
| `SPRING_DATASOURCE_PASSWORD` | `apppassword` |
| `SPRING_RABBITMQ_HOST` | `localhost` |
| `SPRING_RABBITMQ_PORT` | `5672` |
| `SPRING_RABBITMQ_USERNAME` | `appuser` |
| `SPRING_RABBITMQ_PASSWORD` | `apppassword` |
| `JWT_SECRET` | _(ver application.yml)_ |
| `JWT_EXPIRATION_MS` | `86400000` (24h) |
