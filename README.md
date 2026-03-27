# 🌾 Barleyssal — Spring Boot Backend

> **모의 주식 거래 플랫폼**의 메인 백엔드 서버입니다.  
> 회원 인증, 계좌·주문 관리, 관심종목, 거래 통계 등 핵심 비즈니스 로직을 담당합니다.

<br/>

## 📌 관련 레포지토리

| 서버 | 역할 | 링크 |
|------|------|------|
| **Spring Boot** (현재) | 메인 비즈니스 로직 API 서버 | — |
| **Go** | 실시간 시세·호가·주문 매칭 게이트웨이 | [barleyssal-go](https://github.com/hkmin3827/barleyssal-go) |
| **React** | 프론트엔드 SPA | [barleyssal-react](https://github.com/hkmin3827/barleyssal-react-vite) |

<br/>
## Vercel Link

[⭐ Barleyssal 홈페이지 바로 가기](https://barleyssal.vercel.app/)

#### ⚠️ 현재, 배포과정에서 비용 문제로 k3s 기반 전체 배포가 중단되었습니다.

#### ⚠️ Docker-compose ▷ Go와 Redis만 AWS EC2f로 배포 진행되었으니 홈페이지와 종목 탭의 종목리스트, 종목 상세페이지만 이용가능합니다.

#### ⚠️ 종목 상세의 1/5/10/30분봉의 분봉 차트는 내부 Redis 저장 데이터로 자체적으로 그리고 있으니, 3/25기준 배포로 데이터가 없을 수 있습니다.


#### 🚫 로그인, 회원가입 등 인증 & 관심종목 페이지, 엘라스틱서치 기반 랭킹 탭은 이용 불가하니 양해 부탁드립니다.

<br/>

## 🏗️ 시스템 아키텍처

```
[React Client]
     │  REST / Cookie Session
     ▼
[Spring Boot :8080]        ──Kafka──▶        [Go Market Gateway :4000]
     │                                            │
     ├── PostgreSQL (JPA + Flyway)                ├── Redis (시세 캐시, 호가 큐)
     ├── Redis (세션, 분산락, 계좌/주문, Rate-Limit) ├── KIS WebSocket (한국투자증권 실시간 API)
     ├── Kafka (주문 이벤트 발행, 체결 이벤트 소비)    └── Kafka (체결 이벤트 발행, 주문 이벤트 소비)
     └── Elasticsearch (거래 통계)                 
```

### 주문 흐름

```
클라이언트 → Spring Boot (주문 생성 & 검증)
           → Kafka [order.created] 발행
           → Go 매칭 엔진 (Redis 호가큐 매칭)
           → Kafka [execution.event] 발행
           → Spring Boot (체결 이벤트 소비 → DB 반영 & Elasticsearch 저장)
```

<br/>

## 🛠️ 기술 스택

| 분류                 | 기술                                                 |
|--------------------|----------------------------------------------------|
| Language           | Java 25 (Virtual Threads)                          |
| Framework          | Spring Boot 4.0.3, Spring Security, Spring Data JPA |
| Database           | PostgreSQL (HikariCP, Flyway 마이그레이션)               |
| Cache / Session    | Redis (Lettuce, Spring Session)                    |
| Distributed Lock   | Redisson                                     |
| Message Broker     | Apache Kafka                                       |
| Search / Analytics | Elasticsearch                                      |
| Auth               | JWT + HTTP Session (Cookie-based)                  |
| Build              | Gradle                                             |

<br/>

## 📂 프로젝트 구조

```
src/main/java/com/hakyung/barleyssal_spring/
├── interfaces/
│   ├── auth/            # 인증 (로그인·회원가입·비밀번호찾기)
│   ├── user/            # 사용자 프로필
│   ├── account/         # 계좌·보유 종목
│   ├── order/           # 주문 생성·취소·조회
│   ├── watchlist/       # 관심종목
│   ├── Stats/           # 거래 통계 (Elasticsearch)
│   └── admin/           # 관리자 (회원 관리)
├── application/
├── domain/              # 도메인 모델
│   ├── user/
│   ├── account/
│   ├── order/
│   ├── holding/
│   ├── watchlist/
│   └── common/VO/       # Money, StockCode VO
├── infrastructure/ 
│   ├── kafka/           # 주문 이벤트 Producer / 체결 이벤트 Consumer / DLQ 관리
│   ├── redis/           # 계좌·주문·시세 Redis 저장소
│   ├── elastic/         # 거래 / 주문 도큐먼트·쿼리
│   ├── scheduling/      # 스케쥴링 작업 (주문 만료, 정리, 복구, 엘라스틱 저장)
│   └── go/              # Go 서버 HTTP Client
└── global/
    ├── config/          # 설정 (Security, Kafka, Redis, CORS 등)
    ├── jwt/             # JWT 발급·파싱
    ├── security/        # Spring Security 필터·UserDetails
    ├── lock/            # 분산락 AOP (@DistributedLock)
    ├── ratelimit/       # Rate-Limit 인터셉터
    └── exception/       # 전역 예외 처리
```

<br/>

## 📋 API 기능 명세

### 🔐 인증 API (`/api/v1/auth`)

| Method | Endpoint | 인증 필요 | 설명                                        |
|--------|----------|:---------:|-------------------------------------------|
| `POST` | `/login` | ❌ | 이메일 + 비밀번호로 로그인. JWT를 세션에 저장하고 CSRF 토큰 발급 |
| `POST` | `/signup` | ❌ | 신규 회원 가입. 가입 즉시 세션 발급                     |
| `POST` | `/logout` | ✅ | 세션 무효화 및 인증 쿠키 삭제                         |
| `POST` | `/withdraw` | ✅ | 비밀번호 확인 후 회원 탈퇴 (soft delete)             |
| `POST` | `/password/forgot` | ❌ | 이메일로 비밀번호 재설정 링크 발송                       |
| `POST` | `/password/reset` | ❌ | 재설정 토큰으로 새 비밀번호 저장                        |
| `GET`  | `/csrf` | ❌ | CSRF 토큰 초기화 (SPA 최초 로드 시 호출)              |

---

### 👤 사용자 API (`/api/v1/users`)

| Method | Endpoint | 인증 필요 | 설명 |
|--------|----------|:---------:|------|
| `GET` | `/me` | ✅ | 로그인한 사용자 상세 정보 조회 |
| `POST` | `/me/password-verify` | ✅ | 현재 비밀번호 유효성 확인 |
| `PATCH` | `/me` | ✅ | 닉네임·전화번호 등 프로필 수정 |
| `POST` | `/me/password-change` | ✅ | 비밀번호 변경 (신규 비밀번호 + 확인) |

---

### 💳 계좌 API (`/api/v1/accounts`)

| Method | Endpoint | 인증 필요 | 설명 |
|--------|----------|:---------:|------|
| `GET` | `/me` | ✅ | 내 계좌 조회 (없으면 자동 생성) |
| `PUT` | `/set-principal` | ✅ | 투자 원금 설정 (Rate-Limit: 2회/초) |
| `GET` | `/me/holdings` | ✅ | 보유 종목 목록 조회 (종목코드·수량·평균가) |

---

### 📈 주문 API (`/api/v1/orders`)

| Method | Endpoint | 인증 필요 | 설명 |
|--------|----------|:---------:|------|
| `POST` | `/` | ✅ | 주문 생성 (지정가/시장가, 매수/매도). Rate-Limit: 2회/초. 분산락으로 동시 주문 방지 |
| `POST` | `/{orderId}/cancel` | ✅ | 주문 취소 (PENDING·SUBMITTED 상태만 가능) |
| `GET` | `/` | ✅ | 내 주문 목록 전체 조회 |
| `GET` | `/{orderId}` | ✅ | 특정 주문 상세 조회 |

**주문 상태 흐름**
```
PENDING → SUBMITTED → FILLED
      ↘ CANCELLED  ↘ CANCELLED
                    ↘ REJECTED (잔고 부족) -> 현재 주문 생성 시 Redis 현재가 체크로 인해 처리 로직 제외
                    ↘ EXPIRED  (만료 스케줄러)
```

---

### ⭐ 관심종목 API (`/api/v1/watchlist`)

| Method | Endpoint | 인증 필요 | 설명 |
|--------|----------|:---------:|------|
| `GET` | `/` | ✅ | 관심종목 목록 조회 |
| `POST` | `/{stockCode}` | ✅ | 관심종목 추가 (종목명 body로 전달 가능) |
| `DELETE` | `/{stockCode}` | ✅ | 관심종목 삭제 |
| `POST` | `/{stockCode}/toggle` | ✅ | 추가/삭제 토글 (응답: `{ watched, stockCode }`) |
| `GET` | `/{stockCode}/status` | ✅ | 특정 종목 관심 등록 여부 확인 |

---

### 📊 통계 API (`/api/v1/stats`)  *(Elasticsearch 기반)*

| Method | Endpoint | 인증 필요 | 설명 |
|--------|----------|:-----:|------|
| `GET` | `/top-profitable` |   ❌   | 최근 14일 수익률 상위 거래 목록 |
| `GET` | `/popular-stocks` |   ❌   | 최근 14일 가장 많이 거래된 종목 Top N |
| `GET` | `/daily-efficiency` |   ❌   | 최근 14일 일별 주문 효율(체결률) 통계 |
| `GET` | `/admin/hourly-trade-volume` |   ❌   | 최근 14일 시간대별 거래량 통계 |

---

### 🔧 관리자 API (`/api/v1/admin/users`)  *(ROLE_ADMIN 전용)*

| Method | Endpoint | 인증 필요 | 설명 |
|--------|----------|:---------:|------|
| `GET` | `/` | ✅ Admin | 활성/비활성 회원 목록 페이지 조회 (`?active=true`, 페이지 기본 20건) |
| `GET` | `/{userId}` | ✅ Admin | 특정 회원 상세 정보 조회 |
| `PATCH` | `/{userId}/activate` | ✅ Admin | 회원 활성화 |
| `PATCH` | `/{userId}/deactivate` | ✅ Admin | 회원 비활성화 |

<br/>

## ⚙️ 주요 기술 구현 포인트

### 분산락 (`@DistributedLock`)
Redis 기반 AOP 분산락으로 동일 사용자의 동시 주문을 방지합니다.
```java
@DistributedLock(key = "'order:user:' + #userId", waitTime = 0, leaseTime = 3)
public OrderResponse createOrder(Long userId, CreateOrderRequest req) { ... }
```

### Rate Limiting
Redis Sliding Window 방식으로 엔드포인트별 요청 빈도를 제한합니다.
```java
@RateLimit(maxRequests = 2, windowSeconds = 1)
@PostMapping
public ResponseEntity<OrderResponse> placeOrder(...) { ... }
```

### Kafka 이벤트 흐름
- **Producer**: 주문 생성 시 `order.created` 토픽으로 `OrderCreatedEvent` 발행
- **Consumer**: Go 서버의 체결 결과(`execution.event`)를 구독해 DB 반영 및 Elasticsearch 저장
- **DLQ**: 처리 실패 이벤트는 Dead Letter Queue로 이동하여 유실 방지

### 스케줄러
| 스케줄러 | 주기 | 역할                          |
|----------|------|-----------------------------|
| `OrderBulkExpireScheduler` | 주기적 | 장 종료 후 미체결 주문 일괄 EXPIRED 처리 |
| `OrderRecoveryScheduler` | 주기적 | Redis→DB 동기화 누락된 주문 복구      |
| `MarketCleanupScheduler` | 주기적 | 장 종료 후 주문 데이터 정리            |
| `TestUserResetScheduler` | 주기적 | 테스트 계정 데이터 초기화              |

<br/>

## 🗄️ 데이터베이스 스키마 (ERD 요약)

```
users ──────────── accounts ──────────── holdings
  │                    │                (account_id, stock_code, qty, avg_price)
  │                    └──────────────── orders
  │                                     (order_side, order_type, order_status, ...)
  └── watchlists
      (user_id, stock_code, stock_name)
```

Flyway를 통한 버전 관리: `src/main/resources/db/migration/V1__init_schema.sql`

<br/>

## 🔒 보안 설계

- **인증**: JWT를 서버 세션(Redis)에 저장하는 방식 (Stateless JWT가 아닌 Session-backed JWT)
- **CSRF 방어**: `X-XSRF-TOKEN` 헤더 검증 (SPA에서 `/api/v1/auth/csrf` 로 토큰 발급)
- **쿠키 설정**: `HttpOnly`, `Secure`, `SameSite=Lax`
- **비밀번호**: BCrypt 해싱
- **세션 TTL**: 30분 (Redis Spring Session)
