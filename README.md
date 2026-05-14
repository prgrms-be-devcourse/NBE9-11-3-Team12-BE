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
7. &nbsp;&nbsp;[🤝 깃 컨벤션](#-깃-컨벤션)
8. &nbsp;&nbsp;[📂 프로젝트 구조](#-프로젝트-구조)
9. &nbsp;&nbsp;[🎬 실행하기](#-실행하기)
10. &nbsp;&nbsp;[👨‍👩‍👧‍👧 역할 분담](#%E2%80%8D%E2%80%8D%E2%80%8D-역할-분담)
    

</details>
<br>

## 🔍 서비스 소개

### 배경
국내 마라톤을 즐겨하는 인구는 꾸준히 증가하고 있으며, 매년 수백 개의 대회가 전국에서 개최되고 있습니다.
그러나 현재 마라톤 대회 정보는 여러 커뮤니티, 블로그 등에 분산되어 있어 참여자와 주최자가 모두 불편을 겪고 있습니다.

**RunGo**는 흩어져 있는 마라톤 대회 정보를 한곳에서 조회하고, 참가자가 원하는 코스에 접수할 수 있도록 돕는 마라톤 통합 플랫폼입니다.

### 주요 기능

- 대회 조회/접수/운영
- Redis 기반 토큰 관리
- 접수 완료 및 대회 취소 비동기 이메일 알림 (Gmail SMTP)
- k6 기반 부하 테스트

<br>

## 📄 API 명세서

- Auth
<img width="1236" height="304" alt="Image" src="https://github.com/user-attachments/assets/6a8eac29-95bb-4c1b-9ef0-ed8f7420ce6c" />
<br>

- Users
<img width="1236" height="207" alt="Image" src="https://github.com/user-attachments/assets/714b9182-9485-491a-8127-a2848fac473a" />
<br>

- Marathon
<img width="1236" height="351" alt="Image" src="https://github.com/user-attachments/assets/59705bdc-54f7-451e-b090-a20298c3d112" />
<br>

- Registration
<img width="1236" height="208" alt="Image" src="https://github.com/user-attachments/assets/fad6e2a6-9833-4c04-8e88-75ae87f835e1" />
<br>

- Organizer
<img width="1236" height="208" alt="Image" src="https://github.com/user-attachments/assets/d6109eac-d9f7-4be7-a4bc-6d276393ddeb" />
<br>

- Admin
<img width="1236" height="112" alt="Image" src="https://github.com/user-attachments/assets/214ac52d-3d30-4de4-92c0-3e9e2e716dcd" />


<br>

## 💡 기술 스택

Frontend | Backend | Security | Database | Deployment | Other
|:------:|:------:|:------:|:------:|:------:|:------:|
|<img src="https://img.shields.io/badge/Next.js-000000?style=flat-square&logo=nextdotjs&logoColor=white"/>|<img src="https://img.shields.io/badge/Java-007396?style=flat-square&logo=openjdk&logoColor=white"/><br><img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=flat-square&logo=springboot&logoColor=white"/><br><img src="https://img.shields.io/badge/JPA-59666C?style=flat-square&logo=hibernate&logoColor=white"/>|<img src="https://img.shields.io/badge/SpringSecurity-6DB33F?style=flat-square&logo=springsecurity&logoColor=white"/><br><img src="https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white"/><br><img src="https://img.shields.io/badge/OAuth2-3423A6?style=flat-square&logo=auth0&logoColor=white"/>|<img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white"/><br><img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white"/>|<img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white"/>|<img src="https://img.shields.io/badge/Notion-000000?style=flat-square&logo=notion&logoColor=white"/><br><img src="https://img.shields.io/badge/Swagger-85EA2E?style=flat-square&logo=swagger&logoColor=black"/><br><img src="https://img.shields.io/badge/k6-7D64FF?style=flat-square&logo=k6&logoColor=white"/>

```
- Frontend : Next.js
- Backend : Spring Boot, Java, JPA(Hibernate)
- Security : Spring Security, JWT, OAuth2
- Database : MySQL, Redis
- Deployment : Docker
- Documentation : Notion, Swagger
- Load Test : k6
```

<br>

## 🗂️ 데이터베이스

<!-- ERD 이미지를 여기에 추가해주세요 -->
<img width="1842" height="967" alt="Image" src="https://github.com/user-attachments/assets/6dd78021-fc08-4cb6-bdd0-635c90140ddf" />

<br>

## 💻 시스템 아키텍처

<!-- ### System -->

<img width="781" height="487" alt="Image" src="https://github.com/user-attachments/assets/90e8312a-d4e5-43f7-9f95-517dac7e4286" />

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

<br>

## 🤝 깃 컨벤션

### Branch 전략

GitHub Flow 기반으로 총 3종류의 브랜치를 운영, `dev` 브랜치를 중심으로 통합
모든 작업은 이슈 기반 브랜치 생성 후 PR을 통해 merge

| 브랜치명 | 역할 및 특징 | 작업 규칙 |
| --- | --- | --- |
| **`main`** | 실제 배포 브랜치 | 직접 push 금지, PR/리뷰 후 merge 가능 |
| **`dev`** | 개발 통합 브랜치 | 모든 feature 브랜치가 모이는 기준점 |
| **`타입/*`** | 기능 개발 브랜치 | 이슈 단위로 생성하여 작업 |

### 커밋 전략
**형식:** `type: 작업내용` (예: `feat: 대회 목록 조회 API 추가`)

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

- **제목:** `type: 작업내용 (#이슈번호)`
- **본문:** 팀 공통 템플릿 사용 (반드시 `Closes #이슈번호` 기입하여 이슈 자동 종료 유도)
- **Merge 시 주의사항 (Squash Merge):** 
깃허브 머지 시 **커밋 제목** 뒤에 자동 생성되는 `(#PR번호)`는 지우고, 원래의 `(#이슈번호)`만 작성
    - `type: 작업내용 (#이슈번호)`
- PR은 최소 1명 이상의 approve 후 merge를 원칙으로 함

<br>

## 📂 프로젝트 구조

```text
backend/src/main/java/com/rungo/api
├── domain
│   ├── auth               # 인증, 로그인, 토큰 관리
│   ├── marathon           # 마라톤 대회 및 코스 관리
│   ├── notification       # 메일 알림 이벤트 처리
│   ├── registration       # 참가 접수, 조회, 취소
│   └── users              # 사용자, 관리자, 주최자 승인
└── global
    ├── config             # 전역 설정
    ├── exception          # 공통 예외 처리
    ├── file               # 파일 업로드 처리
    ├── infrastructure     # 외부 인프라 연동
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

애플리케이션 실행 전에 다음 환경 변수를 설정합니다. 로컬 실행 시에는 `backend/.env` 파일을 사용할 수 있습니다.

```text
JWT_SECRET=your-jwt-secret
MAIL_USERNAME=your-mail@example.com
MAIL_PASSWORD=your-mail-password
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

> 메일 발송을 사용하지 않을 경우 `backend/src/main/resources/application.yaml`의 `app.mail.enabled` 값을 `false`로 설정합니다.

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
| 최윤서 (팀장) | JWT 인증/인가, Redis 기반 토큰 발급 및 동시성 처리 |
| 강승규 | 마라톤 CRUD API 구현 |
| 김동호 | 접수 생성/취소 구현 및 동시성 처리 | 
| 백채현 | 접수 조회, 대회 현황 및 참가자 관리 | 
| 장재원 | 비동기 이메일 알림, 소셜 로그인, 성능(부하) 테스트 | 
