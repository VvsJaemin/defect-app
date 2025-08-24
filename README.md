# 품질관리시스템 (Quality Management System, QMS)

[👉 서비스 바로가기](https://qms.jaemin.app)  

**테스트 계정**  
- 아이디 `test` / 비밀번호 `test12#$` - 관리자(MG) 권한  
- 아이디 `devtest` / 비밀번호 `devtest12#$` - 개발자(DEV) 권한

---

## 🛠️ 기술 스택

| 구분         | 기술 및 도구                                                       |
|--------------|--------------------------------------------------------------------|
| 프론트엔드   | React 19, React Router, Zustand, Axios, Tailwind CSS              |
| 백엔드       | Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, QueryDSL, Gradle |
| 데이터베이스 | MySQL 8.x (AWS RDS)                                                |
| 인프라       | AWS EC2, Nginx, GitHub Actions, PM2                                |
| 인증 및 보안 | JWT, HTTPOnly Cookie                                               |

---

## 1. 프로젝트 개요

사내 JSP 레거시 시스템을 React 기반 SPA로 전면 개편한 프로젝트입니다.  
QA팀과 고객사는 결함을 쉽게 등록하고, 개발자는 빠르게 처리하여 품질 개선에 기여할 수 있도록 설계했습니다.  
프론트엔드부터 백엔드, 배포 자동화까지 **1인 풀스택 개발**로 진행하였으며, 운영까지 직접 관리하고 있습니다.

- **개발 인원:** 1인  
- **진행 기간:** 2025.05 ~ 현재  
- **주요 역할:** 설계 · 개발 · 테스트 · 배포 및 운영

---

## 2. 주요 기능

- 권한별 메뉴 접근 제어 및 사용자 관리
- 고객사 프로젝트 및 참여자 관리
- 결함 관리 및 변경사항 이력 추적
- QueryDSL 활용 동적 다중 조건 검색
- 다중 파일 첨부/다운로드 및 결함 통계 시각화
- JWT + HTTPOnly 쿠키 기반 사용자 인증
- GitHub Actions CI/CD 파이프라인 기반 블루-그린 무중단 배포

---

## 3. 기술적 성과

- 세션 기반 인증에서 JWT + HTTPOnly 쿠키 기반 인증으로 마이그레이션하여 XSS 공격 방어
- GitHub Actions CI/CD 파이프라인을 활용한 Nginx 포트 스위칭 기반 블루-그린 무중단 배포 구현
- 결함 상태 변경 시 이력 테이블에 기록하여 추적 가능성 확보
- React 기반 SPA로 전환하여 페이지 리로딩 없는 UX 구현
---

### 🔄 향후 개선 계획

- Springdoc(OpenAPI) 도입을 통한 API 문서 자동화
- React 프로젝트에 TypeScript를 도입하여 타입 안전성과 유지보수성 향상

---

