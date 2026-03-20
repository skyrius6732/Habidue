# HabiDue API Endpoints Specification

## 1. 인증 (Authentication)
| Method | Endpoint | Description | Status |
|---|---|---|---|
| GET | `/oauth2/authorization/google` | 구글 소셜 로그인 시작 | 구현 완료 |
| POST | `/api/auth/logout` | 로그아웃 및 토큰 무효화 | 구현 완료 |

## 2. 공고 관리 (Notices)
| Method | Endpoint | Description | Status |
|---|---|---|---|
| GET | `/api/notices` | 공고 목록 조회 (QueryDSL 정렬: alarm, interest, newest, deadline) | 구현 완료 |
| GET | `/api/notices/{id}` | 특정 공고 상세 정보 조회 | 구현 완료 |

## 3. 알림 키워드 (Keywords)
| Method | Endpoint | Description | Status |
|---|---|---|---|
| GET | `/api/keywords` | 내 알림 키워드 목록 조회 | 구현 완료 |
| POST | `/api/keywords` | 새 알림 키워드 추가 | 구현 완료 |
| DELETE | `/api/keywords/{id}` | 알림 키워드 삭제 | 구현 완료 |

## 4. 관심 공고 / 스크랩 (User Notices)
| Method | Endpoint | Description | Status |
|---|---|---|---|
| GET | `/api/user-notices` | 내 관심 공고 목록 조회 (D-Day 포함) | 구현 완료 |
| POST | `/api/user-notices` | 관심 공고 추가 (스크랩) | 구현 완료 |
| PUT | `/api/user-notices/{id}` | 메모 및 참고 URL 통합 업데이트 | 구현 완료 |
| DELETE | `/api/user-notices/{id}` | 관심 공고 삭제 (ID 기준) | 구현 완료 |
| DELETE | `/api/user-notices/notice/{noticeId}` | 공고 원본 ID 기반 관심 삭제 (토글용) | 구현 완료 |

## 5. 예정 및 필요 API (To-be Implemented)
| Method | Endpoint | Description | Category |
|---|---|---|---|
| POST | `/api/notifications/push/enable` | 푸시 알림 활성화/비활성화 | 알림 설정 |
| GET | `/api/crawler/stats` | 크롤링 수집 현황 통계 | 관리자/배치 |
| POST | `/api/user/export` | 내 관심 공고 데이터 엑셀 내보내기 | 마이페이지 |
