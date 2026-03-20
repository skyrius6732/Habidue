     1 # HabiDue 프로젝트 데이터 수집 전략 및 진행 상황
     2
     3 ## 🎯 프로젝트 목표
     4 흩어져 있는 다양한 공고 정보를 통합하여 사용자에게 일관되고 편리한 마감 일정 관리 경험을 제공합니다.
       이를 위해 여러 데이터 소스(API, 크롤러)에서 공고 데이터를 수집하고, 중복 없이 효율적으로 관리하는
       파이프라인을 구축합니다.
     5
     6 ---
     7
     8 ## 📊 데이터 소스 현황 및 전략
     9
    10 ### **1. LH (한국토지주택공사)**
    11 *   **방식:** Open API (`http://apis.data.go.kr/B552555/lhLeaseNoticeInfo1/lhLeaseNoticeInfo1`)
    12 *   **특징:**
    13     *   `ServiceKey` 기반 인증, `PAGE`/`PG_SZ` 파라미터를 통한 페이지네이션.
    14     *   응답 구조: `[{"dsSch": [...], "dsList": [...]}]` 형태의 중첩 구조.
    15     *   주요 필드: `PAN_ID` (고유 ID), `PAN_NM` (공고명), `AIS_TP_CD_NM` (유형명), `CNP_CD_NM` (지역명),
       `PAN_DT` (공고 게시일, yyyyMMdd), `CLSG_DT` (마감일, yyyy.MM.dd), `DTL_URL`, `PAN_SS` (공고 상태),
       `ALL_CNT` (전체 공고 수).
    16     *   **데이터 범위:** 현재 API는 최신 또는 활성 공고 위주로 제공하며, 과거 데이터 전체 조회를 위한
       명확한 날짜 범위 파라미터는 API 문서 추가 확인 필요.
    17 *   **처리 전략:**
    18     *   `LhApiClient`를 통해 API 호출 및 데이터 수집.
    19     *   `LhNoticeResponse` DTO로 파싱.
    20     *   `LhNoticeCollectorService`에서 페이지네이션 로직으로 전체 데이터 수집 및 `Notice` 엔티티로
       변환/저장/업데이트.
    21     *   `createdAt`에 `PAN_DT` (공고 게시일) 매핑. `deadline`에 `CLSG_DT` (마감일) 매핑.
    22     *   `공고 접수 시작일` 필드는 현재 API에서 직접 제공되지 않음.
    23
    24 ### **2. HUG (주택도시보증공사)**
    25 *   **방식:** Open API (예상)
    26 *   **특징:**
    27     *   공고 만료 시 API에서 데이터가 사라지는 특성.
    28 *   **처리 전략:**
    29     *   Open API 연동 및 `source="HUG"`로 식별.
    30     *   `Notice` 엔티티의 `apiId` 필드와 `source`를 활용하여 중복 저장 방지.
    31     *   **만료 처리:** API에서 데이터가 조회되지 않는 공고는 DB 내에서 '마감' 또는 '만료' 상태로
       업데이트하는 별도 로직 필요 (스케줄러 연동).
    32
    33 ### **3. SH (서울주택도시공사)**
    34 *   **방식:** Crawler (Jsoup 등 사용)
    35 *   **URL:** `https://www.i-sh.co.kr/main/lay2/program/S1T294C297/www/brd/m_247/list.do`
    36 *   **추출 필드 (예상):** 공고명 (`table.board-list tbody tr td.al a`), 상세 URL, 공고 게시일
       (`td:nth-child(4)`), 마감일 (상세 페이지에서 추출 필요 가능성 있음).
    37 *   **처리 전략:**
    38     *   HTML 파싱을 위한 Crawler 구현.
    39     *   `source="SH"`로 식별.
    40     *   `apiId` (SH 고유 ID)를 추출하여 `Notice` 엔티티 `apiId` 필드에 저장.
    41     *   상세 페이지에서 필요한 추가 정보(마감일 등) 추출 로직 필요.
    42     *   페이지네이션은 URL 파라미터(`pageIndex` 등) 기반으로 처리.
    43
    44 ### **4. 민간임대 (서울시 청년안심주택)**
    45 *   **방식:** Crawler (Jsoup 등 사용)
    46 *   **URL:** `https://soco.seoul.go.kr/youth/bbs/BMSR00015/list.do?menuNo=400008`
    47 *   **추출 필드 (예상):** 공고명, 상세 URL, 공고 게시일, 마감일 (주로 Period 컬럼 또는 배지), 상태
       (Ongoing/Closed 배지).
    48 *   **처리 전략:**
    49     *   HTML 파싱을 위한 Crawler 구현.
    50     *   `source="민간"` 또는 `source="SeoulYouth"` 등으로 식별.
    51     *   `apiId` (서울시 고유 ID) 추출 및 `Notice` 엔티티 `apiId` 필드에 저장.
    52     *   상태 및 날짜 정보를 정확히 추출하는 로직 개발.
    53     *   페이지네이션은 URL 파라미터(`pageIndex=N` 등) 기반으로 처리.
    54
    55 ---
    56
    57 ## 💡 데이터 수집 전략 (A & B) 요약
    58
    59 ### **전략 A: 수집 소스 우선순위 및 안정성 확보**
    60 *   **API 우선:** LH, HUG 등 안정적인 Open API를 우선적으로 사용합니다.
    61 *   **크롤러는 차선책:** API가 없을 경우에만 SH, 민간임대 등 크롤링 방식을 사용합니다.
    62 *   **안정성:** 크롤링 시 사이트 구조 변경에 대비하여, 오류 발생 시 즉시 알림(Slack/Discord 등)을 받을
       수 있는 에러 핸들링 및 알림 시스템을 구축합니다.
    63
    64 ### **전략 B: 데이터 파이프라인 및 중복 방지**
    65 *   **통합 DB:** 모든 소스의 공고 데이터는 `notices` 테이블에 통합 저장됩니다.
    66 *   **고유 식별:** `Notice` 엔티티에 `apiId` (소스별 고유 ID)와 `source` 필드를 사용하여, **`(source,
       apiId)` 조합을 복합 유니크 키**로 설정하여 데이터 중복을 방지합니다.
    67 *   **초기 전체 로드:** 서비스 출시 또는 API 연동 완료 시, 각 소스에서 제공하는 **최대한의 과거
       데이터**를 모두 가져와 DB를 초기화합니다. (LH의 경우, API가 제공하는 최대 범위 내에서 페이지네이션을
       통해 수집)
    68 *   **점진적 업데이트 (주기적 동기화):** 초기 로드 이후, 스케줄러(`@Scheduled`)를 통해 매일 새벽 2시마다
       **새로운 공고를 추가**하거나 **기존 공고의 변경 사항(마감일, 상태 등)을 업데이트**합니다.
    69 *   **HUG API 만료 처리:** API에서 데이터가 사라진 공고는 DB 내에서 만료 상태로 전환하는 로직을
       추가합니다.
    70
    71 ---
    72
    73 ## 🚀 현재까지 완료된 작업 (LH API 연동 중심)
    74
    75 *   **LH API 연동 기본 설정:**
    76     *   `application.yml`: `lh.api.key` 설정 및 파일 복구 완료.
    77     *   `WebClientConfig`: `WebClient.Builder` 빈 등록.
    78     *   `LhApiClient`: LH API 엔드포인트(`baseUrl`, `path`) 및 쿼리 파라미터(`PAGE`, `PG_SZ`,
       `serviceKey`) 수정 완료.
    79     *   DTO: `LhNoticeResponse` (필드 추가: `allCnt`), `LhApiDataContainer` 작성.
    80 *   **데이터 모델 및 저장 로직:**
    81     *   `Notice` 엔티티: `apiId` 필드 추가 및 `(source, apiId)` 복합 유니크 제약 조건 설정 완료.
    82     *   `NoticeRepository`: `findByApiId`, `findBySourceAndApiId` 메서드 추가 완료.
    83     *   `LhNoticeCollectorService`:
    84         *   LH API 전체 데이터 수집을 위한 **페이지네이션 로직 구현** (totalPages 계산, 루프 내 호출,
       지연 시간 `Thread.sleep(500)` 추가).
    85         *   `ALL_CNT` 파싱 로직 구현.
    86         *   기존 공고 `title`, `deadline` 업데이트 로직 구현.
    87         *   날짜 파싱 메서드 (`parseLhDate`, `parseLhPanDt`) 구현.
    88 *   **스케줄링:**
    89     *   `AppApplication`: `@EnableScheduling` 추가.
    90     *   `LhScheduler`: `LhNoticeCollectorService`를 10초 간격으로 실행하도록 임시 설정 (`*/10 * * * *
       *`). (운영 시에는 0 0 2 * * * 로 복구 예정)
    91
    92 ---
    93
    94 ## 🎯 향후 작업 계획 (우선순위)
    95
    96 1.  **LH API 날짜 필터링 조사:** `data.go.kr`에서 `lhLeaseNoticeInfo1` API 문서 상세 확인하여
       `PAN_ST_DT`, `PAN_ED_DT` 등 날짜 범위 조회 파라미터 존재 여부 확인. (존재 시 `LhApiClient` 수정하여 과거
       데이터 수집 기능 강화)
    97 2.  **SH, Private Rental Crawler 구현:** 각 사이트 HTML 구조 분석 및 Jsoup 등을 이용한 크롤러 개발.
    98 3.  **HUG API 연동 및 만료 처리 로직 구체화:** API 특성에 맞춰 만료 공고 처리 로직 구현.
    99 4.  **`Notice` 엔티티 및 DTO 확장:**
   100     *   `applicationStartDate` 등 추가 정보 필요 시 필드 추가.
   101     *   LH API 응답에 `PAN_SS` (공고 상태)가 있다면, 이를 `Notice` 엔티티의 상태 필드에 매핑하는 로직
       고려.
   102 5.  **스케줄러 설정 및 배포:**
   103     *   `LhScheduler`의 cron 표현식을 운영 환경에 맞게 조정 (예: `0 0 2 * * *`).
   104     *   전체 데이터 수집 로직이 안정화된 후, 필요에 따라 점진적 업데이트 로직으로 전환.
   105     *   AWS EC2 배포 준비 (Dockerfile, CI/CD 등).
   106
   107 이로써 데이터 수집 전략 및 지금까지의 작업 내용을 종합하여 파일로 저장했습니다.
   108 이 내용이 HabiDue 프로젝트의 데이터 관리 방향을 명확히 하는 데 도움이 되기를 바랍니다.
