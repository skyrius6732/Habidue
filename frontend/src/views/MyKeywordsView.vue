<template>
  <div class="settings-container">
    <PageHeader 
      icon="👤" 
      title="마이 페이지" 
      :stats-text="activeTab === 'activity' ? '획득 배지' : activeTab === 'keywords' ? '설정된 태그' : activeTab === 'messages' ? '받은 쪽지' : '알림 채널'" 
      :stats-value="activeTab === 'activity' ? (activityData?.badges?.length || 0) : activeTab === 'keywords' ? userTags.length : activeTab === 'messages' ? (messageStore.receivedMessages.length) : (activeNotificationCount + '개 활성')"
      :bio="currentBio"
    />

    <div class="settings-content-wrapper">
      <div class="settings-box">
        <div class="settings-sidebar">
          <div class="sidebar-item" :class="{ active: activeTab === 'activity' }" @click="setActiveTab('activity')">내 활동</div>
          <div class="sidebar-item" :class="{ active: activeTab === 'keywords' }" @click="setActiveTab('keywords')">알림 키워드</div>
          <div class="sidebar-item" :class="{ active: activeTab === 'messages' }" @click="setActiveTab('messages')">
            쪽지함
            <span v-if="messageStore.unreadCount > 0" class="sidebar-unread-badge">{{ messageStore.unreadCount }}</span>
          </div>
          <div class="sidebar-item" :class="{ active: activeTab === 'notifications' }" @click="setActiveTab('notifications')">알림 설정</div>
          <div class="sidebar-item" :class="{ active: activeTab === 'account' }" @click="setActiveTab('account')">계정 정보</div>
        </div>
        
        <div class="settings-content">
          <!-- 0. 쪽지함 탭 -->
          <div v-if="activeTab === 'messages'" class="messages-tab-content">
            <h2 class="section-title">📬 쪽지함</h2>
            <p class="section-desc">다른 사용자나 시스템으로부터 받은 소중한 메시지입니다.</p>
            <div class="message-inbox-wrapper">
              <MessageInbox />
            </div>
          </div>

          <!-- 1. 내 활동 탭 -->
          <div v-if="activeTab === 'activity'" class="activity-tab-content">
            <div class="section-header-flex">
              <div class="text-group">
                <h2 class="section-title">📊 내 활동 지표</h2>
                <p class="section-desc">habiDue 커뮤니티에서 쌓아온 소중한 기록들입니다.</p>
              </div>
            </div>

            <!-- [시니어 조치] 통합 레벨링 대시보드 (v4 - 8:2 비율 및 영역 통합) -->
            <div v-if="userProfile" class="level-dashboard-card-v4 fade-in">
              <!-- 좌측 열 (8): 프로필 정보 + 경험치 프로그레스 통합 -->
              <div class="dash-main-col">
                <div class="dash-integrated-profile">
                  <!-- 프로필 기본 정보 -->
                  <div class="profile-top-info">
                    <div class="dash-level-hexagon" :class="`tier-${badgeStore.getAccountTierNumber(userProfile.level)}`">
                      <span class="hex-lv">Lv</span>
                      <span class="hex-num">{{ userProfile.level }}</span>
                    </div>
                    <div class="dash-user-details">
                      <div class="dash-nickname-group">
                        <AnimatedNickname 
                          :user-id="userProfile.id"
                          :nickname="userProfile.nickname || userProfile.username" 
                          :level="userProfile.level" 
                          :exp="userProfile.totalExp"
                          :badges="activityData?.badges"
                          :show-effects="userProfile.showLevelEffects"
                          :equipped-badge-name="equippedBadgeDisplayName"
                          :karma-point="userProfile.karmaPoint"
                        />
                        <button class="dash-effects-btn" :class="{ active: userProfile.showLevelEffects }" @click="toggleLevelEffects" title="닉네임 효과 토글">
                          {{ userProfile.showLevelEffects ? '✨' : '👤' }}
                        </button>
                      </div>
                      <div class="dash-username-sub">@{{ userProfile.username }}</div>
                    </div>
                  </div>

                  <!-- 경험치 프로그레스 영역 -->
                  <div class="dash-progress-area">
                    <div class="progress-header">
                      <span class="progress-title">성장 경험치 (EXP)</span>
                      <div class="progress-values">
                        <span class="curr">{{ formatNumber(userProfile.totalExp) }}</span>
                        <span class="sep">/</span>
                        <span class="max">{{ formatNumber(nextLevelExp) }}</span>
                      </div>
                    </div>
                    <div class="dash-progress-track">
                      <div class="dash-progress-fill" :style="{ width: expPercentage + '%' }">
                        <div class="progress-shine"></div>
                      </div>
                      <span class="progress-pct-float">{{ Math.floor(expPercentage) }}%</span>
                    </div>
                    <div class="progress-footer-tip">
                      다음 레벨까지 <b>{{ formatNumber(nextLevelExp - userProfile.totalExp) }} EXP</b> 남았습니다.
                    </div>
                  </div>
                </div>
              </div>

              <!-- 우측 열 (2): 지표 카드 -->
              <div class="dash-side-col">
                <div class="dash-stat-box karma" :class="getKarmaClass(userProfile.karmaPoint)">
                  <div class="stat-icon-mini">🛡️</div>
                  <div class="stat-info">
                    <span class="stat-label">신뢰 점수</span>
                    <div class="stat-value-row">
                      <span class="stat-num">{{ (userProfile.karmaPoint / 10).toFixed(1) }}</span>
                      <span class="stat-unit">P</span>
                    </div>
                  </div>
                </div>

                <div v-if="activityData" class="dash-stat-box rank">
                  <div class="stat-icon-mini">🏆</div>
                  <div class="stat-info">
                    <span class="stat-label">커뮤니티 순위</span>
                    <div class="stat-value-row">
                      <span class="stat-num">{{ activityData.totalRank }}</span>
                      <span class="stat-unit">위</span>
                    </div>
                  </div>
                  <!-- 상위 퍼센트 태그 추가 -->
                  <div class="dash-percent-tag">상위 {{ activityData.rankPercent }}%</div>
                </div>
              </div>
            </div>


            
            <!-- [시니어] 활동 신뢰 점수 및 랭킹 가이드 섹션 (종합 워딩으로 변경) -->
            <div class="karma-history-container" :class="{ 'is-open': isKarmaHistoryOpen }">
              <div class="karma-history-header" @click="isKarmaHistoryOpen = !isKarmaHistoryOpen">
                <div class="h-title-group">
                  <h3 class="sub-section-title">나의 활동 성장 및 신뢰도 관리</h3>
                  <span class="karma-info-tip">나의 랭킹 순위와 활동 신뢰도를 한눈에 확인하세요.</span>
                </div>
                <span class="accordion-arrow">{{ isKarmaHistoryOpen ? '▼' : '▶' }}</span>
              </div>
              
              <Transition name="fade-slide">
                <div v-if="isKarmaHistoryOpen" class="karma-history-content-wrapper">
                  
                  <!-- [시니어] 순서 변경: 활동 랭킹 도전 가이드를 최상단으로 -->
                  <!-- [시니어] 활동 점수(EXP) 획득 가이드 섹션 (정밀 가이드로 개편) -->
                  <div class="exp-activity-guide-card">
                    <h4 class="guide-title">🎖️ 활동 랭킹 도전 가이드</h4>
                    <p class="guide-desc">각 메뉴별 지정된 주제로 활동 시 해당 분야의 EXP가 자동으로 합산됩니다.</p>
                    <div class="exp-grid">
                      <!-- 지식인 가이드 -->
                      <div class="exp-item knowledge">
                        <div class="exp-icon">💡</div>
                        <div class="exp-text">
                          <span class="exp-name">지식인 (Knowledge)</span>
                          <p>
                            <b>[공고 소통방]</b>의 모든 주제(공고문의, 서류준비, 현장사진, 경쟁률) 또는 
                            <b>[통합광장]</b>의 기관 소통 탭 내 정보 주제(기관별팁, 서류현황, 기관문의), 
                            <b>[당첨후기]</b>의 노하우 주제(당첨비결, 서류팁, 자금계획, 일반팁)로 글을 작성하고 하트를 받으면 점수가 합산됩니다.
                          </p>
                          <div class="point-badge-group">
                            <span class="exp-points">게시글 작성 +30 EXP</span>
                            <span class="exp-points">하트(좋아요) 수신 +5 EXP</span>
                            <span class="exp-points photo">사진 첨부 +10 EXP</span>
                          </div>
                        </div>
                      </div>
                      <!-- 리뷰왕 가이드 -->
                      <div class="exp-item review">
                        <div class="exp-icon">🏆</div>
                        <div class="exp-text">
                          <span class="exp-name">리뷰왕 (Review)</span>
                          <p>
                            <b>[당첨후기]</b> 게시판의 경험 주제(내 집 마련기, 사전점검, 입주후기) 또는 
                            <b>[파트너스]</b> 메뉴의 업체 이용후기(이사, 청소, 인테리어)를 작성하고 하트를 받으면 점수가 합산됩니다.
                          </p>
                          <div class="point-badge-group">
                            <span class="exp-points">게시글 작성 +40 EXP</span>
                            <span class="exp-points">하트(좋아요) 수신 +10 EXP</span>
                            <span class="exp-points photo">사진 첨부 +10 EXP</span>
                          </div>
                        </div>
                      </div>
                      <!-- 열정파 가이드 -->
                      <div class="exp-item sincerity">
                        <div class="exp-icon">🔥</div>
                        <div class="exp-text">
                          <span class="exp-name">열정파 (Sincerity)</span>
                          <p>
                            <b>[출석체크]</b>(로그인)를 완료하거나, <b>[통합광장]</b>의 일상 주제(자유수다, 동네소식, 장소추천, 동네번개) 게시글 작성, 모든 게시판에서의 댓글 작성 및 하트 수신 시 점수가 합산됩니다.
                          </p>
                          <div class="point-badge-group">
                            <span class="exp-points">출석 +20 EXP</span>
                            <span class="exp-points">게시글 +10 EXP</span>
                            <span class="exp-points">댓글 +3 EXP</span>
                            <span class="exp-points">하트 +5 EXP</span>
                            <span class="exp-points photo">사진 첨부 +10 EXP</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- [시니어] 활동 신뢰 점수 가이드 (구체적 수치 및 제한 기준 추가) -->
                  <div class="karma-guide-card">
                    <h4 class="guide-title">💡 활동 신뢰 점수 가이드</h4>
                    <div class="guide-grid">
                      <div class="guide-item plus">
                        <span class="g-icon">✅</span>
                        <div class="g-text">
                          <span class="g-title">신뢰 점수 얻는 법 (UP)</span>
                          <ul class="guide-sub-list">
                            <li><strong>좋아요 획득 (+0.1P):</strong> 게시글/댓글이 공감을 받으면 점수가 오릅니다. (게시물당 최대 1.0P)</li>
                            <li><strong>모범 활동:</strong> 신고 없이 꾸준히 활동하면 신뢰 등급이 안전하게 유지됩니다.</li>
                            <li><strong>제재 복구:</strong> 억울한 신고로 깎인 점수는 관리자 검토 후 즉시 전액 복구됩니다.</li>
                          </ul>
                        </div>
                      </div>
                      <div class="guide-item minus">
                        <span class="g-icon">⚠️</span>
                        <div class="g-text">
                          <span class="g-title">점수가 깎이는 경우 (DOWN)</span>
                          <ul class="guide-sub-list">
                            <li><strong>운영원칙 위반 (-10P ~ -70P):</strong> 비방, 욕설, 광고 등 신고 승인 시 크게 감점됩니다.</li>
                            <li><strong>쪽지 발송 소모 (-0.1P):</strong> 매너 있는 소통을 위해 발송 시 소량의 점수가 사용됩니다.</li>
                            <li><strong>허위 신고:</strong> 타인을 방해할 목적으로 거짓 신고를 할 경우 운영 정책에 따라 감점됩니다.</li>
                          </ul>
                        </div>
                      </div>
                    </div>
                    
                    <div class="karma-penalty-table">
                      <div class="penalty-header">🚫 점수별 서비스 이용 제한</div>
                      <div class="penalty-body">
                        <div class="p-row"><span class="p-range">80.0P 미만</span><span class="p-effect">쪽지 발송 불가</span></div>
                        <div class="p-row"><span class="p-range">50.0P 미만</span><span class="p-effect">24시간 커뮤니티 이용 정지</span></div>
                        <div class="p-row"><span class="p-range">30.0P 미만</span><span class="p-effect">7일간 커뮤니티 이용 정지</span></div>
                        <div class="p-row"><span class="p-range">10.0P 미만</span><span class="p-effect">계정 영구 활동 제한</span></div>
                      </div>
                    </div>
                  </div>

                  <div v-if="karmaHistory.length > 0" class="karma-history-list">
                    <div v-for="item in karmaHistory.slice(0, 5)" :key="item.id" class="karma-history-item">
                      <div class="h-main-info">
                        <span class="h-date">{{ formatDate(item.createdAt) }}</span>
                        <span class="h-reason">{{ item.reasonDescription }}</span>
                      </div>
                      <div class="h-side-info">
                        <span class="h-comment" v-if="item.comment">{{ item.comment }}</span>
                        <span class="h-change" :class="item.pointChange > 0 ? 'is-plus' : 'is-minus'">
                          {{ item.pointChange > 0 ? '+' : '' }}{{ (item.pointChange / 10).toFixed(1) }}P
                        </span>
                      </div>
                    </div>
                  </div>
                  <div v-else class="empty-karma-history">
                    최근 30일 내 활동 신뢰 점수 변동 이력이 없습니다.
                  </div>
                </div>
              </Transition>
            </div>

            <div v-if="activityData" class="stats-dashboard-v2">
              <div class="stat-card-v2" :class="{ 'is-active': activeStatTooltip === 'COMMUNITY' }" @click="toggleStatTooltip('COMMUNITY')">
                <span class="s-icon">📝</span>
                <div class="s-info">
                  <span class="s-label">총 게시글</span>
                  <span class="s-value">{{ activityData.totalPostCount }}</span>
                </div>
                <div class="stat-tooltip-v2">{{ getStatTooltip('COMMUNITY', activityData.totalPostCount) }}</div>
              </div>
              <div class="stat-card-v2" :class="{ 'is-active': activeStatTooltip === 'COMMUNICATOR' }" @click="toggleStatTooltip('COMMUNICATOR')">
                <span class="s-icon">💬</span>
                <div class="s-info">
                  <span class="s-label">총 댓글</span>
                  <span class="s-value">{{ activityData.totalCommentCount }}</span>
                </div>
                <div class="stat-tooltip-v2">{{ getStatTooltip('COMMUNICATOR', activityData.totalCommentCount) }}</div>
              </div>
              <div class="stat-card-v2" :class="{ 'is-active': activeStatTooltip === 'POST_LIKE' }" @click="toggleStatTooltip('POST_LIKE')">
                <span class="s-icon">❤️</span>
                <div class="s-info">
                  <span class="s-label">게시글 좋아요</span>
                  <span class="s-value">{{ activityData.postLikeReceivedCount }}</span>
                </div>
                <div class="stat-tooltip-v2">{{ getStatTooltip('KNOWLEDGE', activityData.postLikeReceivedCount) }}</div>
              </div>
              <div class="stat-card-v2" :class="{ 'is-active': activeStatTooltip === 'COMMENT_LIKE' }" @click="toggleStatTooltip('COMMENT_LIKE')">
                <span class="s-icon">❤️</span>
                <div class="s-info">
                  <span class="s-label">댓글 좋아요</span>
                  <span class="s-value">{{ activityData.commentLikeReceivedCount }}</span>
                </div>
                <div class="stat-tooltip-v2">{{ getStatTooltip('KNOWLEDGE', activityData.commentLikeReceivedCount) }}</div>
              </div>
              <div class="stat-card-v2" :class="{ 'is-active': activeStatTooltip === 'VIEW' }" @click="toggleStatTooltip('VIEW')">
                <span class="s-icon s-icon-svg">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                </span>
                <div class="s-info">
                  <span class="s-label">누적 조회수</span>
                  <span class="s-value">{{ activityData.totalViewReceivedCount }}</span>
                </div>
                <div class="stat-tooltip-v2">{{ getStatTooltip('VIEW', activityData.totalViewReceivedCount) }}</div>
              </div>
              <div class="stat-card-v2" :class="{ 'is-active': activeStatTooltip === 'COLLECTOR' }" @click="toggleStatTooltip('COLLECTOR')">
                <span class="s-icon">🔖</span>
                <div class="s-info">
                  <span class="s-label">관심 공고</span>
                  <span class="s-value">{{ activityData.totalNoticeInterestCount }}</span>
                </div>
                <div class="stat-tooltip-v2">{{ getStatTooltip('COLLECTOR', activityData.totalNoticeInterestCount) }}</div>
              </div>
              <div class="stat-card-v2" :class="{ 'is-active': activeStatTooltip === 'ATTENDANCE' }" @click="toggleStatTooltip('ATTENDANCE')">
                <span class="s-icon">📅</span>
                <div class="s-info">
                  <span class="s-label">연속 출석</span>
                  <span class="s-value">{{ activityData.consecutiveAttendanceDays }}일</span>
                </div>
                <div class="stat-tooltip-v2">
                  {{ getStatTooltip('ATTENDANCE', activityData.totalAttendanceCount + activityData.consecutiveAttendanceDays) }}
                </div>
              </div>
              <div class="stat-card-v2" :class="{ 'is-active': activeStatTooltip === 'REVIEW' }" @click="toggleStatTooltip('REVIEW')">
                <span class="s-icon">✨</span>
                <div class="s-info">
                  <span class="s-label">후기 작성</span>
                  <span class="s-value">{{ activityData.reviewPostCount }}건</span>
                </div>
                <div class="stat-tooltip-v2">{{ getStatTooltip('REVIEW', activityData.reviewPostCount) }}</div>
              </div>
              <div class="stat-card-v2" :class="{ 'is-active': activeStatTooltip === 'TOTAL_ATTENDANCE' }" @click="toggleStatTooltip('TOTAL_ATTENDANCE')">
                <span class="s-icon">🔥</span>
                <div class="s-info">
                  <span class="s-label">누적 출석</span>
                  <span class="s-value">{{ activityData.totalAttendanceCount }}회</span>
                </div>
                <div class="stat-tooltip-v2">
                  {{ getStatTooltip('ATTENDANCE', activityData.totalAttendanceCount + activityData.consecutiveAttendanceDays) }}
                </div>
              </div>
            </div>



            <!-- 실제 내 배지함 (세트 효과 적용) -->
            <div class="badge-section">
              <h3 class="sub-section-title">내 배지함 <span class="badge-count">{{ activityData?.badges?.length || 0 }}</span></h3>
              <div v-if="activityData?.badges?.length > 0" class="badge-grid">
                  <div v-for="badge in activityData.badges" :key="badge.id" 
                    class="badge-card-set" 
                    :class="[`tier-${badge.level}`, { 'is-equipped': userProfile.equippedBadgeId === (badge.badgeId || badge.id) }]">
                    
                    <!-- [시니어 조치] 레벨 30 이상 카드 표면 유리 반사 효과 -->
                    <div v-if="badge.level >= 30" class="card-glass-reflection"></div>

                    <div class="badge-visual-set" :class="`tier-${badge.level}`">
                      <span class="badge-emoji-set">{{ badge.displayName?.split(' ')[0] || '🎖️' }}</span>
                    </div>
                    <div class="badge-info-set">
                      <div class="badge-top-flex">
                        <div class="badge-level-tag-set" :class="`tier-${badge.level}`">
                          Lv.{{ badge.level }}
                        </div>
                        <button 
                          v-if="userProfile.equippedBadgeId !== (badge.badgeId || badge.id)" 
                          class="btn-equip-badge" 
                          @click="handleEquipBadge(badge.badgeId || badge.id)"
                        >
                          대표 설정
                        </button>
                        <span v-else class="equipped-tag">장착됨</span>
                      </div>
                      <h4 class="badge-name-set" :class="`tier-${badge.level}`">{{ badge.displayName.replace(/[\uD800-\uDBFF][\uDC00-\uDFFF]\s*/g, '') || '알 수 없는 배지' }}</h4>
                      <p class="badge-desc-set">{{ badge.description }}</p>
                      <!-- DB에서 넘어온 꿀팁 적용 -->
                      <div v-if="badge.badgeTip" class="badge-tip-set" :class="`tier-${badge.level}`">
                        <span class="tip-icon-v3">💡</span>
                        <span class="tip-text-standard">{{ badge.badgeTip }}</span>
                      </div>
                      <!-- [시니어] 배지 진행도 시각화 (프로그레스 바) -->
                      <div class="badge-progress-container" v-if="badge.nextTargetValue">
                        <div class="progress-info">
                          <span class="progress-label">다음 단계까지</span>
                          <span class="progress-val"><b>{{ badge.currentValue }}</b> / {{ badge.nextTargetValue }}</span>
                        </div>
                        <div class="progress-bar-bg">
                          <div class="progress-bar-fill" :class="`tier-${badge.level}`" :style="{ width: Math.min(100, (badge.currentValue / badge.nextTargetValue) * 100) + '%' }"></div>
                        </div>
                      </div>
                      <div class="badge-progress-container is-max" v-else>
                        <div class="progress-info">
                          <span class="progress-label">마스터 달성!</span>
                          <span class="progress-val">최고 레벨</span>
                        </div>
                        <div class="progress-bar-bg">
                          <div class="progress-bar-fill is-max" :class="`tier-${badge.level}`" style="width: 100%"></div>
                        </div>
                      </div>

                      <div class="badge-footer-set" :class="`tier-${badge.level}`">
                        <span class="badge-date-set">{{ formatDate(badge.acquiredAt) }} 획득</span>
                      </div>
                    </div>
                  </div>
                </div>
              <div v-else class="empty-badge-box">
                <p>아직 획득한 배지가 없네요.<br>커뮤니티 활동을 통해 배지를 모아보세요!</p>
                <button @click="$router.push('/board')" class="btn-go-action">커뮤니티 활동하러 가기</button>
              </div>
            </div>
          </div>

          <!-- 2. 키워드 설정 탭 -->
          <div v-if="activeTab === 'keywords'">
            <h2 class="section-title">🏷️ 알림 키워드</h2>
            <p class="section-desc">관심 있는 태그를 등록하면 공고 리스트에서 우선적으로 강조됩니다.</p>
            
            <div class="tag-search-container">
              <div class="input-wrapper">
                <input 
                  v-model="searchQuery" 
                  @input="handleSearch"
                  @keydown.down.prevent="moveSelection(1)"
                  @keydown.up.prevent="moveSelection(-1)"
                  @keydown.enter.prevent="selectCurrentItem"
                  @keydown.esc="closeSearch"
                  placeholder="태그를 검색하세요 (예: 서울, 청년, 행복주택)" 
                  class="tag-search-input"
                />
                <ul v-if="searchResults.length > 0" ref="dropdownRef" class="search-results-dropdown">
                  <li 
                    v-for="(result, index) in searchResults" 
                    :key="result.tagId" 
                    @click="addTag(result)"
                    class="result-item"
                    :class="{ 'is-selected': selectedIndex === index }"
                  >
                    <span class="type-badge" :class="result.type">{{ getTypeLabel(result.type) }}</span>
                    <span class="name">{{ result.name }}</span>
                  </li>
                </ul>
              </div>
            </div>

            <div class="keyword-list-horizontal">
              <div v-for="tag in userTags" :key="tag.id" class="tag-chip-v2-fixed">
                <span class="kw-hash">#</span><span class="kw-text">{{ tag.name }}</span>
                <button @click="removeTag(tag.id)" class="btn-tag-delete">&times;</button>
              </div>
              <div v-if="userTags.length === 0" class="empty-msg">등록된 태그가 없습니다. 검색을 통해 추가해 보세요!</div>
            </div>
          </div>

          <!-- 3. 알림 설정 탭 -->
          <div v-if="activeTab === 'notifications'">
            <h2 class="section-title">🔔 알림 설정</h2>
            <div class="setting-row">
              <div class="setting-label">
                <span class="label-main">실시간 키워드 푸시</span>
                <p class="label-sub">관심 키워드와 일치하는 새 공고가 뜨면 앱 푸시를 보냅니다.</p>
              </div>
              <div class="toggle-switch" :class="{ active: pushEnabled }" @click="togglePush"></div>
            </div>
            <div class="setting-section">
              <div class="setting-row no-border">
                <div class="setting-label">
                  <span class="label-main">메일 리포트</span>
                  <p class="label-sub">마감 임박 공고와 주간 맞춤 소식을 이메일로 받아봅니다.</p>
                </div>
                <div class="toggle-switch" :class="{ active: mailEnabled && isEmailVerified, disabled: !isEmailVerified }" @click="toggleMailReport"></div>
              </div>
              <div class="email-verify-box" :class="{ verified: isEmailVerified }">
                <div v-if="!isEmailVerified" class="verify-form">
                  <div class="responsive-input-group">
                    <input v-model="reportEmailInput" placeholder="리포트 받을 이메일" class="verify-input" :disabled="isCodeSent" />
                    <button v-if="!isCodeSent" @click="requestVerificationCode" class="btn-verify-action" :disabled="!reportEmailInput">인증발송</button>
                    <button v-else @click="isCodeSent = false" class="btn-verify-cancel">수정</button>
                  </div>
                  <div v-if="isCodeSent" class="responsive-input-group mt-15 fade-in">
                    <input v-model="verificationCodeInput" placeholder="인증코드 6자리" class="verify-input code-input" maxlength="6" />
                    <button @click="verifyCode" class="btn-verify-confirm">확인</button>
                  </div>
                </div>
                <div v-else class="verified-status fade-in">
                  <div class="status-top">
                    <span class="check-icon">✅</span>
                    <span class="verified-email">{{ userProfile?.reportEmail }}</span>
                    <span class="status-badge-verified">인증됨</span>
                  </div>
                  <div class="verified-actions-horizontal">
                    <button @click="sendTestReport" class="btn-test-report-mini">테스트 발송</button>
                    <button @click="resetEmailVerification" class="btn-email-change-mini">메일 변경</button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 4. 계정 정보 탭 -->
          <div v-if="activeTab === 'account'">
            <h2 class="section-title">⚙️ 계정 정보</h2>
            <div v-if="userProfile" class="account-profile-card">
              <div class="account-pic-large">{{ (userProfile.nickname || userProfile.username)?.charAt(0).toUpperCase() }}</div>
              <div class="account-details">
                <div class="username-edit-group">
                  <template v-if="!isEditingNickname">
                    <h3 class="acc-display-name">{{ userProfile.nickname || userProfile.username }}</h3>
                    <button @click="startEditNickname" class="btn-username-edit" title="닉네임 수정">✏️</button>
                  </template>
                  <template v-else>
                    <div class="edit-input-wrapper">
                      <input v-model="newNickname" class="username-input" placeholder="새 닉네임 입력" @keyup.enter="saveNickname" maxlength="15" />
                      <div class="edit-actions">
                        <button @click="saveNickname" class="btn-save-mini" :disabled="!newNickname.trim() || newNickname === userProfile.nickname">저장</button>
                        <button @click="isEditingNickname = false" class="btn-cancel-mini">취소</button>
                      </div>
                    </div>
                  </template>
                </div>
                <span class="acc-email-label">{{ userProfile.email }}</span>
                <span class="acc-provider">HabiDue 계정으로 로그인됨</span>
              </div>
            </div>
            <div class="account-management-section">
              <h4 class="sub-section-title">데이터 및 계정 관리</h4>
              <div class="account-actions">
                <button class="acc-btn export-btn" @click="handleExport">📁 내 공고 데이터 내보내기 (.xlsx)</button>
                <button class="acc-btn delete-btn" @click="handleDeleteAccount">서비스 탈퇴하기</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/plugins/axios'
import PageHeader from '@/components/PageHeader.vue'
import AnimatedNickname from '@/components/AnimatedNickname.vue'
import MessageInbox from '@/components/MessageInbox.vue'
import { useAuthStore } from '@/stores/auth'
import { useBadgeStore } from '@/stores/badge'
import { useMessageStore } from '@/stores/message'
import { useNotificationStore } from '@/stores/notification'
import { useUiStore } from '@/stores/ui'

const authStore = useAuthStore()
const badgeStore = useBadgeStore()
const messageStore = useMessageStore()
const notificationStore = useNotificationStore()
const uiStore = useUiStore()
const route = useRoute()
const router = useRouter()
const activeTab = ref(route.query.tab || 'activity')

// [시니어 조치] 탭 전환 시 URL 쿼리 파라미터 업데이트
const setActiveTab = (tab) => {
  activeTab.value = tab
  router.push({ query: { ...route.query, tab } })
}

// [시니어 조치] URL 쿼리 파라미터 변경 감시하여 탭 전환
watch(() => route.query.tab, (newTab) => {
  if (newTab) activeTab.value = newTab
})
const userTags = ref([])
const searchQuery = ref('')
const searchResults = ref([])
const selectedIndex = ref(-1) 
const dropdownRef = ref(null)
const selectedItemRef = ref(null)
const userProfile = ref(null)
const activityData = ref(null)
const karmaHistory = ref([]) // [시니어] 활동 신뢰 점수 변동 이력 추가
const isKarmaHistoryOpen = ref(false) // [시니어] 아코디언 상태 (기본: 접힘)

const isUserRestricted = computed(() => {
  if (!userProfile.value?.restrictedUntil) return false
  return new Date(userProfile.value.restrictedUntil) > new Date()
})

const isEditingNickname = ref(false)
const newNickname = ref('')
const isSyncing = ref(false)
const activeStatTooltip = ref(null)

// [시니어 조치] 대표 배지 장착 처리
const handleEquipBadge = async (badgeId) => {
  try {
    await axios.patch('/api/users/me/equipped-badge', null, { params: { badgeId } })
    userProfile.value.equippedBadgeId = badgeId
    await uiStore.showAlert('대표 칭호가 성공적으로 변경되었습니다.', '칭호 변경')
  } catch (e) {
    await uiStore.showAlert('칭호 변경에 실패했습니다.', '오류')
  }
}

// [시니어 조치] 레벨 효과 토글 처리
const toggleLevelEffects = async () => {
  const newValue = !userProfile.value.showLevelEffects
  try {
    await axios.patch('/api/users/me/level-effects', null, { params: { enabled: newValue } })
    userProfile.value.showLevelEffects = newValue
  } catch (e) {
    await uiStore.showAlert('설정 변경에 실패했습니다.', '오류')
  }
}

const toggleStatTooltip = (type) => {
  if (activeStatTooltip.value === type) {
    activeStatTooltip.value = null
  } else {
    activeStatTooltip.value = type
  }
}

// [시니어 조치] 장착된 배지 명칭 계산
const equippedBadgeDisplayName = computed(() => {
  if (!userProfile.value?.equippedBadgeId || !activityData.value?.badges) return null
  const badge = activityData.value.badges.find(b => (b.badgeId || b.id) === userProfile.value.equippedBadgeId)
  return badge ? badge.displayName : null
})

// [시니어 조치] 다음 레벨 필요 경험치 계산 (ExpService 역산: N * N * 50)
const nextLevelExp = computed(() => {
  if (!userProfile.value) return 0
  const lv = userProfile.value.level
  return lv * lv * 50
})

// [시니어 조치] 경험치 백분율 계산
const expPercentage = computed(() => {
  if (!userProfile.value) return 0
  const lv = userProfile.value.level
  const exp = userProfile.value.totalExp
  const prevLevelExp = lv > 1 ? (lv - 1) * (lv - 1) * 50 : 0
  const currentLevelMax = nextLevelExp.value - prevLevelExp
  if (currentLevelMax <= 0) return 0
  const currentEarned = exp - prevLevelExp
  const pct = (currentEarned / currentLevelMax) * 100
  return Math.min(Math.max(pct, 0), 100)
})

const formatNumber = (num) => {
  if (num === undefined || num === null) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")
}

// [시니어 조치] DB에서 가져온 규칙 기반 동적 툴팁 생성 (성능 최적화 버전)
const getStatTooltip = (type, value) => {
  if (value === undefined || value === null || !activityData.value?.badgeRules) return '데이터를 불러오는 중...';
  
  // 해당 타입의 규칙만 필터링 후 수치순 정렬
  const rules = activityData.value.badgeRules
    .filter(r => r.badgeType === type)
    .sort((a, b) => a.requiredValue - b.requiredValue);

  if (rules.length === 0) return '규칙 정보가 없습니다.';

  // 현재 등급 찾기 (내 수치보다 작거나 같은 규칙 중 가장 큰 것)
  const currentRule = [...rules].reverse().find(r => value >= r.requiredValue);
  
  // 다음 등급 찾기 (내 수치보다 큰 규칙 중 가장 작은 것)
  const nextRule = rules.find(r => value < r.requiredValue);
  
  let text = '';
  if (currentRule) {
    text += `현재 등급: ${currentRule.rankTitle} ${currentRule.rankEmoji}`;
  } else {
    text += `현재 등급: 입문자 🔰`;
  }

  if (nextRule) {
    const remaining = nextRule.requiredValue - value;
    if (type === 'ATTENDANCE') {
      const fastPath = Math.ceil(remaining / 2); // 연속 출석 시 2점씩 상승
      text += ` > 다음 등급(${nextRule.rankTitle} ${nextRule.rankEmoji})까지 ${remaining}개 남았습니다.` ;
      text += `(연속 출석 시 ${fastPath}일, 누적 출석 시 ${remaining}일 기준)`;
    } else {
      text += ` > 다음 등급(${nextRule.rankTitle} ${nextRule.rankEmoji})까지 ${remaining}개 남았습니다.`;
    }
  } else {
    text += ` > 최고 등급 달성! 🎉`;
  }
  
  return text;
};

// [시니어 조치] 활동 동기화 테스트 실행
const handleSyncActivity = async () => {
  isSyncing.value = true
  try {
    await axios.post('/api/users/me/activity/sync')
    await fetchUserActivity()
    await uiStore.showAlert('활동 지표 및 배지 정보가 최신화되었습니다.', '동기화 완료')
  } catch (e) {
    await uiStore.showAlert('동기화에 실패했습니다.', '오류')
  } finally {
    isSyncing.value = false
  }
}

const startEditNickname = () => { newNickname.value = userProfile.value.nickname || userProfile.value.username; isEditingNickname.value = true }
const saveNickname = async () => {
  const trimmedName = newNickname.value.trim()
  if (trimmedName === (userProfile.value.nickname || userProfile.value.username)) { isEditingNickname.value = false; return }
  try {
    await axios.patch('/api/users/me/nickname', null, { params: { nickname: trimmedName } })
    userProfile.value.nickname = trimmedName; isEditingNickname.value = false
    if (authStore.user) authStore.user.nickname = trimmedName
    await uiStore.showAlert('닉네임이 성공적으로 변경되었습니다.', '알림')
  } catch (e) { await uiStore.showAlert('이미 사용 중인 닉네임이거나 변경에 실패했습니다.', '오류') }
}

const pushEnabled = ref(true)
const mailEnabled = ref(false)
const isEmailVerified = computed(() => userProfile.value?.reportEmailVerified || false)
const reportEmailInput = ref('')
const verificationCodeInput = ref('')
const isCodeSent = ref(false)

const getBadgeMetricValue = (type) => {
  if (!activityData.value) return '0'
  switch (type) {
    case 'KNOWLEDGE': return activityData.value.totalLikeReceivedCount + '회'
    case 'COLLECTOR': return activityData.value.totalNoticeInterestCount + '회'
    case 'REVIEW': return activityData.value.reviewPostCount + '건'
    case 'COMMUNITY': return activityData.value.totalPostCount + '건'
    case 'COMMUNICATOR': return activityData.value.totalCommentCount + '개'
    case 'ATTENDANCE': return activityData.value.consecutiveAttendanceDays + '일'
    default: return '0'
  }
}

const getTypeLabel = (type) => {
  const labels = { 'METRO': '광역', 'CITY_COUNTY': '시/군/구', 'SUBWAY_LINE': '노선', 'STATION': '역', 'TYPE': '유형', 'TARGET': '대상', 'SPECIAL': '특수', 'REGION': '지역', 'PROVIDER': '기관', 'SYSTEM': '상태' }
  return labels[type] || type
}

const activeNotificationCount = computed(() => { let c = 0; if (pushEnabled.value) c++; if (mailEnabled.value && isEmailVerified.value) c++; return c })
const currentBio = computed(() => {
  if (activeTab.value === 'activity') return '커뮤니티 활동을 통해 획득한 나의 배지와 활동 지표를 확인하세요.'
  if (activeTab.value === 'keywords') return '관심 태그를 설정하여 나에게 꼭 맞는 공고을 가장 먼저 확인하세요.'
  if (activeTab.value === 'messages') return '개인 소통 및 시스템 알림을 확인하는 나만의 우체통입니다.'
  if (activeTab.value === 'notifications') return '나에게 편한 방법으로 공고 알림 소식을 받아보세요.'
  return '내 계정 정보를 확인하고 데이터를 안전하게 관리하세요.'
})

const fetchMyTags = async () => { try { const res = await axios.get('/api/user-tags'); userTags.value = res.data.data } catch (e) {} }
const fetchUserProfile = async () => { try { const res = await axios.get('/api/users/me'); userProfile.value = res.data.data; if (userProfile.value.reportEmail) reportEmailInput.value = userProfile.value.reportEmail } catch (e) {} }
const fetchUserActivity = async () => { try { const res = await axios.get('/api/users/me/activity'); activityData.value = res.data.data } catch (e) { console.error('활동 데이터 로드 실패', e) } }
const fetchKarmaHistory = async () => { try { const res = await axios.get('/api/users/me/karma-history'); karmaHistory.value = res.data.data } catch (e) { console.error('활동 신뢰 점수 이력 로드 실패', e) } }

const toggleMailReport = async () => {
  if (!isEmailVerified.value) { await uiStore.showAlert('이메일 인증이 완료된 후에 기능을 켤 수 있습니다.', '안내'); return }
  const newValue = !mailEnabled.value
  try { await axios.patch('/api/users/me/email-report', null, { params: { enabled: newValue } }); mailEnabled.value = newValue } catch (e) { await uiStore.showAlert('설정 변경에 실패했습니다.', '오류') }
}
const togglePush = () => { pushEnabled.value = !pushEnabled.value }
const requestVerificationCode = async () => { try { await axios.post('/api/users/me/report-email/send-code', null, { params: { email: reportEmailInput.value } }); isCodeSent.value = true; await uiStore.showAlert('인증 코드가 발송되었습니다.\n메일함을 확인해 주세요.', '코드 발송') } catch (e) { await uiStore.showAlert('코드 발송 실패.', '오류') } }
const verifyCode = async () => { try { const res = await axios.post('/api/users/me/report-email/verify-code', null, { params: { email: reportEmailInput.value, code: verificationCodeInput.value } }); if (res.data.data) { await uiStore.showAlert('인증 완료!', '인증 완료'); await fetchUserProfile(); isCodeSent.value = false } else { await uiStore.showAlert('인증 코드가 일치하지 않습니다.', '오류') } } catch (e) { await uiStore.showAlert('인증 처리 중 오류 발생.', '오류') } }
const resetEmailVerification = async () => { if (await uiStore.showConfirm('이메일 주소를 변경하시겠습니까?', '이메일 변경')) { userProfile.value.reportEmailVerified = false; isCodeSent.value = false; verificationCodeInput.value = ''; mailEnabled.value = false } }
const sendTestReport = async () => { try { const res = await axios.post('/api/users/me/email-report/test'); await uiStore.showAlert(res.data.data, '테스트 발송') } catch (e) { await uiStore.showAlert('발송 실패.', '오류') } }

const handleSearch = async () => {
  if (searchQuery.value.trim().length < 1) { searchResults.value = []; selectedIndex.value = -1; return }
  try {
    const res = await axios.get('/api/user-tags/search', { params: { name: searchQuery.value } })
    searchResults.value = res.data.data.filter(r => !userTags.value.some(my => my.tagId === r.tagId))
    selectedIndex.value = -1 // 검색할 때마다 선택 인덱스 초기화
  } catch (e) {}
}

const moveSelection = (direction) => {
  if (searchResults.value.length === 0) return
  const nextIndex = selectedIndex.value + direction
  if (nextIndex >= 0 && nextIndex < searchResults.value.length) {
    selectedIndex.value = nextIndex
    // 스크롤 동기화
    nextTick(() => {
      const activeEl = dropdownRef.value?.querySelector('.is-selected')
      if (activeEl) {
        activeEl.scrollIntoView({ block: 'nearest' })
      }
    })
  }
}

const selectCurrentItem = () => {
  if (selectedIndex.value >= 0 && selectedIndex.value < searchResults.value.length) {
    addTag(searchResults.value[selectedIndex.value])
  } else if (searchQuery.value.trim().length > 0 && searchResults.value.length > 0) {
    // 선택된 게 없는데 엔터 치면 첫 번째 항목 추가
    addTag(searchResults.value[0])
  }
}

const closeSearch = () => {
  searchQuery.value = ''
  searchResults.value = []
  selectedIndex.value = -1
}

const addTag = async (tag) => { try { await axios.post(`/api/user-tags/${tag.tagId}`); closeSearch(); fetchMyTags() } catch (e) { await uiStore.showAlert('이미 등록된 태그입니다.', '알림') } }
const removeTag = async (id) => { try { await axios.delete(`/api/user-tags/${id}`); fetchMyTags() } catch (e) {} }
const formatDate = (dateStr) => { if (!dateStr) return ''; return dateStr.split('T')[0].replace(/-/g, '.') }
const handleExport = async () => { /* 엑셀 추출 기존 로직 */ }
const handleDeleteAccount = async () => { 
  if (await uiStore.showConfirm('정말로 탈퇴하시겠습니까?\n모든 활동 데이터가 초기화됩니다.', '회원 탈퇴')) { 
    try { 
      await axios.delete('/api/users/me')
    } catch (e) {
      // [시니어 조치] 이미 탈퇴 처리되어 인증이 풀린 경우(401)에도 성공으로 간주하고 계속 진행
      if (e.response?.status !== 401) {
        await uiStore.showAlert('탈퇴 처리 중 오류가 발생했습니다.', '오류')
        return
      }
    } 

    // [시니어 조치] 성공 또는 인증 해제(탈퇴 완료) 시 로컬 정보 즉시 정리 및 이동
    authStore.clearTokens()
    notificationStore.disconnectSse()
    
    await uiStore.showAlert('탈퇴 처리가 완료되었습니다.\n그동안 이용해 주셔서 감사합니다.', '탈퇴 완료')
    window.location.href = '/' // 완전히 새로고침하여 메인으로 이동
  } 
}

const getKarmaClass = (point) => {
  if (point >= 800) return 'safe'
  if (point >= 500) return 'warning'
  return 'danger'
}

onMounted(() => { 
  fetchMyTags(); 
  fetchUserProfile(); 
  fetchUserActivity(); 
  fetchKarmaHistory();
  messageStore.fetchReceivedMessages(); // 진입 시 쪽지함 데이터 로드
})
</script>

<style scoped>
/* --- 공통 티어 색상 변수 (닉네임 세트) --- */
.tier-1 { --t-color: var(--text-secondary); --t-bg: rgba(0,0,0,0.03); --t-grad: var(--text-primary); }
.tier-5 { --t-color: #b08d57; --t-bg: rgba(176, 141, 87, 0.08); --t-grad: linear-gradient(135deg, #804a00, #b08d57, #804a00); }
.tier-10 { --t-color: #475569; --t-bg: rgba(71, 85, 105, 0.08); --t-grad: linear-gradient(135deg, #334155, #94a3b8, #334155); }
.tier-30 { --t-color: #facc15; --t-bg: rgba(250, 204, 21, 0.08); --t-grad: linear-gradient(135deg, #a16207, #fde047, #a16207); }
.tier-50 { --t-color: #10b981; --t-bg: rgba(16, 185, 129, 0.08); --t-grad: linear-gradient(135deg, #065f46, #34d399, #065f46); }
.tier-70 { --t-color: #ff416c; --t-bg: rgba(225, 29, 72, 0.06); --t-grad: linear-gradient(90deg, #ff416c, #ffd700, #48bb78, #4facfe, #9400d3); }
.tier-90 { --t-color: #22d3ee; --t-bg: rgba(34, 211, 238, 0.06); --t-grad: linear-gradient(135deg, #0891b2, #ffffff, #0891b2); }
.tier-100 { --t-color: #facc15; --t-bg: rgba(0, 0, 0, 0.95); --t-grad: linear-gradient(to right, #facc15, #ffffff, #facc15); }

:global(.dark-mode) .tier-10 { --t-color: #cbd5e1; --t-bg: rgba(203, 213, 225, 0.12); --t-grad: linear-gradient(135deg, #94a3b8, #f8fafc, #94a3b8); }

.settings-container { width: 100%; min-height: 100vh; padding-bottom: 80px; }
.settings-content-wrapper { padding: 0 20px; }
.settings-box { margin-top: 1.5rem; background: var(--card-bg); border: 1px solid var(--border-color); display: flex; min-height: 650px; border-radius: 12px; color: var(--text-primary); overflow: hidden; }

.settings-sidebar { flex: 0 0 200px; border-right: 1px solid var(--border-color); background-color: var(--hover-bg); }
.sidebar-item { padding: 20px 25px; cursor: pointer; font-size: 0.95rem; color: var(--text-secondary); transition: all 0.2s; white-space: nowrap; text-align: left; font-weight: 600; }
.sidebar-item.active { color: var(--text-primary); font-weight: 800; border-left: 4px solid var(--link-color); background-color: var(--card-bg); padding-left: 21px; }

.settings-content { flex: 1; padding: 40px 60px; min-width: 0; }
.section-header-flex { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 32px; gap: 20px; }
.section-title { font-size: 1.6rem; font-weight: 800; margin-bottom: 8px; color: var(--text-primary); }
.section-desc { color: var(--text-secondary); font-size: 0.9rem; margin: 0; }

.btn-sync-test { background: var(--hover-bg); border: 1px solid var(--border-color); padding: 8px 16px; border-radius: 8px; font-size: 0.8rem; font-weight: 700; color: var(--link-color); cursor: pointer; transition: all 0.2s; }
.btn-sync-test:hover:not(:disabled) { border-color: var(--link-color); background: var(--card-bg); }
.btn-sync-test:disabled { opacity: 0.5; cursor: not-allowed; }

.sidebar-unread-badge { background: #e74c3c; color: white; font-size: 0.65rem; padding: 2px 6px; border-radius: 8px; margin-left: 6px; }

/* [시니어 조치] 레벨 대시보드 v4 (8:2 비율 및 통합 디자인) */
.level-dashboard-card-v4 {
  background: var(--card-bg); border: 1.5px solid var(--border-color); border-radius: 28px;
  display: grid; grid-template-columns: 8fr 2fr; margin-bottom: 35px; box-shadow: 0 12px 40px rgba(0,0,0,0.04); overflow: visible;
}
.dash-main-col { padding: 35px; border-right: 1.5px solid var(--border-color); background: linear-gradient(135deg, var(--hover-bg), var(--card-bg)); border-radius: 28px 0 0 28px; }
.dash-side-col { padding: 25px; display: flex; flex-direction: column; gap: 12px; justify-content: center; background: var(--card-bg); border-radius: 0 28px 28px 0; }

.dash-integrated-profile { display: flex; flex-direction: column; gap: 30px; }
.profile-top-info { display: flex; align-items: center; gap: 25px; }
.dash-integrated-box { display: flex; flex-direction: column; gap: 30px; }
.dash-profile-row { display: flex; align-items: center; gap: 25px; }

.dash-level-hexagon { width: 80px; height: 80px; background: var(--t-grad); clip-path: polygon(25% 0%, 75% 0%, 100% 50%, 75% 100%, 25% 100%, 0% 50%); display: flex; flex-direction: column; align-items: center; justify-content: center; color: white; box-shadow: 0 8px 20px rgba(0,0,0,0.1); flex-shrink: 0; }
.hex-lv { font-size: 0.75rem; font-weight: 800; }
.hex-num { font-size: 1.7rem; font-weight: 950; }

.dash-nickname-group { display: flex; align-items: center; gap: 12px; }
.dash-nickname-group :deep(.animated-nickname) { font-size: 1.5rem !important; font-weight: 900; }
.dash-username-sub { font-size: 0.95rem; color: var(--text-secondary); font-weight: 600; opacity: 0.7; }
.dash-effects-btn { background: var(--hover-bg); border: 1px solid var(--border-color); width: 34px; height: 34px; border-radius: 10px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; font-size: 1.1rem; }
.dash-effects-btn.active { background: var(--link-color); color: white; border-color: transparent; }

.dash-progress-area { background: rgba(255, 255, 255, 0.05); padding: 20px; border-radius: 20px; border: 1px solid var(--border-color); }
.progress-header { display: flex; justify-content: space-between; margin-bottom: 12px; }
.progress-title { font-size: 0.85rem; font-weight: 850; color: var(--text-secondary); }
.progress-values { font-size: 0.9rem; font-weight: 700; }
.dash-progress-track { height: 14px; background: var(--border-color); border-radius: 10px; overflow: hidden; position: relative; }
.dash-progress-fill { height: 100%; background: linear-gradient(90deg, var(--link-color), #60a5fa); border-radius: 10px; position: relative; transition: width 1.2s cubic-bezier(0.34, 1.56, 0.64, 1); }
.progress-shine { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent); animation: shine-move 2s infinite; }
.progress-pct-float { position: absolute; right: 10px; top: 50%; transform: translateY(-50%); font-size: 0.65rem; font-weight: 950; color: white; }
.progress-footer-tip { font-size: 0.8rem; color: var(--text-secondary); text-align: right; margin-top: 8px; }

.dash-stat-box {
  background: var(--hover-bg);
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--border-color);
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: all 0.2s;
  position: relative; /* 태그 배치를 위해 추가 */
}

.dash-percent-tag {
  position: absolute;
  top: -10px;
  right: 10px;
  background: #fadb14;
  color: #000;
  font-size: 0.65rem;
  font-weight: 950;
  padding: 3px 8px;
  border-radius: 6px;
  box-shadow: 0 4px 8px rgba(250, 219, 20, 0.3);
  z-index: 10;
}
.dash-stat-box:hover { transform: translateY(-3px); border-color: var(--link-color); }
.stat-icon-mini { font-size: 1.5rem; }
.stat-label { font-size: 0.65rem; font-weight: 800; color: var(--text-secondary); text-transform: uppercase; }
.stat-num { font-size: 1.2rem; font-weight: 950; }

.message-inbox-wrapper { margin-top: 20px; }

/* [시니어 조치] 레벨 대시보드 v3 (전문가용 대시보드 레이아웃) */
.level-dashboard-card-v3 {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 24px;
  padding: 30px;
  margin-bottom: 35px;
  display: grid;
  grid-template-columns: 1fr 300px; /* PC: 2열 구조 고정 */
  gap: 40px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.04);
  position: relative;
  overflow: visible;
}

.dash-left-col {
  display: flex;
  flex-direction: column;
  gap: 25px;
  min-width: 0;
  overflow: visible;
}

.dash-right-col {
  display: flex;
  flex-direction: column;
  gap: 15px;
  justify-content: center;
}

/* 1. 유저 프로필 섹션 */
.dash-profile-section {
  display: flex;
  align-items: center;
  gap: 25px;
  overflow: visible;
}

.dash-level-hexagon {
  width: 85px;
  height: 85px;
  background: var(--t-grad);
  clip-path: polygon(25% 0%, 75% 0%, 100% 50%, 75% 100%, 25% 100%, 0% 50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 20px rgba(0,0,0,0.15);
  flex-shrink: 0;
  transition: transform 0.3s ease;
}
.dash-level-hexagon:hover { transform: scale(1.05) rotate(5deg); }
.hex-lv { font-size: 0.75rem; font-weight: 800; opacity: 0.9; text-transform: uppercase; margin-bottom: -2px; }
.hex-num { font-size: 1.8rem; font-weight: 950; text-shadow: 0 2px 4px rgba(0,0,0,0.2); }

.dash-user-details { display: flex; flex-direction: column; gap: 6px; }
.dash-nickname-group { display: flex; align-items: center; gap: 12px; }
.dash-nickname-group :deep(.animated-nickname) { font-size: 1.6rem !important; font-weight: 900; }

.dash-effects-btn {
  background: var(--hover-bg);
  border: 1px solid var(--border-color);
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 1.1rem;
}
.dash-effects-btn:hover { border-color: var(--link-color); transform: translateY(-2px); }
.dash-effects-btn.active { background: var(--link-color); color: white; border-color: transparent; box-shadow: 0 4px 10px rgba(0,149,246,0.3); }

.dash-username-sub { font-size: 0.95rem; color: var(--text-secondary); font-weight: 600; opacity: 0.7; }

/* 2. 지표 그리드 섹션 */
.dash-stats-grid {
  display: flex;
  gap: 15px;
  flex-shrink: 0;
}

.dash-stat-box {
  min-width: 160px;
  background: var(--hover-bg);
  padding: 18px 22px;
  border-radius: 20px;
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 15px;
  position: relative;
  transition: all 0.25s ease;
}
.dash-stat-box:hover { transform: translateY(-4px); box-shadow: 0 8px 20px rgba(0,0,0,0.05); border-color: var(--link-color); }

.stat-icon-mini { font-size: 1.6rem; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1)); }
.stat-info { display: flex; flex-direction: column; gap: 2px; }
.stat-label { font-size: 0.72rem; font-weight: 800; color: var(--text-secondary); text-transform: uppercase; }
.stat-value-row { display: flex; align-items: baseline; gap: 3px; }
.stat-num { font-size: 1.3rem; font-weight: 950; color: var(--text-primary); }
.stat-unit { font-size: 0.8rem; font-weight: 700; color: var(--text-secondary); }

/* 패널티 및 퍼센트 태그 */
.dash-penalty-tag {
  position: absolute; top: -10px; right: 10px;
  background: #ff4d4f; color: white; font-size: 0.65rem; font-weight: 900;
  padding: 3px 8px; border-radius: 6px; box-shadow: 0 4px 8px rgba(255,77,79,0.3);
  animation: bounce-in 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.dash-percent-tag {
  position: absolute; top: -10px; right: 10px;
  background: #fadb14; color: #000; font-size: 0.65rem; font-weight: 900;
  padding: 3px 8px; border-radius: 6px; box-shadow: 0 4px 8px rgba(250,219,20,0.3);
}

@keyframes bounce-in {
  from { opacity: 0; transform: scale(0.5); }
  to { opacity: 1; transform: scale(1); }
}

/* 3. 진행도 섹션 */
.dash-progress-section {
  background: var(--hover-bg);
  padding: 20px 25px;
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border: 1px solid var(--border-color);
}

.progress-header { display: flex; justify-content: space-between; align-items: center; }
.progress-title { font-size: 0.85rem; font-weight: 850; color: var(--text-secondary); }
.progress-values { font-size: 0.9rem; font-weight: 700; }
.progress-values .curr { color: var(--link-color); font-size: 1.1rem; font-weight: 900; }
.progress-values .sep { margin: 0 4px; opacity: 0.3; }
.progress-values .max { color: var(--text-secondary); }

.dash-progress-track {
  height: 14px;
  background: var(--border-color);
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}
.dash-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--link-color), #60a5fa);
  border-radius: 10px;
  position: relative;
  transition: width 1.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.progress-shine {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  animation: shine-move 2s infinite;
}
.progress-pct-float {
  position: absolute; right: 10px; top: 50%; transform: translateY(-50%);
  font-size: 0.65rem; font-weight: 950; color: white; text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.progress-footer-tip { font-size: 0.8rem; color: var(--text-secondary); text-align: right; font-weight: 500; opacity: 0.8; }
.progress-footer-tip b { color: var(--link-color); font-weight: 800; }

/* 반응형: 모바일 최적화 */
@media (max-width: 992px) {
  .level-dashboard-card-v3 { 
    display: flex; /* 모바일은 수직 스택으로 */
    flex-direction: column;
    padding: 25px 20px; 
    gap: 25px; 
    border-radius: 0; 
    border-left: none; 
    border-right: none; 
  }
  
  .dash-left-col { gap: 20px; }
  .dash-profile-section { justify-content: center; text-align: center; flex-direction: column; gap: 15px; }
  .dash-nickname-group { justify-content: center; flex-wrap: wrap; }
  
  .dash-right-col { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
  .dash-stat-box { min-width: 0; padding: 15px; gap: 10px; justify-content: center; }
  
  .guide-grid { grid-template-columns: 1fr; gap: 12px; } /* 가이드 카드 모바일 1열 */
  
  .stat-icon-mini { font-size: 1.3rem; }
  .stat-num { font-size: 1.1rem; }
  
  .dash-progress-section { padding: 15px; }
  .progress-header { flex-direction: column; gap: 5px; text-align: center; }
  .progress-footer-tip { text-align: center; margin-top: 5px; }
}

@media (max-width: 480px) {
  .dash-stats-grid { grid-template-columns: 1fr; }
}

/* [시니어 조치] 등급 미리보기 섹션 스타일 */
.tier-preview-section {
  background: var(--hover-bg);
  border: 1px dashed var(--border-color);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 40px;
}
.test-badge {
  font-size: 0.65rem;
  background: #ed4956;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  vertical-align: middle;
  margin-left: 6px;
}
.tier-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 15px;
}
.preview-item {
  background: var(--card-bg);
  padding: 12px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.preview-label {
  font-size: 0.7rem;
  font-weight: 800;
  color: var(--text-secondary);
  text-transform: uppercase;
}

/* [시니어 조치] 대시보드 v2 스타일 보강 - 9개 항목 최적화 (3열 그리드) */
.stats-dashboard-v2 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 48px; }
.stat-card-v2 { 
  background: var(--hover-bg); padding: 16px 20px; border-radius: 16px; border: 1px solid var(--border-color); 
  display: flex; align-items: center; gap: 15px; transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer; position: relative; 
}
.stat-card-v2:hover { transform: translateY(-4px); border-color: var(--link-color); background: var(--card-bg); box-shadow: 0 10px 25px rgba(0,0,0,0.06); }

/* 커스텀 미니 툴팁 스타일 */
.stat-tooltip-v2 {
  position: absolute; bottom: calc(100% + 12px); left: 50%; transform: translateX(-50%) translateY(8px);
  background: rgba(33, 37, 41, 0.98); color: #fff; padding: 10px 14px; border-radius: 12px;
  font-size: 0.72rem; font-weight: 600; white-space: nowrap; z-index: 100;
  opacity: 0; visibility: hidden; transition: all 0.2s ease;
  box-shadow: 0 8px 24px rgba(0,0,0,0.25); pointer-events: none;
  border: 1px solid rgba(255,255,255,0.1);
}
.stat-tooltip-v2::after {
  content: ''; position: absolute; top: 100%; left: 50%; margin-left: -6px;
  border-width: 6px; border-style: solid; border-color: rgba(33, 37, 41, 0.98) transparent transparent transparent;
}

/* PC 호버 시 노출 */
@media (min-width: 993px) {
  .stat-card-v2:hover .stat-tooltip-v2 { opacity: 1; visibility: visible; transform: translateX(-50%) translateY(0); }
}

/* 모바일 클릭/액티브 시 노출 */
.stat-card-v2.is-active .stat-tooltip-v2 { opacity: 1; visibility: visible; transform: translateX(-50%) translateY(0); }

.stat-card-v2 .s-icon { font-size: 1.25rem; background: var(--card-bg); width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 10px rgba(0,0,0,0.04); flex-shrink: 0; color: var(--text-primary); }
.s-icon-svg { color: var(--text-primary); }
.stat-card-v2 .s-info { display: flex; flex-direction: column; min-width: 0; }
.stat-card-v2 .s-label { font-size: 0.75rem; color: var(--text-secondary); font-weight: 800; margin-bottom: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.stat-card-v2 .s-value { font-size: 1.15rem; font-weight: 900; color: var(--text-primary); }

/* [시니어 조치] 배지 카드 미리보기 전용 "세트(Set)" 스타일 */
.badge-card-set {
  position: relative; background: var(--card-bg); border: 1.5px solid var(--t-color); border-radius: 20px; 
  padding: 24px; display: flex; gap: 20px; transition: 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); 
  overflow: hidden; background-color: var(--t-bg) !important;
}
.badge-card-set:hover { transform: translateY(-6px); box-shadow: 0 10px 25px rgba(0,0,0,0.1); }

/* 유리 반사 효과 (Lv 30+) */
.card-glass-reflection {
  position: absolute; inset: 0; 
  background: linear-gradient(135deg, transparent 45%, rgba(255,255,255,0.1) 50%, transparent 55%);
  background-size: 250% 250%; animation: cardShine 6s infinite; pointer-events: none; z-index: 5;
}
@keyframes cardShine { 0% { background-position: 250% 250%; } 15% { background-position: -150% -150%; } 100% { background-position: -150% -150%; } }

.badge-visual-set { 
  width: 66px; height: 66px; border-radius: 18px; background: rgba(255,255,255,0.1); 
  border: 1.5px solid var(--t-color); display: flex; align-items: center; justify-content: center; 
  font-size: 2.2rem; flex-shrink: 0; box-shadow: 0 8px 20px rgba(0,0,0,0.05);
}
.badge-visual-set.tier-100 { background: #000; box-shadow: 0 0 15px var(--t-color); }

.badge-info-set { flex: 1; min-width: 0; position: relative; z-index: 10; }
.badge-top-flex { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px; }

/* [시니어 조치] 레벨 태그 너비 최적화 (내용물 크기에 딱 맞춤) */
.badge-level-tag-set { 
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 0.7rem; font-weight: 900; padding: 3px 10px; border-radius: 8px; 
  border: 1px solid var(--t-color); color: var(--t-color); background: rgba(255,255,255,0.5); 
}
:global(.dark-mode) .badge-level-tag-set { background: rgba(0,0,0,0.3); }

.badge-name-set { font-size: 1.15rem; font-weight: 950; background: var(--t-grad); -webkit-background-clip: text; -webkit-text-fill-color: transparent !important; margin: 10px 0 6px; letter-spacing: -0.03em; }
.badge-name-set.tier-1 { -webkit-text-fill-color: var(--text-primary) !important; }

.badge-desc-set { font-size: 0.85rem; color: var(--text-secondary); line-height: 1.5; margin-bottom: 12px; }

.badge-tip-set { 
  background: rgba(255,255,255,0.3); border: 1px dashed var(--t-color); 
  padding: 8px 12px; border-radius: 10px; margin-bottom: 12px; font-size: 0.78rem; color: var(--text-primary); 
  display: flex; gap: 8px;
}
:global(.dark-mode) .badge-tip-set { background: rgba(0,0,0,0.2); }

/* [시니어] 배지 진행도 프로그레스 바 스타일 */
.badge-progress-container { margin: 15px 0; }
.progress-info { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.progress-label { font-size: 0.72rem; font-weight: 800; color: var(--text-secondary); opacity: 0.8; }
.progress-val { font-size: 0.75rem; font-weight: 900; color: var(--text-primary); }
.progress-val b { color: var(--t-color); font-size: 0.85rem; }

.progress-bar-bg { width: 100%; height: 6px; background: rgba(0,0,0,0.05); border-radius: 10px; overflow: hidden; position: relative; }
:global(.dark-mode) .progress-bar-bg { background: rgba(255,255,255,0.05); }

.progress-bar-fill { height: 100%; border-radius: 10px; background: var(--t-color); transition: width 1s cubic-bezier(0.34, 1.56, 0.64, 1); }
.progress-bar-fill.tier-70, .progress-bar-fill.tier-90, .progress-bar-fill.tier-100 { background: var(--t-grad); background-size: 200% auto; animation: shine-move 2s linear infinite; }
.progress-bar-fill.is-max { box-shadow: 0 0 10px var(--t-color); }

.badge-footer-set { display: flex; justify-content: space-between; align-items: center; padding-top: 10px; border-top: 1px dashed rgba(0,0,0,0.1); }
.badge-metric-set { font-size: 0.75rem; color: var(--text-secondary); }
.badge-metric-set b { color: var(--t-color); }
.badge-date-set { font-size: 0.7rem; color: var(--text-muted); }

/* --- 배지 섹션 및 기존 공통 유지 --- */
.sub-section-title { font-size: 1.1rem; font-weight: 800; margin-bottom: 24px; display: flex; align-items: center; gap: 10px; }
.badge-count { background: var(--link-color); color: white; padding: 2px 10px; border-radius: 12px; font-size: 0.75rem; }
.badge-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
.badge-card-v2 { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 20px; padding: 24px; display: flex; gap: 20px; align-items: center; position: relative; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.badge-card-v2.badge-rank-70, .badge-card-v2.badge-rank-90, .badge-card-v2.badge-rank-100 { color: white; border-color: rgba(255,255,255,0.2); }
.badge-card-v2.badge-rank-70 .badge-name-v2, .badge-card-v2.badge-rank-90 .badge-name-v2, .badge-card-v2.badge-rank-100 .badge-name-v2 { color: white; }
.badge-card-v2.badge-rank-70 .badge-desc-v2, .badge-card-v2.badge-rank-90 .badge-desc-v2, .badge-card-v2.badge-rank-100 .badge-desc-v2,
.badge-card-v2.badge-rank-70 .badge-metric, .badge-card-v2.badge-rank-90 .badge-metric, .badge-card-v2.badge-rank-100 .badge-metric,
.badge-card-v2.badge-rank-70 .badge-date-v2, .badge-card-v2.badge-rank-90 .badge-date-v2, .badge-card-v2.badge-rank-100 .badge-date-v2 { color: rgba(255,255,255,0.85); }
.badge-card-v2.badge-rank-70 .badge-metric b, .badge-card-v2.badge-rank-90 .badge-metric b, .badge-card-v2.badge-rank-100 .badge-metric b { color: #fff; text-decoration: underline; }

.badge-visual-v2 { width: 66px; height: 66px; background: rgba(255,255,255,0.2); border-radius: 18px; display: flex; align-items: center; justify-content: center; font-size: 2.2rem; flex-shrink: 0; backdrop-filter: blur(4px); }
.badge-info-v2 { flex: 1; min-width: 0; }
.badge-top-flex { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px; }

/* [시니어 조치] 대표 설정 버튼 스타일 고도화 (화이트 모드 가시성 확보) */
.btn-equip-badge {
  background: var(--card-bg);
  border: 1.2px solid var(--t-color);
  color: var(--t-color);
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 0.7rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-equip-badge:hover {
  background: var(--t-color);
  color: white !important;
}

.equipped-tag {
  background: #38a169;
  color: white !important;
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 0.7rem;
  font-weight: 900;
  box-shadow: 0 2px 6px rgba(56, 161, 105, 0.3);
}

.badge-level-tag { margin-bottom: 0; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
.badge-name-v2 { font-size: 1.1rem; font-weight: 850; margin: 0 0 4px; color: var(--text-primary); letter-spacing: -0.03em; }
.badge-desc-v2 { font-size: 0.8rem; color: var(--text-secondary); line-height: 1.5; margin-bottom: 8px; }

/* [시니어 조치] 배지 활동 가이드 팁 스타일 */
.badge-tip-v2 {
  background: rgba(255, 193, 7, 0.08);
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 12px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  border: 1px dashed rgba(255, 193, 7, 0.3);
}
.badge-card-v2.badge-rank-70 .badge-tip-v2, 
.badge-card-v2.badge-rank-90 .badge-tip-v2, 
.badge-card-v2.badge-rank-100 .badge-tip-v2 {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}
.tip-icon { font-size: 0.85rem; filter: drop-shadow(0 0 2px rgba(255, 193, 7, 0.5)); }
.tip-text { font-size: 0.75rem; color: var(--text-primary); line-height: 1.4; word-break: keep-all; }
.badge-card-v2.badge-rank-70 .tip-text, 
.badge-card-v2.badge-rank-90 .tip-text, 
.badge-card-v2.badge-rank-100 .tip-text {
  color: white;
}
.tip-text b { color: #d97706; }
.badge-card-v2.badge-rank-70 .tip-text b, 
.badge-card-v2.badge-rank-90 .tip-text b, 
.badge-card-v2.badge-rank-100 .tip-text b {
  color: #fbbf24;
}

.badge-footer-v2 { display: flex; justify-content: space-between; align-items: center; border-top: 1px dashed rgba(0,0,0,0.1); padding-top: 10px; }
.badge-card-v2.badge-rank-70 .badge-footer-v2, .badge-card-v2.badge-rank-90 .badge-footer-v2, .badge-card-v2.badge-rank-100 .badge-footer-v2 { border-top-color: rgba(255,255,255,0.2); }
.badge-metric { font-size: 0.75rem; color: var(--text-secondary); }
.badge-metric b { color: var(--link-color); }
.badge-date-v2 { font-size: 0.7rem; color: var(--text-muted); }

/* [시니어 조치] 빈 배지 안내 스타일 복구 */
.mb-50 { margin-bottom: 50px; }
.test-preview-tag { font-size: 0.65rem; font-weight: 800; color: #ed4956; border: 1px solid #ed4956; padding: 2px 6px; border-radius: 4px; }

.empty-badge-box { 
  text-align: center; padding: 60px 20px; background: var(--hover-bg); 
  border-radius: 20px; border: 1px dashed var(--border-color); color: var(--text-secondary);
  margin-top: 20px;
}
.empty-badge-box p { font-size: 0.95rem; line-height: 1.6; margin-bottom: 20px; font-weight: 600; }
.btn-go-action { 
  background: var(--link-color); color: white; border: none; 
  padding: 12px 28px; border-radius: 10px; font-weight: 700; cursor: pointer; 
  transition: all 0.2s; box-shadow: 0 4px 12px rgba(0, 149, 246, 0.2);
}
.btn-go-action:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0, 149, 246, 0.3); opacity: 0.9; }

/* [시니어 조치] 깨진 키워드/알림 CSS 복구 */
.tag-search-container { position: relative; margin-bottom: 30px; max-width: 450px; }
.tag-search-input { width: 100%; padding: 14px 16px; border: 1px solid var(--border-color); border-radius: 12px; font-size: 0.95rem; background: var(--card-bg); color: var(--text-primary); outline: none; transition: border-color 0.2s; }
.tag-search-input:focus { border-color: var(--link-color); }
.search-results-dropdown { position: absolute; top: 100%; left: 0; right: 0; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; margin-top: 8px; z-index: 100; box-shadow: 0 10px 30px rgba(0,0,0,0.15); max-height: 250px; overflow-y: auto; padding: 8px 0; }
.result-item { padding: 12px 20px; display: flex; align-items: center; gap: 12px; cursor: pointer; transition: background 0.2s; }
.result-item:hover, .result-item.is-selected { 
  background-color: rgba(0, 149, 246, 0.1); 
  border-left: 4px solid var(--link-color); 
  padding-left: 16px; 
}
.type-badge { font-size: 0.65rem; font-weight: 800; padding: 2px 6px; border-radius: 4px; background: var(--tag-bg); color: var(--text-secondary); }

.keyword-list-horizontal { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 20px; }
.tag-chip-v2-fixed { display: inline-flex; align-items: center; gap: 6px; background: var(--hover-bg); border: 1px solid var(--border-color); padding: 6px 14px; border-radius: 20px; }
.kw-hash { color: var(--link-color); font-weight: 800; }
.kw-text { font-size: 0.85rem; font-weight: 700; color: var(--text-primary); }
.btn-tag-delete { background: none; border: none; font-size: 1.2rem; color: var(--text-secondary); cursor: pointer; padding: 0 4px; line-height: 1; }

/* 알림 설정 레이아웃 보존 */
.setting-row { display: flex; justify-content: space-between; align-items: center; padding: 24px 0; border-bottom: 1px solid var(--divider-color); }
.label-main { display: block; font-size: 1rem; font-weight: 800; color: var(--text-primary); margin-bottom: 4px; }
.label-sub { font-size: 0.85rem; color: var(--text-secondary); margin: 0; }

/* [시니어 조치] 알림 설정 버튼 및 박스 스타일 복구 */
.email-verify-box { margin-top: 20px; padding: 25px; background: var(--hover-bg); border-radius: 12px; border: 1px solid var(--border-color); }
.status-top { display: flex; align-items: center; gap: 10px; margin-bottom: 15px; }
.verified-email { font-weight: 700; color: var(--text-primary); }
.status-badge-verified { font-size: 0.75rem; font-weight: 800; color: #38a169; background: rgba(56, 161, 105, 0.1); padding: 2px 8px; border-radius: 4px; }
.verified-actions-horizontal { display: flex; gap: 10px; }
.btn-test-report-mini { flex: 1; height: 40px; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 8px; font-size: 0.85rem; font-weight: 700; color: var(--text-primary); cursor: pointer; transition: all 0.2s; }
.btn-email-change-mini { flex: 1; height: 40px; background: none; border: 1px solid var(--border-color); border-radius: 8px; font-size: 0.85rem; font-weight: 700; color: var(--text-secondary); cursor: pointer; }
.btn-test-report-mini:hover { border-color: var(--link-color); color: var(--link-color); }

.toggle-switch { width: 44px; height: 24px; background-color: var(--border-color); border-radius: 12px; position: relative; cursor: pointer; transition: background 0.2s; }
.toggle-switch::after { content: ''; position: absolute; top: 3px; left: 3px; width: 18px; height: 18px; background: white; border-radius: 50%; transition: 0.2s; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.toggle-switch.active { background-color: var(--link-color); }
.toggle-switch.active::after { left: 23px; }

/* 이메일 인증 박스 */
.email-verify-box { margin-top: 20px; padding: 25px; background: var(--hover-bg); border-radius: 12px; border: 1px solid var(--border-color); }
.responsive-input-group { display: flex; gap: 10px; }
.verify-input { flex: 1; padding: 12px; border: 1px solid var(--border-color); border-radius: 8px; background: var(--card-bg); color: var(--text-primary); }
.btn-verify-action { padding: 0 20px; background: var(--link-color); color: white; border: none; border-radius: 8px; font-weight: 700; cursor: pointer; }

/* 계정 정보 스타일 */
.account-profile-card { display: flex; align-items: center; gap: 25px; margin-bottom: 30px; background: var(--hover-bg); padding: 30px; border-radius: 20px; border: 1px solid var(--border-color); }
.account-pic-large { width: 80px; height: 80px; background: var(--link-color); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 2rem; font-weight: bold; flex-shrink: 0; }
.username-edit-group { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.btn-username-edit { background: var(--card-bg); border: 1px solid var(--border-color); padding: 4px 8px; border-radius: 6px; cursor: pointer; transition: all 0.2s; }
.btn-username-edit:hover { border-color: var(--link-color); background: var(--hover-bg); }

/* 닉네임 수정 폼 스타일 */
.edit-input-wrapper { display: flex; align-items: center; justify-content: space-between; gap: 12px; width: 100%; }
.username-input { flex: 1; max-width: 250px; padding: 10px 14px; border: 2px solid var(--link-color); border-radius: 10px; background: var(--card-bg); font-weight: 700; color: var(--text-primary); outline: none; }
.edit-actions { display: flex; gap: 8px; margin-left: auto; }
.btn-save-mini { background: var(--link-color); color: white; border: none; padding: 6px 16px; border-radius: 8px; font-weight: 800; font-size: 0.85rem; cursor: pointer; transition: opacity 0.2s; }
.btn-save-mini:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-cancel-mini { background: var(--hover-bg); color: var(--text-secondary); border: 1px solid var(--border-color); padding: 6px 16px; border-radius: 8px; font-weight: 700; font-size: 0.85rem; cursor: pointer; }

.account-actions { display: flex; gap: 15px; margin-top: 20px; }
.acc-btn { flex: 1; height: 50px; border-radius: 10px; font-weight: 700; cursor: pointer; border: 1px solid var(--border-color); background: var(--card-bg); color: var(--text-primary); }
.delete-btn { border-color: #ff4d4f; color: #ff4d4f; }

@media (max-width: 992px) {
  .settings-content-wrapper { padding: 0; }
  .settings-box { flex-direction: column; border-radius: 0; border-left: none; border-right: none; margin-top: 0; }
  
  .settings-sidebar { 
    display: flex; flex: none; width: 100%; border-right: none; border-bottom: 1px solid var(--border-color); 
    overflow-x: auto; scrollbar-width: none; background: var(--card-bg);
  }
  .settings-sidebar::-webkit-scrollbar { display: none; }
  
  .sidebar-item { 
    flex: 1; min-width: 100px; text-align: center; padding: 15px 10px; font-size: 0.85rem; 
    border-left: none !important; border-bottom: 3px solid transparent;
  }
  .sidebar-item.active { border-bottom-color: var(--link-color); background: none; padding-left: 10px; }
  
  .settings-content { padding: 30px 20px; }
  
  .section-header-flex { flex-direction: column; align-items: stretch; gap: 15px; }
  .btn-sync-test { width: 100%; height: 45px; }

  .stats-dashboard-v2 { grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .stat-card-v2 { padding: 10px 5px; gap: 6px; flex-direction: column; text-align: center; }
  .stat-card-v2 .s-icon { width: 32px; height: 32px; font-size: 0.9rem; margin-bottom: 2px; }
  .stat-card-v2 .s-label { font-size: 0.65rem; }
  .stat-card-v2 .s-value { font-size: 0.9rem; }
  
  /* [시니어] 모바일 툴팁 짤림 방지 및 줄바꿈 처리 */
  .stat-tooltip-v2 {
    width: 140px; /* 너비 고정 대신 적절한 크기 부여 */
    white-space: normal; /* 줄바꿈 허용 */
    word-break: keep-all; 
    line-height: 1.4;
    padding: 10px 12px;
    font-size: 0.7rem;
    left: 50%;
    transform: translateX(-50%) translateY(10px);
    bottom: calc(100% + 10px);
  }
  .stat-card-v2:nth-child(3n+1) .stat-tooltip-v2 { left: 20%; transform: translateX(0) translateY(10px); } /* 왼쪽 정렬 */
  .stat-card-v2:nth-child(3n+1) .stat-tooltip-v2::after { left: 25px; }
  .stat-card-v2:nth-child(3n) .stat-tooltip-v2 { left: auto; right: 0; transform: translateX(0) translateY(10px); } /* 오른쪽 정렬 */
  .stat-card-v2:nth-child(3n) .stat-tooltip-v2::after { left: auto; right: 25px; }
  
  .badge-grid { grid-template-columns: 1fr; }
  
  /* [시니어 조치] 모바일 계정 정보 레이아웃 최적화 */
  .account-profile-card { flex-direction: column; text-align: center; padding: 30px 20px; }
  
  /* 모바일 레벨 대시보드 (전면 개편 대응) */
  .level-dashboard-card { 
    flex-direction: column; 
    padding: 30px 20px; 
    gap: 35px; 
    align-items: center;
    text-align: center;
  }
  .dashboard-left { width: 100%; gap: 25px; }
  .dashboard-right { width: 100%; flex: none; justify-content: center; }
  
  .user-profile-header { flex-direction: column; gap: 15px; }
  .nickname-with-toggle { justify-content: center; flex-wrap: wrap; gap: 10px; }
  .nickname-details { align-items: center; }
  
  .user-ranking-info { margin: 0; padding: 25px; }
  .rank-icon { font-size: 2.2rem; }
  .rank-num { font-size: 1.8rem; }
  
  .level-exp-container { padding: 15px; }
  .exp-header { flex-direction: column; gap: 8px; }
  .level-tip { text-align: center; }
  .username-edit-group { justify-content: center; } /* 닉네임+펜버튼 가운데 정렬 */
  
  .edit-input-wrapper { flex-direction: column; align-items: stretch; gap: 8px; }
  .username-input { max-width: 100%; }
  .edit-actions { margin-left: 0; justify-content: flex-end; } /* 버튼들은 입력창 아래 오른쪽 정렬 */

  .account-actions { flex-direction: column; align-items: stretch; }
  .acc-btn { width: 100%; justify-content: center; }
  
  .verified-actions-horizontal { flex-direction: column; }
  .responsive-input-group { flex-direction: column; }
  .btn-verify-action { height: 45px; }
}

/* [시니어] 활동 신뢰 점수 변동 이력 섹션 스타일 */
.karma-history-container { margin-top: 20px; margin-bottom: 30px; padding: 0; background: var(--hover-bg); border-radius: 20px; border: 1px solid var(--border-color); overflow: hidden; transition: all 0.3s ease; }
.karma-history-container.is-open { border-color: var(--link-color); box-shadow: 0 10px 30px rgba(0,0,0,0.05); }

.karma-history-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 25px; cursor: pointer; transition: background 0.2s; user-select: none; }
.karma-history-header:hover { background: var(--card-bg); }
.h-title-group { display: flex; flex-direction: column; gap: 4px; }
.h-title-group .sub-section-title { margin-bottom: 0 !important; }
.karma-info-tip { font-size: 0.75rem; color: var(--text-secondary); opacity: 0.8; }
.accordion-arrow { font-size: 0.8rem; color: var(--text-secondary); transition: transform 0.3s; }

.karma-history-content-wrapper { padding: 0 25px 25px; border-top: 1px dashed var(--border-color); padding-top: 20px; }
.karma-history-list { display: flex; flex-direction: column; gap: 10px; }
.karma-history-item { display: flex; justify-content: space-between; align-items: center; background: var(--card-bg); padding: 15px 20px; border-radius: 12px; border: 1px solid var(--border-color); transition: transform 0.2s; }
.karma-history-item:hover { transform: translateX(5px); border-color: var(--link-color); }

.h-main-info { display: flex; align-items: center; gap: 15px; }
.h-date { font-size: 0.8rem; color: var(--text-muted); font-weight: 600; font-family: monospace; }
.h-reason { font-size: 0.9rem; font-weight: 800; color: var(--text-primary); }

.h-side-info { display: flex; align-items: center; gap: 20px; }
.h-comment { font-size: 0.8rem; color: var(--text-secondary); max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.h-change { font-size: 1rem; font-weight: 900; min-width: 50px; text-align: right; }
.h-change.is-plus { color: #2ecc71; }
.h-change.is-minus { color: #e74c3c; }

.empty-karma-history { text-align: center; padding: 40px; color: var(--text-secondary); font-size: 0.9rem; font-style: italic; opacity: 0.6; }

/* [시니어] 활동 신뢰 점수 가이드 카드 스타일 */
.karma-guide-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 25px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.guide-title {
  font-size: 1rem;
  font-weight: 850;
  margin-bottom: 20px;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.guide-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .guide-grid {
    display: flex;
    flex-direction: row;
    overflow-x: auto;
    margin: 0 -10px 20px;
    padding: 0 10px 10px;
    gap: 15px;
    scrollbar-width: none;
    -webkit-overflow-scrolling: touch;
  }
  .guide-grid::-webkit-scrollbar { display: none; }
  
  .guide-item {
    flex: 0 0 260px;
    max-width: 260px;
  }
}

.guide-item {
  display: flex;
  gap: 12px;
  padding: 15px;
  border-radius: 12px;
  background: var(--hover-bg);
  border: 1px solid transparent;
}

.guide-item.plus { border-color: rgba(46, 204, 113, 0.2); }
.guide-item.minus { border-color: rgba(231, 76, 60, 0.2); }

.g-icon { font-size: 1.2rem; flex-shrink: 0; padding-top: 2px; }

.g-text { display: flex; flex-direction: column; flex: 1; }
.g-title { font-size: 0.9rem; font-weight: 900; color: var(--text-primary); display: block; margin-bottom: 10px; }
.guide-sub-list { list-style: none; padding: 0; margin: 0; }
.guide-sub-list li { font-size: 0.8rem; color: var(--text-secondary); line-height: 1.6; margin-bottom: 6px; position: relative; padding-left: 14px; word-break: keep-all; }
.guide-sub-list li::before { content: "•"; position: absolute; left: 0; color: var(--link-color); font-weight: bold; }
.guide-sub-list li strong { color: var(--text-primary); font-weight: 700; }

.karma-penalty-table { margin-top: 20px; background: var(--card-bg); border-radius: 12px; border: 1.5px solid var(--border-color); overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.02); }
.penalty-header { background: rgba(231, 76, 60, 0.05); padding: 12px 15px; font-size: 0.85rem; font-weight: 950; color: #e74c3c; border-bottom: 1px solid var(--border-color); }
.penalty-body { padding: 5px 0; }
.p-row { display: flex; justify-content: space-between; padding: 10px 15px; border-bottom: 1px solid var(--divider-color); transition: background 0.2s; }
.p-row:last-child { border-bottom: none; }
.p-row:hover { background: var(--hover-bg); }
.p-range { font-size: 0.8rem; font-weight: 900; color: var(--text-primary); }
.p-effect { font-size: 0.8rem; font-weight: 700; color: #ed4956; }

.guide-footer {
  font-size: 0.75rem;
  color: var(--text-secondary);
  background: rgba(0, 0, 0, 0.03);
  padding: 10px 15px;
  border-radius: 8px;
  margin: 0;
  line-height: 1.4;
}
:global(.dark-mode) .guide-footer { background: rgba(255, 255, 255, 0.05); }

/* 트랜지션 애니메이션 */
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.3s ease-out; max-height: 500px; opacity: 1; }
.fade-slide-enter-from, .fade-slide-leave-to { max-height: 0; opacity: 0; transform: translateY(-10px); }

@media (max-width: 768px) {
  .karma-history-item { flex-direction: column; align-items: flex-start; gap: 10px; }
  .h-side-info { width: 100%; justify-content: space-between; border-top: 1px dashed var(--border-color); padding-top: 10px; }
  .h-comment { max-width: 100%; }
}

/* [시니어] EXP 활동 점수 가이드 카드 스타일 추가 */
.exp-activity-guide-card { 
  background: linear-gradient(135deg, rgba(52, 152, 219, 0.05), rgba(52, 152, 219, 0.02)); 
  padding: 25px; 
  border-radius: 20px; 
  border: 1px solid var(--border-color); 
  margin-bottom: 30px; 
  box-shadow: 0 4px 20px rgba(0,0,0,0.02); 
  overflow: hidden; /* 영역 밖 스크롤 제어 */
}
.guide-desc { font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 20px; font-weight: 600; }

/* [시니어] 모바일 가로 스크롤 레이아웃 최적화 */
.exp-grid { 
  display: flex; 
  flex-direction: column; 
  gap: 15px; 
}

@media (max-width: 768px) {
  .exp-grid {
    flex-direction: row;
    overflow-x: auto;
    padding-bottom: 15px;
    margin: 0 -10px; /* 좌우 여백 확보 */
    padding: 0 10px 15px;
    scrollbar-width: none; /* 파이어폭스용 */
    -webkit-overflow-scrolling: touch;
  }
  .exp-grid::-webkit-scrollbar { display: none; } /* 크롬용 */
  
  .exp-item {
    flex: 0 0 280px; /* 카드의 고정 너비 지정 */
    max-width: 280px;
  }
}

.exp-item { display: flex; align-items: flex-start; gap: 15px; padding: 18px; background: var(--card-bg); border-radius: 15px; border: 1px solid var(--border-color); transition: all 0.25s; }
.exp-item:hover { transform: translateX(8px); border-color: #3498db; box-shadow: 0 5px 15px rgba(52, 152, 219, 0.1); }
.exp-icon { font-size: 1.6rem; filter: drop-shadow(0 4px 8px rgba(0,0,0,0.1)); flex-shrink: 0; }
.exp-text { flex: 1; }
.exp-name { font-size: 0.95rem; font-weight: 950; color: var(--text-primary); display: block; margin-bottom: 5px; }
.exp-text p { font-size: 0.82rem; color: var(--text-secondary); line-height: 1.6; margin-bottom: 12px; word-break: keep-all; }
.exp-text b { color: var(--text-primary); font-weight: 800; }
.point-badge-group { display: flex; flex-wrap: wrap; gap: 8px; }
.exp-points { font-size: 0.72rem; font-weight: 900; color: #3498db; background: rgba(52, 152, 219, 0.08); padding: 4px 10px; border-radius: 8px; border: 1px solid rgba(52, 152, 219, 0.15); }
</style>