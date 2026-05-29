<div align="center">
  <h3 align="center">RunGo 🏃🏻‍♂️</h3>
  <p align="center">
    마라톤 통합 플랫폼 서비스
  </p>
</div>
<br>

<details open>
  <summary><strong>&nbsp;📖&nbsp;목차</strong></summary>

1. &nbsp;&nbsp;[🔍 서비스 소개](#-서비스-소개)
2. &nbsp;&nbsp;[📄 API 명세서](#-api-명세서)
3. &nbsp;&nbsp;[💡 기술 스택](#-기술-스택)
4. &nbsp;&nbsp;[🗂️ 데이터베이스](#%EF%B8%8F-데이터베이스)
5. &nbsp;&nbsp;[💻 시스템 아키텍처](#-시스템-아키텍처)
6. &nbsp;&nbsp;[💡 핵심 설계 의사결정](#-핵심-설계-의사결정)
7. &nbsp;&nbsp;[🚀 3차 추가 기능](#-3차-추가-기능)
8. &nbsp;&nbsp;[🤝 깃 컨벤션](#-깃-컨벤션)
9. &nbsp;&nbsp;[📂 프로젝트 구조](#-프로젝트-구조)
10. &nbsp;&nbsp;[🎬 실행하기](#-실행하기)
11. &nbsp;&nbsp;[👨‍👩‍👧‍👧 역할 분담](#%E2%80%8D%E2%80%8D%E2%80%8D-역할-분담)
    

</details>
<br>

## 🔍 서비스 소개

### 배경
국내 마라톤을 즐겨하는 인구는 꾸준히 증가하고 있으며, 매년 수백 개의 대회가 전국에서 개최되고 있습니다.
그러나 현재 마라톤 대회 정보는 여러 커뮤니티, 블로그 등에 분산되어 있어 참여자와 주최자가 모두 불편을 겪고 있습니다.

**RunGo**는 흩어져 있는 마라톤 대회 정보를 한곳에서 조회하고, 참가자가 원하는 코스에 접수할 수 있도록 돕는 마라톤 통합 플랫폼입니다.

### 주요 기능

- 마라톤 대회 조회 / 접수 / 운영
- 토스페이먼츠 기반 결제
- OAuth2(Google) 로그인
- Redis 기반 Refresh Token 관리
- Redis Sorted Set 기반 접수 대기열 처리
- Outbox Pattern 기반 비동기 이메일 알림 (Gmail SMTP)
- 주최자 권한 신청 및 승인 / 거절
- 데이터 초기화 스케줄러
- GitHub Actions 기반 CI/CD 자동 배포
- k6 기반 부하 테스트

<br>

## 📄 API 명세서

- **Auth**
<img width="963" height="229" alt="Image" src="https://github.com/user-attachments/assets/1595db85-36bb-4421-a696-c398022923aa" />
<br>

- **Users**
<img width="965" height="157" alt="Image" src="https://github.com/user-attachments/assets/30f7f81d-1058-47dc-8bbf-67d3d305dca9" />
<br>

- **Marathon**
<img width="966" height="265" alt="Image" src="https://github.com/user-attachments/assets/cb70d673-3dbe-4f25-b065-55d9b302aacd" />
<br>

- **Registration**
<img width="959" height="153" alt="Image" src="https://github.com/user-attachments/assets/324a93bb-95b0-4c42-b512-673b2b121763" />
<br>

- **Payment**
<img width="961" height="80" alt="Image" src="https://github.com/user-attachments/assets/b4d2c6db-446c-4a23-a561-bea30db089e8" />
<br>

- **Organizer**
<img width="965" height="155" alt="Image" src="https://github.com/user-attachments/assets/935e181b-74db-419d-bd74-ba97c30070bf" />
<br>

<img width="963" height="84" alt="Image" src="https://github.com/user-attachments/assets/585f074a-f927-4892-9b8c-2e49d068b756" />
<br>

- **Admin**
<img width="972" height="198" alt="Image" src="https://github.com/user-attachments/assets/e28d5834-91e7-4e45-b2ba-494168072f47" />
<br>

## 💡 기술 스택

Frontend | Backend | Security | Database | Deployment | Monitoring | Collaboration & Docs
|:------:|:------:|:------:|:------:|:------:|:------:|:------:|
|<img src="https://img.shields.io/badge/Next.js-000000?style=flat-square&logo=nextdotjs&logoColor=white"/>|<img src="https://img.shields.io/badge/Java-007396?style=flat-square&logo=openjdk&logoColor=white"/><br><img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white"/><br><img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=flat-square&logo=springboot&logoColor=white"/><br><img src="https://img.shields.io/badge/JPA-59666C?style=flat-square&logo=hibernate&logoColor=white"/>|<img src="https://img.shields.io/badge/SpringSecurity-6DB33F?style=flat-square&logo=springsecurity&logoColor=white"/><br><img src="https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white"/><br><img src="https://img.shields.io/badge/OAuth2-3423A6?style=flat-square&logo=auth0&logoColor=white"/>|<img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white"/><br><img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white"/>|<img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white"/><br><img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white"/><br><img src="https://img.shields.io/badge/AWS_EC2-FF9900?style=flat-square&logo=amazonec2&logoColor=white"/><br><img src="https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white"/><br><img src="https://img.shields.io/badge/Vercel-000000?style=flat-square&logo=vercel&logoColor=white"/>|<img src="https://img.shields.io/badge/k6-7D64FF?style=flat-square&logo=k6&logoColor=white"/><br><img src="https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=prometheus&logoColor=white"/><br><img src="https://img.shields.io/badge/Grafana-F46800?style=flat-square&logo=grafana&logoColor=white"/>|<img src="https://img.shields.io/badge/Jira-0052CC?style=flat-square&logo=jira&logoColor=white"/><br><img src="https://img.shields.io/badge/Notion-000000?style=flat-square&logo=notion&logoColor=white"/><br><img src="https://img.shields.io/badge/Swagger-85EA2E?style=flat-square&logo=swagger&logoColor=black"/>|
```
- Frontend : Next.js
- Backend : Kotlin, Java, Spring Boot, Spring Data JPA(Hibernate)
- Security : Spring Security, JWT, OAuth2
- Database : MySQL, Redis
- Deployment : Docker, GitHub Actions, AWS EC2, Nginx, Vercel
- Monitoring : k6, Prometheus, Grafana
- Collaboration & Docs : Jira, Notion, Swagger
```

<br>

## 🗂️ 데이터베이스

<!-- ERD 이미지를 여기에 추가해주세요 -->
<img width="2247" height="1223" alt="Image" src="https://github.com/user-attachments/assets/53de66b1-1528-4435-91b6-1ddcd54a4d6d" />

<br>

## 💻 시스템 아키텍처

<!-- ### System -->

<img width="672" height="445" alt="Image" src="https://github.com/user-attachments/assets/5c9800ce-24af-46fc-ad4a-05d59cedc0f3" />

<!-- ### Network -->

<br>

## 💡 핵심 설계 의사결정

### 1. 대회 접수 동시성 제어

대회 접수 기능에서는 여러 사용자가 동시에 같은 코스에 신청할 경우 정원 초과 접수와 `currentCount` 정합성 문제가 발생할 수 있습니다.

동시성 제어 방식으로 **원자적 업데이트 vs 비관적 락**을 비교하여 원자적 업데이트 방식을 채택하였습니다.

**테스트 조건**
- 총 요청 수: 1,000명
- 동시 요청: 100 / 500
- 시나리오: 동일 코스 접수 요청

**결과**

| 동시 요청 | 원자적 업데이트 | 비관적 락 | 성능 차이 |
| --- | --- | --- | --- |
| 100 | 286ms | 328ms | 약 12.7% 빠름 |
| 500 | 1.04s | 1.26s | 약 17.5% 빠름 |

원자적 업데이트는 비관적 락보다 대기 시간이 적고, 낙관적 락처럼 재시도 정책을 두지 않아도 되기 때문에 성능과 구현 복잡도의 균형이 가장 좋다고 판단했습니다.

---

### 2. 중복 신청 방지

한 사용자는 동일한 대회에서 하나의 코스만 신청할 수 있도록 제한했습니다.

이를 위해 `user_id`와 `marathon_id`를 기준으로 복합 유니크 제약을 두는 방식을 채택했습니다.

```sql
UNIQUE (user_id, marathon_id)
```

복합 유니크 방식은 DB 레벨에서 중복 신청을 직접 막기 때문에, 동시 요청 상황에서도 애플리케이션에서 복잡한 중복 제어 로직을 두지 않아도 안정적으로 처리할 수 있습니다.

---

### 3. 접수 취소 및 재신청 데이터 설계

접수 취소 이후 사용자가 다시 신청할 수 있어야 하며, 동시에 기존 접수 이력도 관리할 수 있어야 합니다.

| 방식 | 장점 | 단점 |
| --- | --- | --- |
| 유니크 제약 제거 후 애플리케이션 제어 | 유연한 제어 가능 | 기존 복합 유니크 키를 제거해야 하며, 중복 방지를 애플리케이션 로직에 의존 |
| 하드 딜리트 + 이력 테이블 분리 | 재신청과 정합성 확보, 데이터 의미 명확 | 관리 테이블 증가 |
| `canceled_at` 포함 복합 유니크 | 재신청 가능 | `NULL` 처리와 기본값 사용으로 데이터 의미가 왜곡될 수 있음 |

최종적으로 **하드 딜리트 + 이력 테이블 분리** 방식을 채택했습니다. 현재 접수 데이터는 활성 접수 상태만 관리하고, 취소 이력은 별도 테이블에서 관리하는 구조가 데이터 의미가 명확하고 유지보수에 유리하다고 보았습니다.

---

### 4. Redis 기반 Refresh Token 관리

Refresh Token은 재발급과 로그아웃 처리를 위해 서버에서도 관리가 필요합니다.

| 항목 | RDB | Redis |
| --- | --- | --- |
| 저장 방식 | 디스크 기반 저장 | 메모리 기반 저장 |
| 만료 처리 | 별도 만료 처리 로직 필요 | TTL 기능으로 자동 만료 |
| 조회 방식 | SQL 기반 조회 | Key 기반 조회 |
| 속도 | 상대적으로 느림 | 빠름 |

Redis는 메모리 기반으로 빠르게 조회할 수 있고, TTL 기능을 통해 Refresh Token 만료 시간을 자동으로 관리할 수 있습니다. 확장성과 성능을 고려해 Redis를 도입했습니다.

---

### 5. Refresh Token 재발급 동시성 제어

동일한 사용자가 동시에 토큰 재발급을 요청하면 각각 새로운 Refresh Token이 발급되고, 마지막 요청이 Redis의 기존 값을 덮어쓸 수 있습니다. 이 경우 클라이언트가 가진 토큰과 서버에 저장된 토큰이 불일치하여 인증 오류가 발생할 수 있습니다.

| 방식 | 평균 응답 속도 | 성공률 | 안정성 | 확장성 | 프로젝트 적합성 |
| --- | --- | --- | --- | --- | --- |
| 낙관적 락 | 가장 빠름 | 높음 | 실패 발생 | 낮음 | 적합 |
| 비관적 락 | 빠름 | 낮음 | 대기 발생 | 낮음 | 부적합 |
| Redis 분산 락 | 느림 | 가장 높음 | 안정적 | 높음 | 적합 |

이미 Refresh Token을 Redis에 저장하고 있었기 때문에 별도의 인프라 추가 없이 Redis 기반 분산 락을 적용했습니다. 향후 서버 확장 시에도 Redis가 중앙에서 락을 관리할 수 있어 인스턴스 수와 관계없이 일관된 동시성 제어가 가능합니다.

---

### 6. Outbox Pattern 기반 비동기 이메일 처리

기존에는 도메인 이벤트 발생 시 `@Async`를 이용해 이메일을 즉시 발송하는 구조를 사용했습니다.

하지만 SMTP 서버 장애나 네트워크 오류 발생 시 실패 이력을 추적하기 어려웠고, 재시도 정책이 없어 일부 사용자가 알림을 수신하지 못할 가능성이 존재했습니다. 이를 해결하기 위해 이메일 발송 요청을 DB에 먼저 저장하고, 별도 Scheduler가 발송을 담당하는 **Outbox Pattern**을 도입했습니다.

| 항목 | 기존 구조 | 개선 구조 |
| --- | --- | --- |
| 실패 이력 관리 | 어려움 | 가능 |
| 재시도 처리 | 명확하지 않음 | 최대 3회 재시도 |
| 상태 추적 | 어려움 | PENDING, PROCESSING, SUCCESS, FAILED, EXHAUSTED |
| 장애 격리 | 어려움 | 가능 |
| 운영 안정성 | 낮음 | 높음 |

Outbox Pattern 도입을 통해 이메일 발송 상태를 데이터베이스에서 관리하고, 실패한 이메일을 재처리할 수 있는 구조를 구축했습니다.
재시도 횟수를 모두 소진한 이메일은 `EXHAUSTED` 상태로 전환하여 추가 재시도를 중단하고, 운영자가 장애 원인을 추적 및 수동 조치할 수 있도록 설계했습니다.

<br>

## 🚀 3차 추가 기능

### 1. Java → Kotlin 마이그레이션 전략

프로젝트의 유지보수성과 생산성을 높이기 위해 Java 기반 코드를 Kotlin으로 마이그레이션했습니다. 단순 문법 변환이 아니라 기존 서비스 기능을 유지하면서 Kotlin의 장점을 활용하는 방향으로 진행했습니다.

#### 마이그레이션 목표

- Null-Safety 기반 안정성 향상
- DTO/Data Class 기반 코드 간결화
- `val` 기반 불변성 표현
- Kotlin 문법을 활용한 가독성 개선
- 기존 서비스 기능 유지

#### 마이그레이션 순서

```text
DTO -> Controller -> Service -> Entity -> Repository / Status -> Test
```

#### 주요 개선 사항

| 개선 항목 | 설명 |
| --- | --- |
| Null-Safety | nullable 타입을 명시하여 NPE 가능성을 줄이고 컴파일 단계에서 null 처리를 강제 |
| Data Class | Java의 생성자, getter/setter, equals/hashCode, toString 등 반복 코드 감소 |
| 불변성 표현 | 요청/응답 DTO를 `val` 중심으로 작성하여 불필요한 상태 변경 방지 |
| Named Argument | 팩토리 메서드 호출 시 인자 의미를 명확히 표현 |
| Kotlin 문법 활용 | `when`, scope function, import alias 등을 적용하여 코드 가독성 향상 |

#### LOC(Line Of Code) 개선 결과

| 구분 | 변경 전 | 변경 후 | 감소율 |
| --- | --- | --- | --- |
| Controller | 581 | 535 | -7.9% |
| DTO | 1,577 | 1,519 | -3.6% |
| Entity | 741 | 716 | -3.3% |
| Service | 1,350 | 1,019 | -24.5% |
| 평균 | - | - | -10.8% |

Kotlin 전환을 통해 전체적으로 약 10.8%의 코드 감소 효과를 확인했으며, 특히 Service 계층에서 가장 큰 감소 효과를 얻었습니다.

---

### 2. Redis 기반 접수 대기열

인기 마라톤 대회 접수 시 짧은 시간에 많은 요청이 몰리면 DB Connection Pool 포화, 응답 지연, 접수 순서 보장 문제 등이 발생할 수 있습니다.

이를 해결하기 위해 기존 접수 API 뒤에 서버 내부 대기열을 적용하고, 코스별 요청을 Redis Sorted Set에 적재한 뒤 Worker가 순차적으로 소비하는 구조를 도입했습니다.

#### 구현 범위

- 기존 접수 API 뒤에 서버 내부 대기열 적용
- 코스별 요청을 Redis Sorted Set에 적재
- 대기열에서 순서대로 요청을 꺼내 Worker가 처리
- 여러 Worker가 나누어 처리하되 선착순 의미를 유지

#### Redis Sorted Set 선택 이유

| 이유 | 설명 |
| --- | --- |
| 순번 조회에 유리 | `ZRANK`로 현재 순번 확인 가능 |
| 대기 인원 계산 가능 | `ZCARD`와 조합해 앞/뒤 대기 인원 계산 가능 |
| 중간 사용자 제거 가능 | `ZREM`으로 취소/이탈 사용자 제거 가능 |
| 운영 유연성 | Kafka 같은 MQ보다 단순한 구조로 현재 요구사항에 적합 |

#### 구현 결과

- Worker 동시 처리 수 500에서는 DB Connection Pool 포화 현상 확인
- Worker 동시 처리 수 50에서는 안정적인 처리 흐름 확인
- Redis Queue 적재 및 소비 정상 동작 확인
- 선착순 처리 흐름 보장 확인

---

### 3. 결제 기능 도입

기존 접수 흐름은 정원 검증 후 바로 접수가 확정되는 구조였습니다. 3차 프로젝트에서는 유료 마라톤 접수를 지원하기 위해 토스페이먼츠 기반 결제 기능을 도입했습니다.

#### 기존 흐름

```text
마라톤 신청 -> 정원 검증 -> 접수 확정
```

#### 변경 흐름

```text 
마라톤 신청 -> 정원 검증 -> 접수 생성 (결제 대기 상태) -> 결제 완료 -> 접수 확정
```

#### 결제 승인 흐름

```text
프론트 결제 성공 callback -> 백엔드 confirm 요청 -> Toss Payments 승인 검증 -> 결제 완료 -> 접수 확정
```

#### 결제 안정성 설계

| 항목 | 설명 |
| --- | --- |
| 중복 요청 방지 | 동일 결제 요청은 한 번만 처리 |
| 비관적 락 적용 | 결제 상태 변경 중 race condition 방지 |
| 결제 만료 처리 | 일정 시간 초과 시 결제 대기 상태 만료 |
| 이력 보존 | 결제와 접수를 분리하여 결제 데이터 추적 가능 |

---

### 4. 데이터 초기화 스케줄러

운영/테스트 과정에서 접수, 취소 이력, 종료된 대회 데이터가 계속 누적되면 DB 부하가 증가하고 관리가 어려워질 수 있습니다.

이를 해결하기 위해 일정 기준이 지난 데이터를 자동으로 정리하는 데이터 초기화 스케줄러를 구현했습니다.

#### 구현 방식

- Spring Scheduler 기반 자동 실행
- 매일 오전 3시 실행
- Spring Transaction 기반 원자적 처리
- FK 제약 조건을 고려한 삭제 순서 적용

#### 삭제 흐름

```text
현재 날짜 기준 5년 이상 지난 대회 존재 여부 확인
 ↓
접수 취소 이력 삭제
 ↓
대회 접수 이력 삭제
 ↓
마라톤 대회 삭제
```

#### 삭제 순서 기준

FK 제약 조건으로 인해 부모 테이블인 마라톤 대회를 먼저 삭제할 수 없기 때문에, 자식 데이터인 접수 취소 이력과 접수 이력을 먼저 삭제한 뒤 마라톤 대회를 삭제하도록 설계했습니다.

---

### 5. 관리자 권한 신청 및 관리 기능

기존에는 관리자가 직접 특정 사용자의 역할을 변경해야 했습니다. 이 방식은 사용자의 신청 의사와 처리 이력을 관리하기 어렵다는 문제가 있었습니다.

이를 개선하기 위해 사용자가 직접 주최자 권한을 신청하고, 관리자가 해당 신청을 조회한 뒤 승인/거절할 수 있는 기능을 추가했습니다.

#### 기존 흐름

```text
관리자가 특정 유저를 주최자 역할로 직접 변경
```

#### 개선 흐름

```text
사용자 주최자 권한 신청
 ↓
관리자 신청 목록 조회
 ↓
관리자 승인 / 거절
 ↓
승인 시 ORGANIZER 권한 부여
```

#### 제공 기능

- 주최자 권한 신청
- 관리자 권한 신청 목록 조회
- 관리자 권한 신청 승인
- 관리자 권한 신청 거절
- 신청 상태 및 처리 이력 관리

---

### 6. CI/CD 및 운영 자동화

운영 배포 과정을 자동화하고, 배포 실패나 장애 상황에 대비하기 위해 GitHub Actions 기반 CI/CD와 운영 안정화 절차를 구축했습니다.

#### CI

- PR 또는 push 시 Gradle build/test 자동 실행
- 테스트 실패 시 merge 제한
- 테스트 결과 및 리포트 확인 가능

#### CD

```text
main 브랜치 merge
 ↓
GitHub Actions 실행
 ↓
Docker Image Build
 ↓
GHCR Push
 ↓
EC2 SSH 접속
 ↓
docker compose pull
 ↓
docker compose up -d
 ↓
Health Check 검증
```

#### 운영 안정화

- 배포 전 MySQL 자동 백업
- 배포 전 Redis RDB 자동 백업
- 배포 후 `/actuator/health` 자동 검증
- health check 실패 시 CD 실패 처리

<br>

## 🤝 깃 컨벤션

### Branch 전략

GitHub Flow 기반으로 총 4종류의 브랜치를 운영, `dev` 브랜치를 중심으로 통합
모든 작업은 **Jira 티켓(Task) 기반** 브랜치 생성 후 PR을 통해 merge

| 브랜치명 | 역할 및 특징 | 작업 규칙 |
| --- | --- | --- |
| **`main`** | 실제 배포 브랜치 | 직접 push 금지, PR/리뷰 후 merge 가능 |
| **`dev`** | 개발 통합 브랜치 | 모든 feature 브랜치가 모이는 기준점 |
| **`타입/*`** | 기능 개발 브랜치 | Jira 티켓 단위로 생성하여 작업 |
| **`hotfix/*`** | 운영 배포 이후 긴급 수정 브랜치 | `main` 기준으로 생성 후 PR을 통해 `merge`, 이후 `dev`에 반영 |

### 커밋 전략
**형식:** `[지라티켓번호] type: 작업내용` <br>
예: `[RUN-1] feat: 대회 목록 조회 API 추가`

| 타입 | 의미 |
| --- | --- |
| **feat** | 새로운 기능 추가 |
| **fix** | 버그 수정 |
| **refactor** | 코드 리팩토링 |
| **test** | 테스트 코드 추가 및 수정 |
| **file** | 파일 이동 또는 삭제, 파일명 변경 |
| **docs** | md, yml 등의 문서 작업 |
| **chore** | 설정 및 기타 변경 |
| **style** | 코드 스타일 변경, 포맷 수정 |

### PR 전략

- **제목:** `[지라티켓번호] type: 작업내용`
- **본문:** 팀 공통 템플릿 사용
- **Merge 시 주의사항 (Squash Merge):** 
Jira 자동 완료 연동을 위해 머지 시 **커밋 제목** 맨 앞의 `[지라티켓번호]` 확인 <br>
예 : `[지라티켓번호] type: 작업내용`
- PR은 최소 1명 이상의 approve 후 merge를 원칙으로 함

<br>

## 📂 프로젝트 구조

```text
backend/src/main/java/com/rungo/api
├── domain
│   ├── auth               # 인증, 로그인, 토큰 관리
│   ├── marathon           # 마라톤 대회 및 코스 관리
│   ├── notification       # 메일 알림 이벤트 처리
│   ├── payment            # 결제 요청, 승인, 만료 처리
│   ├── registration       # 참가 접수, 조회, 취소, 대기열
│   └── users              # 사용자, 관리자, 주최자 승인
└── global
    ├── config             # 전역 설정
    ├── exception          # 공통 예외 처리
    ├── file               # 파일 업로드 처리
    ├── infrastructure     # 외부 인프라 연동
    │   └── mail           # EmailOutbox, SMTP 발송 처리
    ├── response           # 공통 응답 형식
    ├── security           # 인증/인가 보안
    ├── springDoc          # Swagger 문서 설정
    └── util               # 공통 유틸
```

<br>

## 🎬 실행하기

### 요구 사항

- Java 21
- Docker
- Docker Compose

### 인프라 실행

루트 디렉터리에서 MySQL과 Redis를 실행합니다.

```bash
docker compose up -d
```

기본 연결 정보는 다음과 같습니다.

| 항목 | 값 |
| --- | --- |
| MySQL | `localhost:13306` |
| Database | `rungo` |
| Username | `root` |
| Password | `root` |
| Redis | `localhost:16379` |

### 환경 변수

애플리케이션 실행 전에 다음 환경 변수를 설정합니다. 

#### Backend (`backend/.env`)

```env
JWT_SECRET=your-jwt-secret

MAIL_USERNAME=your-mail@example.com
MAIL_PASSWORD=your-mail-password

GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

TOSS_SECRET_KEY=your-toss-secret
```
#### Frontend (`frontend/.env.local`)
```env
NEXT_PUBLIC_TOSS_CLIENT_KEY=your-toss-client-key
```

운영 환경에서는 추가로 아래 값을 사용합니다.
```env
APP_IMAGE=ghcr.io/your-org/your-project:latest
SPRING_PROFILES_ACTIVE=prod

SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/rungo
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_mysql_password

REDIS_PASSWORD=your_redis_password
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=your_redis_password

APP_FRONTEND_URL=https://your-domain.com
APP_MAIL_ENABLED=true
FILE_UPLOAD_DIR=/app/uploads
```

> 메일 발송을 사용하지 않을 경우 `app.mail.enabled` 값을 `false`로 설정합니다.

### 애플리케이션 실행

```bash
cd backend
./gradlew bootRun
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

### API 문서

애플리케이션 실행 후 Swagger UI에서 API 명세를 확인할 수 있습니다.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

API 응답은 공통 응답 객체인 `ApiResponse<T>` 형식을 사용합니다.

<br>

## 👨‍👩‍👧‍👧 역할 분담

| 이름 | 역할 | 
| --- | --- | 
| 최윤서 (팀장) | JWT 인증/인가, Redis 기반 토큰 발급 및 동시성 처리, 데이터 초기화 스케줄러 구현 |
| 강승규 | 마라톤 CRUD API 구현, 주최자 신청/승인/거절/조회 기능 구현 |
| 김동호 | 접수 생성/취소 구현 및 동시성 처리, 접수 대기열 구현 | 
| 백채현 | 접수 조회, 대회 현황 및 참가자 관리, 결제 기능 구현, API 문서화 | 
| 장재원 | 비동기 이메일 알림, OAuth2 로그인, 성능 테스트, CI/CD 구축 및 운영 배포 | 
> Kotlin 마이그레이션은 팀 전체가 공동으로 진행하였습니다.
