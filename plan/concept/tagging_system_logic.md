공고 데이터가 수집되어 여러분의 화면에 태그가 붙어 나오기까지의 과정을 '자동 사서(Librarian)'
  시스템에 비유해서 아주 자세히 설명해 드릴게요.

  이 흐름을 이해하면 우리 프로젝트의 데이터 정규화(Normalization)가 어떻게 이루어지는지 한눈에 보일
  것입니다.

  ---


  [Step 1] 지식 창고 (The Dictionary): "어떤 단어를 찾을 것인가?"
  태깅의 시작은 "우리가 어떤 단어를 중요하게 생각하는가?"를 정의하는 것입니다.


   * 위치: NoticeKeyword (Enum 파일)
   * 역할: 우리 서비스가 관리하는 태그 메타데이터입니다.
       * 예: "행복주택"이라는 단어가 나오면 TYPE(주거형태) 태그로 분류해라.
       * 예: "신혼부부"라는 단어가 나오면 TARGET(공급대상) 태그로 분류해라.
   * 특징: 이 파일은 사전에 정의된 '사전'과 같습니다. DB가 아니라 코드(Enum)에 박혀 있어 타입 안정성이
     높고 관리가 쉽습니다.

  ---


  [Step 2] 원재료 수집 (Raw Data): "책을 가져오기"
  수집기들이 외부 사이트(LH, SH 등)에서 공고를 긁어옵니다.


   * 위치: LhNoticeCollectorService, ShNoticeCrawlerService 등
   * 역할: 제목(Title)과 본문(Content), 마감일 정보를 가져와서 Notice 테이블에 먼저 저장합니다.
   * 상황: 이때까지 공고는 그냥 '생짜 텍스트' 덩어리일 뿐입니다. 태그는 아직 없습니다.

  ---

  [Step 3] 태깅 엔진 가동 (The Engine): "분석 시작"
  수집기가 저장 직후 tagService.autoClassifyAndAddTags(notice)를 호출합니다. 여기가 핵심입니다.


   * 위치: TagService (중앙 엔진)
   * 분석 로직 (3단계):
       1. Enum 매칭: 제목에 NoticeKeyword에 정의된 단어(예: "청년", "매입임대")가 있는지 훑습니다.
       2. 패턴 분석 (Regex): "24-1차", "25-2차" 처럼 숫자가 변하는 단어들을 정규표현식으로 추출합니다.
       3. 지능형 매핑: "서울대입구역"이라는 단어가 보이면, 엔진 내부에 있는 STATION_DISTRICT_MAP을
          보고 "관악구"라는 태그를 자동으로 하나 더 생성합니다. (우리가 이번에 수집기에서 엔진으로
          옮긴 핵심 로직입니다.)

  ---


  [Step 4] 태그 실체화 (Tag Resolution): "태그 번호표 뽑기"
  엔진이 "청년"이라는 단어를 찾았다면, 이제 DB에 있는 Tag 테이블과 대조합니다.


   * 위치: Tag 테이블 (DB)
   * 과정:
       * DB에 "청년"이라는 태그가 이미 있나? (조회)
       * 없으면 새로 만든다. (생성)
   * 최적화 (1+N 해결 핵심): 여기서 매번 DB를 찌르면 느려지므로, 우리가 이번에 만든 tagCache (Map)를
     사용합니다. 메모리에 태그 리스트를 띄워놓고 "청년 태그 번호가 뭐였지?"를 순식간에 찾아냅니다.

  ---


  [Step 5] 최종 연결 (Persistence): "포스트잇 붙이기"
  가장 중요한 마지막 단계입니다. 공고와 태그를 연결합니다.


   * 위치: NoticeTag 엔티티 & notice_tag 테이블 (DB)
   * 결과:
       * 공고 ID: 101번 (행복주택 공고)
       * 태그 ID: 5번 (청년)
       * 이 두 번호를 묶어서 notice_tag 테이블에 한 줄 저장합니다.
   * 직접 태깅인가?: 네, 별도로 관리하지 않고 NoticeTag라는 '연결 테이블'에 즉시 저장됩니다. 이것이
     나중에 우리가 공고를 조회할 때 notice.getNoticeTags()를 통해 태그를 바로 화면에 뿌릴 수 있게
     해주는 근거가 됩니다.

  ---

  요약하자면 이렇습니다!


   1. NoticeKeyword (Enum): "무엇을 찾을지" 결정하는 사전.
   2. TagService (Engine): 제목을 뽀개서 태그를 골라내는 분석기.
   3. Tag (Table): 태그 그 자체의 정보 (이름, 타입).
   4. NoticeTag (Mapping Table): "이 공고는 이 태그를 가졌다"는 최종 연결 고리.


  이번 리팩토링의 의미:
  이전에는 이 사서(Librarian)가 일을 할 때마다 도서관(DB) 1층부터 10층까지 뛰어다녔다면(N+1), 이제는
  머릿속에 도서 목록을 다 외우고(Cache), 한 번에 여러 권의 책에 포스트잇을 붙이는(BatchSize) 스마트한
  사서로 거듭난 것입니다.


---------------

지식의 흐름을 코드로 구현한 '태깅 엔진 설계도'를 클래스와 메서드 단위로 아주 상세하게 짚어드릴게요.
  이 가이드를 따라가시면 코드의 어느 부분이 어떤 역할을 하는지 완벽하게 파악하실 수 있습니다.

  ---


  Step 1. 수집기 (The Trigger): 공고를 발견하고 엔진을 깨우다
   * 클래스: LhNoticeCollectorService, ShNoticeCrawlerService, PrivateNoticeCrawlerService
   * 핵심 메서드: processSingleLhNotice(), processAiBatchWithTags(), processNotice()


   1. 데이터 저장: 먼저 외부 사이트에서 가져온 정보를 바탕으로 Notice 엔티티를 생성하고 DB에
      저장합니다.
   2. 엔진 호출: 저장된 Notice 객체를 들고 tagService.autoClassifyAndAddTags(notice)를 호출합니다.
      이것이 태깅 작업의 시작 버튼입니다.

  ---


  Step 2. 태그 엔진 (The Brain): 텍스트를 분석하여 의미를 추출하다
   * 클래스: TagService
   * 핵심 메서드 1: autoClassifyAndAddTags(Notice notice, Map tagCache)
       * 역할: 전체 태깅 공정을 지휘하는 컨트롤 타워입니다.
       * 동작: 아래의 세부 분석 메서드들을 순서대로 실행합니다.


   * 세부 분석 메서드 (Private):
       1. NoticeKeyword 루프: NoticeKeyword.values()를 돌면서 제목에 포함된 키워드(청년, 신혼부부
          등)를 찾습니다.
       2. extractAndUpdateSystemStatusTag(): 공고 제목과 마감일(deadline)을 비교하여 이 공고가
          '접수중'인지 '마감'인지, 혹은 '결과발표'인지를 판별합니다.
       3. extractSpecialAndRegionTags():
           * 정규표현식으로 '차수(24-1차)'를 뽑아냅니다.
           * 제목에서 '구/군' 이름을 추출합니다.
           * '역 이름'을 찾으면 내부 STATION_DISTRICT_MAP을 조회해 자동으로 자치구 태그를 추가
             생성합니다.

  ---


  Step 3. 매핑 및 저장 (The Bridge): 찾아낸 단어를 DB 태그와 연결하다
   * 클래스: TagService
   * 핵심 메서드 2: addTagsToNotice(Notice notice, List tagNames, TagType, Map cache)
       * 역할: 분석된 단어 리스트를 실제 DB의 NoticeTag로 변환합니다.
       * 과정:
           1. getOrCreateTagWithCache() 호출: "청년"이라는 단어가 DB의 Tag 테이블에 있는지 확인합니다.
               * 이때 tagCache (Map)를 먼저 봅니다. (DB 조회 횟수를 획기적으로 줄이는 비결!)
               * 캐시에 없으면 tagRepository를 통해 DB에서 가져오거나 새로 만듭니다.
           2. 중복 체크: notice.getNoticeTags() 리스트를 뒤져서 이미 해당 태그가 붙어있는지
              확인합니다. (메모리 내에서 체크하므로 매우 빠릅니다.)
           3. 최종 저장: 중복이 아니라면 NoticeTag 객체를 만들어 noticeTagRepository.save()로 DB에
              저장합니다.

  ---


  Step 4. 조회 최적화 (The Result): 화면에 뿌릴 때 빛을 발하다
   * 클래스: NoticeController / Notice (Entity) / UserNoticeRepositoryImpl
   * 핵심 로직:
       1. Notice 엔티티의 @BatchSize: 컨트롤러가 notice.getNoticeTags()를 호출하는 순간, Hibernate가
          "아, 지금 페이지에 있는 공고 10개 태그를 다 가져와야겠구나!" 하고 IN 쿼리로 한꺼번에
          긁어옵니다.
       2. UserNoticeRepositoryImpl.countInterestBulk(): QueryDSL을 이용해 페이지 내 모든 공고의
          '좋아요' 개수를 단 한 번의 쿼리로 Map에 담아옵니다.
       3. DTO 변환: 컨트롤러는 DB를 다시 부르지 않고, 메모리에 이미 올라와 있는 태그 리스트와 좋아요
          Map을 조합해 NoticeResponseDto를 만들어 사용자에게 보냅니다.

  ---


  [정리] 데이터의 여행 경로
   1. 수집기가 Notice를 낚아챈다.
   2. TagService가 제목을 쪼개서 NoticeKeyword(사전)와 대조한다.
   3. tagCache라는 메모리 지도를 보고 Tag ID를 빛의 속도로 찾아낸다.
   4. NoticeTag라는 연결 고리를 DB에 저장한다.
   5. NoticeController가 @BatchSize 지팡이를 휘둘러 모든 태그를 한 번에 묶어서 화면에 보여준다.


  이것이 우리가 완성한 고성능 자동 태깅 시스템의 전체 명세입니다. 이제 어떤 메서드가 어떤 일을 하는지
  코드를 보실 때 훨씬 명확하게 들어오실 거예요!_

