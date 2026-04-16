# Ride the World Pro | Android Runtime Integration Checklist

## Navigation
- 상위: `RTW_PRO_RELEASE_READINESS_SUMMARY.md`
- 하위: 없음

## 목적
- Android 실제 SDK 연동 작업의 완료 조건을 체크리스트로 관리한다.

## Auth (Google + Firebase)
- [x] Google Sign-In Activity result에서 ID token 수신
- [x] Firebase credential sign-in 성공/실패 분기 처리
- [ ] 토큰 만료/재로그인 경로 검증
- [x] signOut 시 Google/Firebase 세션 정리 경계 정의(`signOutAll`)
- [x] 실패 코드 매핑 구조 정의(`GoogleSignInFailureCode`, `FirebaseSignInErrorCode`)
- [x] Firebase 에러코드 매핑 테이블 정의(`FirebaseAuthErrorMapper`)
- [x] Google Sign-In status code 매핑 테이블 정의(`GoogleSignInErrorMapper`)
- [x] AuthError -> UI 메시지 정책 분리(`AuthUiMessagePolicy`)

## Push (FCM)
- [x] 앱 시작 시 FCM token 동기화
- [x] token refresh 콜백 시 재등록
- [x] 정기 이벤트 topic subscribe/unsubscribe
- [x] 전송 실패 코드 기반 token 등록 결과 매핑(`registerDetailed`, `FcmTokenBackendClient`)
- [x] FCM HTTP 상태 코드 -> 내부 에러 코드 매핑(`FcmErrorMapper`)
- [x] Push 에러 코드 -> UI 메시지 정책(`PushUiMessagePolicy`)
- [x] topic subscribe/unsubscribe 상세 결과 매핑(`subscribeDetailed`, `unsubscribeDetailed`)

## Map / StreetView
- [x] Maps API key 주입/초기화 검증
- [x] 위치 권한 거부/허용 분기 처리
- [x] StreetView timeout 정책 적용
- [x] 미커버리지 시 MAP_ONLY 폴백 연동
- [x] Map bind reason -> 내부 에러 코드 매핑(`MapBindErrorMapper`)
- [x] Map 에러 코드 -> UI 메시지 정책(`MapUiMessagePolicy`)
- [x] Map bind 결과 -> UI 상태(STREETVIEW/MAP_ONLY) 오케스트레이션(`MapRuntimeOrchestrator`)

## Integration Gate
- [x] 로그인 -> 홈 진입 -> 지도 초기화 E2E 통과
- [x] 레이스 종료 후 알림 구독/발송 경로 smoke test 통과
- [x] 장애 시 폴백 메시지/상태 전이 확인

## 실연동 마감 전제조건 (Local)
- [x] `app/google-services.json` 배치
- [x] `local.properties`에 `rtw.auth.webClientId`, `rtw.auth.firebaseProjectId` 실값 입력
- [x] `local.properties`에 `rtw.map.apiKey` 실값 입력
- [x] 대시보드에서 설정 진단 항목 노출 (`authConfigLooksReal`, `googleServicesJsonPresent`)
- [x] 대시보드에 빌드 식별자 노출 (`buildGitSha`)
- [x] Gradle 프리플라이트 점검 태스크 제공 (`:app:printRuntimeIntegrationStatus`)

## 앱 조립(Composition) 확인
- [x] `AppRuntimeComposition`에 Auth/Map/Push 조립 경로 정의
- [x] `AppRuntimeOrchestrator`로 앱 시작/토큰 갱신 흐름 통합
- [x] Android Activity/Service lifecycle 연결 핸들러(`MainAppStartupHandler`, `PushTokenRefreshHandler`) 정의
