# Project Structure

## 개요
- `api-alba`는 Spring Boot + MyBatis 기반의 아르바이트/근태 관리 백엔드 프로젝트입니다.
- 인증, 근태, 점주/직원 기능, 공지/약관/푸시 기능이 도메인별로 나뉘어 있습니다.

## 루트 디렉터리
- `src/`: 애플리케이션 소스와 테스트
- `gradle/`, `gradlew`, `gradlew.bat`: Gradle Wrapper
- `build.gradle`, `settings.gradle`: 빌드 설정
- `postman/`: API 테스트용 컬렉션 추정
- `logs/`: 실행 로그
- `build/`: 빌드 결과물
- `README.md`: 프로젝트 소개 및 실행 방법

## `src/main/java/com/api/alba`
- `controller/`: API 엔드포인트 계층
  - `auth`, `attendance`, `owner`, `staff`, `notice`, `terms`, `push`, `naver`, `web`
- `service/`: 비즈니스 로직 계층
  - 도메인별 서비스(`AuthService`, `AttendanceService`, `OwnerService` 등)
- `mapper/`: MyBatis Mapper 인터페이스
- `domain/`: DB 엔티티/도메인 모델
- `dto/`: 요청/응답 DTO
- `security/`: API Key, JWT, 인증 진입점 등 보안 처리
- `config/`: 보안 설정 등 전역 설정
- `exception/`: 공통 예외 및 예외 응답 처리
- `component/`: 공통 컴포넌트 및 AOP
- `email/`: 이메일 인증/발송 관련 로직
- `firebase/`: FCM 푸시 연동 관련 로직
- `AlbaApplication.java`: Spring Boot 시작점
- `ServletInitializer.java`: WAR 배포용 초기화 클래스

## `src/main/resources`
- `application.yml`, `application.properties`, `application-dev.properties`: 환경 설정
- `schema.sql`: DB 스키마
- `mappers/`: MyBatis XML 쿼리
  - Java `mapper/` 패키지와 도메인별로 대응
- `fcm/`: Firebase 설정 파일
- `logback-spring.xml`, `log4jdbc.log4j2.properties`: 로그 설정
- `banner.txt`: 실행 배너

## `src/test`
- `AlbaApplicationTests.java`: 기본 스프링 부트 테스트

## 구조 특징
- 전형적인 Spring 계층 구조(`controller -> service -> mapper -> DB`)를 따릅니다.
- 도메인 기준으로 패키지가 정리되어 있어 기능별 탐색이 쉬운 편입니다.
- 인증/보안, 근태, 점주/직원 기능이 핵심 축입니다.
