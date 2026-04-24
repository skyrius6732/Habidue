<template>
  <div class="wedding-wrap" v-if="invitation">

    <!-- 배경 꽃잎 -->
    <div class="petals">
      <span v-for="n in 12" :key="n" class="petal" :style="petalStyle(n)">🌸</span>
    </div>

    <!-- 히어로 섹션 -->
    <section class="hero">
      <p class="hero-label">우리 결혼합니다</p>
      <h1 class="hero-names">
        {{ invitation.groomName }} <span class="amp">&amp;</span> {{ invitation.brideName }}
      </h1>
      <p class="hero-date">{{ formatDate(invitation.weddingDate) }}</p>
      <p class="hero-venue">{{ invitation.venueName }}</p>
    </section>

    <!-- D-day 카운터 -->
    <section class="card dday-card">
      <template v-if="dday > 0">
        <span class="dday-label">결혼식까지</span>
        <span class="dday-count">D-{{ dday }}</span>
      </template>
      <template v-else-if="dday === 0">
        <span class="dday-count">D-Day 🎉</span>
      </template>
      <template v-else>
        <span class="dday-label">결혼한 지</span>
        <span class="dday-count">D+{{ Math.abs(dday) }}</span>
      </template>
    </section>

    <!-- 인사말 -->
    <section class="card greeting-card" v-if="invitation.greetingMessage">
      <p class="greeting-text">{{ invitation.greetingMessage }}</p>
      <div class="family-info" v-if="hasParentsInfo">
        <div class="family-row">
          <span class="family-side">신랑측</span>
          <span class="family-names">
            <span v-if="invitation.groomFatherName">{{ invitation.groomFatherName }} · </span>
            <span v-if="invitation.groomMotherName">{{ invitation.groomMotherName }}</span>
            의 아들 <strong>{{ invitation.groomName }}</strong>
          </span>
        </div>
        <div class="family-row">
          <span class="family-side">신부측</span>
          <span class="family-names">
            <span v-if="invitation.brideFatherName">{{ invitation.brideFatherName }} · </span>
            <span v-if="invitation.brideMotherName">{{ invitation.brideMotherName }}</span>
            의 딸 <strong>{{ invitation.brideName }}</strong>
          </span>
        </div>
      </div>
    </section>

    <!-- 갤러리 -->
    <section class="gallery-section" v-if="invitation.photos?.length">
      <div class="gallery-scroll">
        <img
          v-for="photo in invitation.photos"
          :key="photo.id"
          :src="photo.imageUrl"
          class="gallery-img"
          @click="openPhoto(photo.imageUrl)"
          alt="웨딩 사진"
        />
      </div>
    </section>

    <!-- 사진 전체보기 모달 -->
    <div class="photo-modal" v-if="activePhoto" @click="activePhoto = null">
      <img :src="activePhoto" class="modal-img" />
    </div>

    <!-- 예식 안내 -->
    <section class="card venue-card">
      <h2 class="card-title">예식 안내</h2>
      <div class="venue-info">
        <p class="venue-name">{{ invitation.venueName }}</p>
        <p class="venue-address">{{ invitation.venueAddress }}
          <span v-if="invitation.venueDetailAddress"> {{ invitation.venueDetailAddress }}</span>
        </p>
        <p class="venue-datetime">{{ formatDateFull(invitation.weddingDate) }}</p>
      </div>
      <div class="map-buttons">
        <a
          v-if="invitation.venueLat && invitation.venueLng"
          :href="`https://map.kakao.com/link/map/${invitation.venueName},${invitation.venueLat},${invitation.venueLng}`"
          target="_blank"
          class="btn-map kakao"
        >카카오맵으로 보기</a>
        <a
          v-if="invitation.venueLat && invitation.venueLng"
          :href="`https://map.naver.com/v5/search/${encodeURIComponent(invitation.venueAddress)}`"
          target="_blank"
          class="btn-map naver"
        >네이버지도로 보기</a>
      </div>
    </section>

    <!-- 교통/주차 안내 -->
    <section class="card transport-card" v-if="invitation.transportInfo || invitation.parkingInfo">
      <h2 class="card-title">오시는 길</h2>
      <pre class="transport-text" v-if="invitation.transportInfo">{{ invitation.transportInfo }}</pre>
      <p class="parking-text" v-if="invitation.parkingInfo">🅿️ {{ invitation.parkingInfo }}</p>
    </section>

    <!-- 마음 전하실 곳 (계좌) -->
    <section class="card account-card" v-if="invitation.accounts?.length">
      <h2 class="card-title">마음 전하실 곳</h2>
      <div
        v-for="acc in invitation.accounts"
        :key="acc.id"
        class="account-row"
      >
        <span class="account-side-badge" :class="acc.side.toLowerCase()">
          {{ acc.side === 'GROOM' ? '신랑' : '신부' }}측
        </span>
        <div class="account-detail">
          <span class="bank-name">{{ acc.bankName }}</span>
          <span class="account-number">{{ acc.accountNumber }}</span>
          <span class="account-holder">{{ acc.accountHolder }}</span>
        </div>
        <button class="btn-copy" @click="copyAccount(acc)">복사</button>
        <a v-if="acc.kakaoPayLink" :href="acc.kakaoPayLink" target="_blank" class="btn-kakaopay">카카오페이</a>
      </div>
    </section>

    <!-- RSVP -->
    <section class="card rsvp-card">
      <h2 class="card-title">참석 여부 전달</h2>
      <p class="rsvp-desc">참석 여부를 알려주시면 준비에 큰 도움이 됩니다.</p>
      <form class="rsvp-form" @submit.prevent="submitRsvp">
        <div class="form-row">
          <input v-model="rsvp.name" placeholder="이름 *" required class="form-input" />
          <input v-model="rsvp.phone" placeholder="연락처 (선택)" class="form-input" />
        </div>
        <div class="attendance-btns">
          <button
            type="button"
            class="btn-attendance"
            :class="{ active: rsvp.attendance === true }"
            @click="rsvp.attendance = true"
          >참석합니다 👍</button>
          <button
            type="button"
            class="btn-attendance decline"
            :class="{ active: rsvp.attendance === false }"
            @click="rsvp.attendance = false"
          >불참합니다 😢</button>
        </div>
        <template v-if="rsvp.attendance === true">
          <div class="form-row">
            <div class="form-field">
              <label class="form-label">인원</label>
              <select v-model="rsvp.guestCount" class="form-select">
                <option v-for="n in 10" :key="n" :value="n">{{ n }}명</option>
              </select>
            </div>
            <div class="form-field">
              <label class="form-label">식사 여부</label>
              <select v-model="rsvp.mealOption" class="form-select">
                <option :value="true">식사 예정</option>
                <option :value="false">식사 안 함</option>
              </select>
            </div>
          </div>
          <div class="form-field">
            <label class="form-label">신랑/신부 측</label>
            <select v-model="rsvp.side" class="form-select">
              <option value="GROOM">신랑 측</option>
              <option value="BRIDE">신부 측</option>
              <option value="UNKNOWN">미지정</option>
            </select>
          </div>
        </template>
        <textarea v-model="rsvp.message" placeholder="축하 메시지 (선택)" class="form-textarea"></textarea>
        <button type="submit" class="btn-submit" :disabled="rsvp.attendance === null || rsvpSent">
          {{ rsvpSent ? '전달 완료 ✓' : '전달하기' }}
        </button>
      </form>
    </section>

    <!-- 방명록 -->
    <section class="card guestbook-card">
      <h2 class="card-title">방명록</h2>
      <form class="guestbook-form" @submit.prevent="submitGuestbook">
        <div class="form-row">
          <input v-model="gbForm.name" placeholder="이름 *" required class="form-input" />
          <input v-model="gbForm.password" type="password" placeholder="비밀번호 *" required class="form-input" />
        </div>
        <div class="form-row-full">
          <textarea v-model="gbForm.message" placeholder="축하 메시지를 남겨주세요 *" required class="form-textarea"></textarea>
          <button type="submit" class="btn-submit-sm">등록</button>
        </div>
      </form>
      <div class="guestbook-list">
        <div v-if="guestbook.length === 0" class="empty-gb">첫 번째 축하 메시지를 남겨주세요 💌</div>
        <div v-for="entry in guestbook" :key="entry.id" class="gb-entry">
          <div class="gb-header">
            <span class="gb-name">{{ entry.name }}</span>
            <span class="gb-date">{{ formatShortDate(entry.createdAt) }}</span>
            <button class="gb-delete" @click="deleteGuestbook(entry)">삭제</button>
          </div>
          <p class="gb-message">{{ entry.message }}</p>
        </div>
      </div>
    </section>

    <!-- 음악 재생 버튼 -->
    <button
      v-if="invitation.musicUrl"
      class="music-btn"
      @click="toggleMusic"
      :title="musicPlaying ? '음악 끄기' : '음악 켜기'"
    >{{ musicPlaying ? '🔊' : '🔇' }}</button>
    <audio ref="audioRef" :src="invitation.musicUrl" loop></audio>

    <!-- 복사 토스트 -->
    <div class="toast" :class="{ show: toastVisible }">{{ toastMessage }}</div>
  </div>

  <!-- 로딩 -->
  <div class="wedding-loading" v-else-if="loading">
    <p>🌸 청첩장을 불러오는 중...</p>
  </div>

  <!-- 에러 -->
  <div class="wedding-error" v-else>
    <p>{{ error || '청첩장을 찾을 수 없습니다.' }}</p>
  </div>
</template>

<script>
import { useWeddingStore } from '../stores/wedding'

export default {
  name: 'WeddingView',

  data() {
    return {
      invitation: null,
      guestbook: [],
      loading: true,
      error: null,
      activePhoto: null,
      musicPlaying: false,
      toastVisible: false,
      toastMessage: '',
      rsvpSent: false,
      rsvp: {
        name: '',
        phone: '',
        attendance: null,
        guestCount: 1,
        mealOption: true,
        side: 'UNKNOWN',
        message: '',
      },
      gbForm: {
        name: '',
        password: '',
        message: '',
      },
    }
  },

  computed: {
    dday() {
      if (!this.invitation?.weddingDate) return null
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      const wedding = new Date(this.invitation.weddingDate)
      wedding.setHours(0, 0, 0, 0)
      return Math.ceil((wedding - today) / (1000 * 60 * 60 * 24))
    },
    hasParentsInfo() {
      const i = this.invitation
      return i?.groomFatherName || i?.groomMotherName || i?.brideFatherName || i?.brideMotherName
    },
  },

  async created() {
    const store = useWeddingStore()
    const slug = this.$route.params.slug
    try {
      this.invitation = await store.fetchBySlug(slug)
      await store.fetchGuestbook(slug)
      this.guestbook = store.guestbook
    } catch (e) {
      this.error = store.error
    } finally {
      this.loading = false
    }

    if (this.invitation?.musicAutoPlay && this.invitation?.musicUrl) {
      this.$nextTick(() => this.playMusic())
    }
  },

  mounted() {
    // [시니어 조치] 브라우저 정책상 자동 재생이 차단될 수 있으므로, 첫 클릭/터치 시 재생되도록 리스너 등록
    const handleFirstInteraction = () => {
      if (!this.musicPlaying && this.invitation?.musicAutoPlay) {
        this.playMusic()
      }
      document.removeEventListener('click', handleFirstInteraction)
      document.removeEventListener('touchstart', handleFirstInteraction)
    }
    document.addEventListener('click', handleFirstInteraction)
    document.addEventListener('touchstart', handleFirstInteraction)
  },

  methods: {
    formatDate(dt) {
      if (!dt) return ''
      const d = new Date(dt)
      return `${d.getFullYear()}. ${d.getMonth() + 1}. ${d.getDate()}`
    },
    formatDateFull(dt) {
      if (!dt) return ''
      const d = new Date(dt)
      const days = ['일', '월', '화', '수', '목', '금', '토']
      return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일 (${days[d.getDay()]}) ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    },
    formatShortDate(dt) {
      if (!dt) return ''
      const d = new Date(dt)
      return `${d.getMonth() + 1}.${d.getDate()}`
    },
    petalStyle(n) {
      return {
        left: `${(n * 8.3) % 100}%`,
        animationDelay: `${(n * 0.7) % 7}s`,
        animationDuration: `${7 + (n % 5)}s`,
        fontSize: `${12 + (n % 8)}px`,
      }
    },
    openPhoto(url) {
      this.activePhoto = url
    },
    toggleMusic() {
      const audio = this.$refs.audioRef
      if (this.musicPlaying) {
        audio.pause()
        this.musicPlaying = false
      } else {
        this.playMusic()
      }
    },
    playMusic() {
      const audio = this.$refs.audioRef
      audio.play().catch(() => {})
      this.musicPlaying = true
    },
    async copyAccount(acc) {
      const text = `${acc.bankName} ${acc.accountNumber} (${acc.accountHolder})`
      await navigator.clipboard.writeText(text)
      this.showToast('계좌번호가 복사되었습니다.')
    },
    showToast(msg) {
      this.toastMessage = msg
      this.toastVisible = true
      setTimeout(() => { this.toastVisible = false }, 2500)
    },
    async submitRsvp() {
      const store = useWeddingStore()
      try {
        await store.submitRsvp(this.$route.params.slug, this.rsvp)
        this.rsvpSent = true
        this.showToast('참석 여부가 전달되었습니다 💌')
      } catch {
        this.showToast('전달에 실패했습니다. 다시 시도해주세요.')
      }
    },
    async submitGuestbook() {
      const store = useWeddingStore()
      try {
        await store.writeGuestbook(this.$route.params.slug, this.gbForm)
        this.guestbook = store.guestbook
        this.gbForm = { name: '', password: '', message: '' }
        this.showToast('방명록에 등록되었습니다 ✓')
      } catch {
        this.showToast('등록에 실패했습니다.')
      }
    },
    async deleteGuestbook(entry) {
      const pw = prompt('삭제하려면 비밀번호를 입력하세요.')
      if (!pw) return
      const store = useWeddingStore()
      try {
        await store.deleteGuestbook(entry.id, pw)
        this.guestbook = store.guestbook
        this.showToast('삭제되었습니다.')
      } catch {
        this.showToast('비밀번호가 일치하지 않습니다.')
      }
    },
  },
}
</script>

<style scoped>
* { box-sizing: border-box; }

.wedding-wrap {
  font-family: 'Noto Serif KR', 'Georgia', serif;
  background: #fffaf7;
  min-height: 100vh;
  max-width: 480px;
  margin: 0 auto;
  padding-bottom: 80px;
  position: relative;
  overflow: hidden;
}

/* 꽃잎 */
.petals { position: fixed; top: 0; left: 0; right: 0; pointer-events: none; z-index: 0; max-width: 480px; margin: 0 auto; }
.petal {
  position: absolute;
  animation: fall linear infinite;
  opacity: 0.6;
  top: -20px;
}
@keyframes fall {
  0%   { transform: translateY(-20px) rotate(0deg); opacity: 0.7; }
  100% { transform: translateY(110vh) rotate(360deg); opacity: 0; }
}

/* 히어로 */
.hero {
  text-align: center;
  padding: 80px 24px 48px;
  position: relative;
  background: linear-gradient(180deg, #fff5f5 0%, #fffaf7 100%);
}
.hero-label { font-size: 13px; color: #c8a4a4; letter-spacing: 3px; margin-bottom: 16px; }
.hero-names { font-size: 32px; color: #4a3728; font-weight: 400; margin: 0 0 16px; }
.amp { color: #c8a4a4; margin: 0 8px; }
.hero-date { font-size: 15px; color: #7a6a60; margin-bottom: 6px; letter-spacing: 1px; }
.hero-venue { font-size: 13px; color: #a89080; }

/* 카드 공통 */
.card {
  background: #fff;
  border-radius: 16px;
  padding: 28px 24px;
  margin: 0 16px 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  position: relative;
  z-index: 1;
}
.card-title {
  font-size: 14px;
  color: #c8a4a4;
  letter-spacing: 2px;
  text-align: center;
  margin: 0 0 20px;
  font-weight: 400;
}

/* D-day */
.dday-card { text-align: center; padding: 20px; margin-bottom: 16px; }
.dday-label { font-size: 13px; color: #a89080; display: block; margin-bottom: 4px; }
.dday-count { font-size: 36px; color: #c8a4a4; font-weight: 300; letter-spacing: 2px; }

/* 인사말 */
.greeting-text { font-size: 14px; line-height: 2; color: #5a4a40; text-align: center; white-space: pre-wrap; margin-bottom: 24px; }
.family-info { border-top: 1px solid #f0e8e4; padding-top: 20px; }
.family-row { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; font-size: 13px; color: #7a6a60; }
.family-side { background: #fff5f5; color: #c8a4a4; padding: 3px 10px; border-radius: 20px; font-size: 11px; white-space: nowrap; }

/* 갤러리 */
.gallery-section { margin: 0 0 16px; position: relative; z-index: 1; }
.gallery-scroll { display: flex; gap: 8px; overflow-x: auto; padding: 0 16px; scroll-snap-type: x mandatory; -webkit-overflow-scrolling: touch; }
.gallery-scroll::-webkit-scrollbar { display: none; }
.gallery-img { width: 240px; height: 320px; object-fit: cover; border-radius: 12px; flex-shrink: 0; scroll-snap-align: start; cursor: pointer; transition: transform 0.2s; }
.gallery-img:hover { transform: scale(0.98); }

/* 사진 모달 */
.photo-modal { position: fixed; inset: 0; background: rgba(0,0,0,0.9); z-index: 100; display: flex; align-items: center; justify-content: center; }
.modal-img { max-width: 100%; max-height: 90vh; border-radius: 8px; object-fit: contain; }

/* 예식 안내 */
.venue-info { text-align: center; margin-bottom: 20px; }
.venue-name { font-size: 18px; color: #4a3728; margin-bottom: 6px; }
.venue-address { font-size: 13px; color: #7a6a60; margin-bottom: 8px; }
.venue-datetime { font-size: 14px; color: #c8a4a4; font-weight: 500; }
.map-buttons { display: flex; gap: 10px; justify-content: center; }
.btn-map { padding: 10px 20px; border-radius: 8px; font-size: 13px; text-decoration: none; font-weight: 500; transition: opacity 0.2s; }
.btn-map:hover { opacity: 0.85; }
.btn-map.kakao { background: #FEE500; color: #3C1E1E; }
.btn-map.naver { background: #03C75A; color: #fff; }

/* 교통/주차 */
.transport-text { font-size: 13px; color: #5a4a40; line-height: 1.9; white-space: pre-wrap; font-family: inherit; margin: 0 0 12px; }
.parking-text { font-size: 13px; color: #7a6a60; }

/* 계좌 */
.account-row { display: flex; align-items: center; gap: 10px; padding: 14px 0; border-bottom: 1px solid #f0e8e4; flex-wrap: wrap; }
.account-row:last-child { border-bottom: none; }
.account-side-badge { padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; }
.account-side-badge.groom { background: #e8f4fd; color: #5b9bd5; }
.account-side-badge.bride { background: #fdeaf0; color: #e87fa0; }
.account-detail { flex: 1; font-size: 13px; color: #5a4a40; display: flex; flex-direction: column; gap: 2px; }
.bank-name { font-weight: 600; }
.account-number { color: #7a6a60; }
.account-holder { font-size: 12px; color: #a89080; }
.btn-copy { background: #f5f0ec; color: #7a6a60; border: none; padding: 7px 14px; border-radius: 8px; font-size: 12px; cursor: pointer; white-space: nowrap; }
.btn-kakaopay { background: #FEE500; color: #3C1E1E; text-decoration: none; padding: 7px 14px; border-radius: 8px; font-size: 12px; white-space: nowrap; }

/* RSVP */
.rsvp-desc { font-size: 13px; color: #a89080; text-align: center; margin-bottom: 20px; }
.rsvp-form { display: flex; flex-direction: column; gap: 12px; }
.form-row { display: flex; gap: 10px; flex-wrap: wrap; }
.form-row .form-input { flex: 1; min-width: 140px; }
.form-row-full { display: flex; flex-direction: column; gap: 8px; }
.form-field { display: flex; flex-direction: column; gap: 4px; flex: 1; min-width: 140px; }
.form-label { font-size: 12px; color: #a89080; }
.form-input { width: 100%; padding: 12px 14px; border: 1px solid #f0e8e4; border-radius: 10px; font-size: 14px; background: #fffaf7; outline: none; font-family: inherit; }
.form-input:focus { border-color: #c8a4a4; }
.form-select { padding: 12px 14px; border: 1px solid #f0e8e4; border-radius: 10px; font-size: 14px; background: #fffaf7; outline: none; font-family: inherit; width: 100%; }
.form-textarea { padding: 12px 14px; border: 1px solid #f0e8e4; border-radius: 10px; font-size: 14px; background: #fffaf7; outline: none; resize: none; height: 90px; font-family: inherit; width: 100%; }
.form-textarea:focus { border-color: #c8a4a4; }
.attendance-btns { display: flex; gap: 10px; }
.btn-attendance { flex: 1; padding: 14px; border: 2px solid #f0e8e4; border-radius: 10px; background: #fff; font-size: 14px; cursor: pointer; transition: all 0.2s; font-family: inherit; color: #7a6a60; }
.btn-attendance.active { border-color: #7eb87e; background: #f0f9f0; color: #4a7a4a; }
.btn-attendance.decline.active { border-color: #e8a0a0; background: #fdf0f0; color: #904040; }
.btn-submit { width: 100%; padding: 16px; background: #c8a4a4; color: #fff; border: none; border-radius: 12px; font-size: 16px; cursor: pointer; font-family: inherit; transition: background 0.2s; }
.btn-submit:hover:not(:disabled) { background: #b08080; }
.btn-submit:disabled { background: #d4c4c4; cursor: default; }

/* 방명록 */
.guestbook-form { margin-bottom: 20px; display: flex; flex-direction: column; gap: 10px; }
.guestbook-form .form-row .form-input { min-width: 120px; }
.btn-submit-sm { align-self: flex-end; padding: 12px 24px; background: #c8a4a4; color: #fff; border: none; border-radius: 10px; font-size: 14px; cursor: pointer; font-family: inherit; }
.empty-gb { text-align: center; color: #c0b0a8; font-size: 14px; padding: 24px 0; }
.gb-entry { padding: 16px 0; border-bottom: 1px solid #f0e8e4; }
.gb-entry:last-child { border-bottom: none; }
.gb-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.gb-name { font-size: 14px; color: #4a3728; font-weight: 600; }
.gb-date { font-size: 12px; color: #a89080; flex: 1; }
.gb-delete { background: none; border: none; color: #ccc; font-size: 11px; cursor: pointer; padding: 2px 6px; }
.gb-delete:hover { color: #e87f7f; }
.gb-message { font-size: 14px; color: #5a4a40; line-height: 1.7; margin: 0; }

/* 음악 버튼 */
.music-btn { position: fixed; bottom: 24px; right: 24px; width: 48px; height: 48px; border-radius: 50%; background: #fff; border: 2px solid #f0e8e4; font-size: 22px; cursor: pointer; box-shadow: 0 4px 12px rgba(0,0,0,0.1); z-index: 50; display: flex; align-items: center; justify-content: center; }

/* 토스트 */
.toast { position: fixed; bottom: 80px; left: 50%; transform: translateX(-50%) translateY(20px); background: rgba(74,55,40,0.88); color: #fff; padding: 12px 24px; border-radius: 24px; font-size: 14px; opacity: 0; transition: all 0.3s; pointer-events: none; white-space: nowrap; z-index: 200; }
.toast.show { opacity: 1; transform: translateX(-50%) translateY(0); }

/* 로딩/에러 */
.wedding-loading, .wedding-error { display: flex; align-items: center; justify-content: center; height: 100vh; font-size: 16px; color: #a89080; font-family: 'Noto Serif KR', serif; }
</style>
