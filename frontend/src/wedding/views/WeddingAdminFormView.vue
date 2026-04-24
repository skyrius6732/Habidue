<template>
  <div class="form-admin-container">
    <div class="admin-header">
      <button class="btn-back" @click="$router.push({ name: 'adminWedding' })">← 목록으로</button>
      <h2>{{ isEdit ? '청첩장 수정' : '새 청첩장 만들기' }}</h2>
    </div>

    <form class="inv-form" @submit.prevent="save">

      <!-- 기본 정보 -->
      <div class="form-section">
        <h3 class="section-title">기본 정보</h3>
        <div class="form-grid">
          <div class="form-field full">
            <label>URL Slug *</label>
            <input v-model="form.slug" placeholder="예: sujin_jeongho" required class="form-input" />
            <span class="field-hint">habidue.com/wedding/{{ form.slug || 'slug' }}</span>
          </div>
          <div class="form-field">
            <label>신랑 이름 *</label>
            <input v-model="form.groomName" required class="form-input" />
          </div>
          <div class="form-field">
            <label>신부 이름 *</label>
            <input v-model="form.brideName" required class="form-input" />
          </div>
          <div class="form-field">
            <label>신랑 아버지</label>
            <input v-model="form.groomFatherName" class="form-input" />
          </div>
          <div class="form-field">
            <label>신랑 어머니</label>
            <input v-model="form.groomMotherName" class="form-input" />
          </div>
          <div class="form-field">
            <label>신부 아버지</label>
            <input v-model="form.brideFatherName" class="form-input" />
          </div>
          <div class="form-field">
            <label>신부 어머니</label>
            <input v-model="form.brideMotherName" class="form-input" />
          </div>
        </div>
      </div>

      <!-- 예식 정보 -->
      <div class="form-section">
        <h3 class="section-title">예식 정보</h3>
        <div class="form-grid">
          <div class="form-field full">
            <label>예식 일시 *</label>
            <input v-model="form.weddingDate" type="datetime-local" required class="form-input" />
          </div>
          <div class="form-field full">
            <label>예식장 이름 *</label>
            <input v-model="form.venueName" required class="form-input" />
          </div>
          <div class="form-field full">
            <label>주소 *</label>
            <input v-model="form.venueAddress" required class="form-input" />
          </div>
          <div class="form-field full">
            <label>상세 주소</label>
            <input v-model="form.venueDetailAddress" class="form-input" />
          </div>
          <div class="form-field">
            <label>위도 (지도)</label>
            <input v-model.number="form.venueLat" type="number" step="any" class="form-input" />
          </div>
          <div class="form-field">
            <label>경도 (지도)</label>
            <input v-model.number="form.venueLng" type="number" step="any" class="form-input" />
          </div>
        </div>
      </div>

      <!-- 안내 문구 -->
      <div class="form-section">
        <h3 class="section-title">안내 문구</h3>
        <div class="form-grid">
          <div class="form-field full">
            <label>인사말</label>
            <textarea v-model="form.greetingMessage" rows="5" class="form-textarea"></textarea>
          </div>
          <div class="form-field full">
            <label>교통 안내</label>
            <textarea v-model="form.transportInfo" rows="4" class="form-textarea" placeholder="지하철, 버스 등 교통 정보"></textarea>
          </div>
          <div class="form-field full">
            <label>주차 안내</label>
            <input v-model="form.parkingInfo" class="form-input" />
          </div>
        </div>
      </div>

      <!-- 배경 음악 -->
      <div class="form-section">
        <h3 class="section-title">배경 음악</h3>
        <div class="bgm-notice">
          🎵 저작권 무료 BGM 찾기 →
          <a href="https://pixabay.com/music/search/wedding/" target="_blank" class="bgm-link">Pixabay 웨딩 BGM</a>
          <a href="https://www.bensound.com/royalty-free-music/wedding" target="_blank" class="bgm-link">Bensound</a>
          <span class="bgm-hint">곡 다운로드 후 아래 업로드 버튼으로 등록하세요</span>
        </div>
        <div class="form-grid">
          <div class="form-field full">
            <label>음악 파일 업로드 <span class="label-sub">(수정 시 사용 가능)</span></label>
            <div class="music-upload-row">
              <input type="file" accept="audio/*" ref="musicInput" @change="uploadMusic" class="file-input" />
              <button
                type="button"
                class="btn-upload"
                :disabled="!isEdit || musicUploading"
                @click="$refs.musicInput.click()"
              >{{ musicUploading ? '업로드 중...' : '🎵 음악 파일 업로드' }}</button>
              <span v-if="!isEdit" class="bgm-hint">청첩장 생성 후 수정에서 업로드 가능합니다</span>
            </div>
          </div>
          <div class="form-field full">
            <label>음악 URL (.mp3 직접 링크)</label>
            <input v-model="form.musicUrl" class="form-input" placeholder="업로드 후 자동 입력 또는 직접 URL 입력" />
          </div>
          <div class="form-field">
            <label class="checkbox-label">
              <input type="checkbox" v-model="form.musicAutoPlay" />
              자동 재생
            </label>
          </div>
        </div>
      </div>

      <!-- 계좌 정보 -->
      <div class="form-section">
        <h3 class="section-title">계좌 정보</h3>
        <div v-for="(acc, idx) in form.accounts" :key="idx" class="account-block">
          <div class="account-block-header">
            <select v-model="acc.side" class="form-select-sm">
              <option value="GROOM">신랑측</option>
              <option value="BRIDE">신부측</option>
            </select>
            <button type="button" class="btn-remove" @click="removeAccount(idx)">삭제</button>
          </div>
          <div class="form-grid">
            <div class="form-field">
              <label>은행명</label>
              <input v-model="acc.bankName" class="form-input" placeholder="예: 카카오뱅크" />
            </div>
            <div class="form-field">
              <label>계좌번호</label>
              <input v-model="acc.accountNumber" class="form-input" />
            </div>
            <div class="form-field">
              <label>예금주</label>
              <input v-model="acc.accountHolder" class="form-input" />
            </div>
            <div class="form-field">
              <label>카카오페이 링크</label>
              <input v-model="acc.kakaoPayLink" class="form-input" />
            </div>
          </div>
        </div>
        <button type="button" class="btn-add-account" @click="addAccount">+ 계좌 추가</button>
      </div>

      <!-- 갤러리 사진 -->
      <div class="form-section">
        <h3 class="section-title">갤러리 사진</h3>
        
        <div class="photo-grid">
          <!-- 서버에 이미 저장된 사진들 (드래그로 순서 변경) -->
          <div
            v-for="(photo, idx) in currentPhotos"
            :key="photo.id"
            class="photo-item"
            :class="{ 'drag-over': dragOverIdx === idx }"
            draggable="true"
            @dragstart="dragStart(idx)"
            @dragover.prevent="dragOverIdx = idx"
            @dragleave="dragOverIdx = null"
            @drop.prevent="drop(idx)"
            @dragend="dragOverIdx = null"
          >
            <span class="drag-handle">⠿</span>
            <img :src="photo.imageUrl" class="thumb-img" />
            <button type="button" class="btn-delete-photo" @click="deletePhoto(photo)">✕</button>
          </div>

          <!-- 새로 선택한 사진들 (미리보기) -->
          <div v-for="(url, idx) in previews" :key="`new-${idx}`" class="photo-item new-preview">
            <img :src="url" class="thumb-img" />
            <button type="button" class="btn-delete-photo" @click="removeNewFile(idx)">✕</button>
            <span class="new-badge">NEW</span>
          </div>
        </div>

        <div class="upload-area">
          <input type="file" multiple accept="image/*" ref="photoInput" @change="uploadPhotos" class="file-input" />
          <button type="button" class="btn-upload" @click="$refs.photoInput.click()">📷 사진 추가</button>
          <span class="upload-hint">최대 50MB, 다중 선택 가능</span>
        </div>
      </div>

      <!-- 공개 설정 -->
      <div class="form-section">
        <h3 class="section-title">공개 설정</h3>
        <div class="form-grid">
          <div class="form-field">
            <label>상태</label>
            <select v-model="form.status" class="form-input">
              <option value="DRAFT">초안 (비공개)</option>
              <option value="ACTIVE">공개중</option>
              <option value="EXPIRED">만료</option>
            </select>
          </div>
          <div class="form-field">
            <label>만료일</label>
            <input v-model="form.expiresAt" type="datetime-local" class="form-input" />
          </div>
        </div>
      </div>

      <!-- 저장 버튼 -->
      <div class="form-actions">
        <button type="button" class="btn-cancel" @click="$router.push({ name: 'adminWedding' })">취소</button>
        <button type="submit" class="btn-save" :disabled="saving">
          {{ saving ? '저장 중...' : (isEdit ? '수정 완료' : '청첩장 생성') }}
        </button>
      </div>
    </form>
  </div>
</template>

<script>
import axios from '@/plugins/axios'

const defaultForm = () => ({
  slug: '',
  groomName: '',
  brideName: '',
  groomFatherName: '',
  groomMotherName: '',
  brideFatherName: '',
  brideMotherName: '',
  weddingDate: '',
  venueName: '',
  venueAddress: '',
  venueDetailAddress: '',
  venueLat: null,
  venueLng: null,
  greetingMessage: '',
  transportInfo: '',
  parkingInfo: '',
  musicUrl: '',
  musicAutoPlay: false,
  template: 'default',
  status: 'DRAFT',
  expiresAt: '',
  accounts: [],
})

export default {
  name: 'WeddingAdminFormView',
  data() {
    return {
      form: defaultForm(),
      currentPhotos: [],
      deletedPhotoIds: [],
      newFiles: [],
      previews: [],
      saving: false,
      musicUploading: false,
      dragIdx: null,
      dragOverIdx: null,
    }
  },
  computed: {
    isEdit() {
      return !!this.$route.params.id
    },
  },
  async created() {
    if (this.isEdit) {
      const res = await axios.get(`/api/admin/wedding/${this.$route.params.id}`)
      const inv = res.data.data
      this.currentPhotos = inv.photos || []
      this.form = {
        slug: inv.slug,
        groomName: inv.groomName,
        brideName: inv.brideName,
        groomFatherName: inv.groomFatherName || '',
        groomMotherName: inv.groomMotherName || '',
        brideFatherName: inv.brideFatherName || '',
        brideMotherName: inv.brideMotherName || '',
        weddingDate: this.toDatetimeLocal(inv.weddingDate),
        venueName: inv.venueName,
        venueAddress: inv.venueAddress,
        venueDetailAddress: inv.venueDetailAddress || '',
        venueLat: inv.venueLat,
        venueLng: inv.venueLng,
        greetingMessage: inv.greetingMessage || '',
        transportInfo: inv.transportInfo || '',
        parkingInfo: inv.parkingInfo || '',
        musicUrl: inv.musicUrl || '',
        musicAutoPlay: inv.musicAutoPlay,
        template: inv.template || 'default',
        status: inv.status,
        expiresAt: inv.expiresAt ? this.toDatetimeLocal(inv.expiresAt) : '',
        accounts: (inv.accounts || []).map(a => ({ ...a })),
      }
    }
  },
  methods: {
    toDatetimeLocal(dt) {
      if (!dt) return ''
      // [시니어 조치] Date 객체를 거치면 타임존 변환으로 인해 날짜가 틀어질 수 있으므로
      // ISO 문자열에서 날짜와 시간 부분만 추출 (YYYY-MM-DDTHH:mm)
      if (typeof dt === 'string' && dt.includes('T')) {
        return dt.substring(0, 16)
      }
      const d = new Date(dt)
      if (isNaN(d.getTime())) return ''
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    addAccount() {
      this.form.accounts.push({ side: 'GROOM', bankName: '', accountNumber: '', accountHolder: '', kakaoPayLink: '' })
    },
    removeAccount(idx) {
      this.form.accounts.splice(idx, 1)
    },
    async save() {
      this.saving = true
      try {
        const payload = {
          ...this.form,
          weddingDate: this.form.weddingDate ? this.form.weddingDate : null,
          expiresAt: this.form.expiresAt ? this.form.expiresAt : null,
          venueLat: this.form.venueLat || null,
          venueLng: this.form.venueLng || null,
          deletedPhotoIds: this.deletedPhotoIds, // [시니어 조치] 삭제할 사진 ID 목록 포함
        }

        const formData = new FormData()
        // [시니어 조치] JSON 데이터를 Blob으로 만들어 Multipart 파트에 추가
        formData.append('request', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
        
        // 새로 추가할 파일들이 있다면 추가
        this.newFiles.forEach(file => {
          formData.append('files', file)
        })

        if (this.isEdit) {
          await axios.put(`/api/admin/wedding/${this.$route.params.id}`, formData, { timeout: 300000 })
        } else {
          await axios.post('/api/admin/wedding', formData, { timeout: 300000 })
        }
        
        // 미리보기 URL 메모리 해제
        this.previews.forEach(url => URL.revokeObjectURL(url))
        this.$router.push({ name: 'adminWedding' })
      } catch (e) {
        alert(e.response?.data?.message || '저장에 실패했습니다.')
      } finally {
        this.saving = false
      }
    },
    // [시니어 조치] 파일 선택 시 즉시 업로드하지 않고 메모리에 저장 및 미리보기 생성
    async uploadPhotos(e) {
      const files = Array.from(e.target.files)
      if (!files.length) return
      
      for (const file of files) {
        try {
          const compressedFile = await this.compressImage(file)
          this.newFiles.push(compressedFile)
          this.previews.push(URL.createObjectURL(compressedFile))
        } catch (err) {
          // [시니어 조치] 전처리 실패 시에도 중단하지 않고 원본 업로드 허용
          console.warn(`[이미지 전처리 건너뜀] ${file.name}:`, err.message)
          this.newFiles.push(file)
          this.previews.push(URL.createObjectURL(file))
        }
      }
      e.target.value = ''
    },

    // [시니어 조치] 브라우저 기반 이미지 압축 및 RGB 변환 (Canvas API + ImageBitmap)
    async compressImage(file) {
      let bitmap
      try {
        bitmap = await createImageBitmap(file)
      } catch (err) {
        // HEIC/HEIF 시그니처 감지 (ftypheic, ftypmif1 등)
        const buffer = await file.slice(0, 12).arrayBuffer()
        const header = Array.from(new Uint8Array(buffer)).map(b => b.toString(16).padStart(2, '0')).join(' ')
        
        if (header.includes('66 74 79 70 68 65 69 63') || 
            header.includes('66 74 79 70 6d 69 66 31') || 
            header.includes('00 00 00 1c 66 74 79 70 68 65 69 63')) {
          alert(`[지원되지 않는 형식] '${file.name}'은 아이폰 고효율 이미지(HEIC) 포맷입니다.\n웹 브라우저에서는 보이지 않으므로 반드시 JPG로 변환 후 올려주세요!`)
          throw new Error('HEIC 포맷 미지원')
        }
        throw new Error(`이미지를 읽을 수 없습니다.`)
      }

      return new Promise((resolve, reject) => {
        try {
          const canvas = document.createElement('canvas')
          let width = bitmap.width
          let height = bitmap.height
          const maxSide = 1920

          // 비율 유지하며 리사이징 계산
          if (width > height) {
            if (width > maxSide) {
              height *= maxSide / width
              width = maxSide
            }
          } else {
            if (height > maxSide) {
              width *= maxSide / height
              height = maxSide
            }
          }

          canvas.width = width
          canvas.height = height
          const ctx = canvas.getContext('2d')
          
          // 흰색 배경 채우기 (RGB 강제 변환)
          ctx.fillStyle = '#FFFFFF'
          ctx.fillRect(0, 0, width, height)
          
          // Bitmap을 Canvas에 그리기
          ctx.drawImage(bitmap, 0, 0, width, height)
          
          // 사용이 끝난 Bitmap 즉시 메모리 해제
          bitmap.close()

          canvas.toBlob((blob) => {
            if (!blob) {
              reject(new Error('Canvas to Blob conversion failed'))
              return
            }
            // 원본 파일명을 유지한 새 파일 객체 생성 (.jpg로 통일)
            const newFile = new File([blob], file.name.replace(/\.[^/.]+$/, "") + ".jpg", {
              type: 'image/jpeg',
              lastModified: Date.now()
            })
            resolve(newFile)
          }, 'image/jpeg', 0.85)
        } catch (e) {
          if (bitmap) bitmap.close()
          reject(e)
        }
      })
    },

    removeNewFile(idx) {
      URL.revokeObjectURL(this.previews[idx])
      this.newFiles.splice(idx, 1)
      this.previews.splice(idx, 1)
    },
    dragStart(idx) {
      this.dragIdx = idx
    },
    async drop(toIdx) {
      if (this.dragIdx === null || this.dragIdx === toIdx) return
      const moved = this.currentPhotos.splice(this.dragIdx, 1)[0]
      this.currentPhotos.splice(toIdx, 0, moved)
      this.dragIdx = null
      this.dragOverIdx = null
      if (this.isEdit) {
        try {
          await axios.put(
            `/api/admin/wedding/${this.$route.params.id}/photos/order`,
            this.currentPhotos.map(p => p.id)
          )
        } catch {
          alert('순서 저장에 실패했습니다.')
        }
      }
    },
    async uploadMusic(e) {
      const file = e.target.files[0]
      if (!file) return
      this.musicUploading = true
      try {
        const formData = new FormData()
        formData.append('file', file)
        const res = await axios.post(`/api/admin/wedding/${this.$route.params.id}/music`, formData)
        this.form.musicUrl = res.data.data
      } catch {
        alert('음악 업로드에 실패했습니다.')
      } finally {
        this.musicUploading = false
        e.target.value = ''
      }
    },
    deletePhoto(photo) {
      if (!confirm('이 사진을 삭제하시겠습니까? (저장 시 최종 반영됩니다)')) return
      this.deletedPhotoIds.push(photo.id)
      this.currentPhotos = this.currentPhotos.filter(p => p.id !== photo.id)
    },
  },
}
</script>

<style scoped>
.form-admin-container { padding: 0 8px; max-width: 800px; }

.admin-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.admin-header h2 { margin: 0; font-size: 20px; }
.btn-back { background: none; border: 1px solid #e0d0d0; padding: 8px 14px; border-radius: 8px; cursor: pointer; font-size: 13px; color: #7a6a60; }

.form-section { background: #fff; border: 1px solid #f0e0e0; border-radius: 12px; padding: 24px; margin-bottom: 16px; }
.section-title { margin: 0 0 20px; font-size: 15px; color: #7a6a60; font-weight: 600; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-field.full { grid-column: 1 / -1; }
.form-field label { font-size: 13px; color: #888; }
.form-input { padding: 10px 14px; border: 1px solid #e8e0dc; border-radius: 8px; font-size: 14px; outline: none; }
.form-input:focus { border-color: #c8a4a4; }
.form-textarea { padding: 10px 14px; border: 1px solid #e8e0dc; border-radius: 8px; font-size: 14px; outline: none; resize: vertical; font-family: inherit; }
.form-textarea:focus { border-color: #c8a4a4; }
.field-hint { font-size: 12px; color: #c8a4a4; }
.checkbox-label { display: flex; align-items: center; gap: 8px; font-size: 14px; cursor: pointer; }
.bgm-notice { background: #fffaf5; border: 1px solid #f0e0c8; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px; font-size: 13px; color: #7a6a50; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.bgm-link { color: #c8844a; font-weight: 600; text-decoration: none; }
.bgm-link:hover { text-decoration: underline; }
.bgm-hint { color: #aaa; font-size: 12px; }
.label-sub { font-size: 11px; color: #bbb; font-weight: 400; }
.music-upload-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }

.account-block { border: 1px solid #f0e0e0; border-radius: 10px; padding: 16px; margin-bottom: 12px; }
.account-block-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.form-select-sm { padding: 6px 12px; border: 1px solid #e0d0d0; border-radius: 6px; font-size: 13px; }
.btn-remove { background: none; border: none; color: #c05050; font-size: 13px; cursor: pointer; }
.btn-add-account { background: #fdf0f0; color: #c8a4a4; border: 1px dashed #e0c0c0; padding: 10px 20px; border-radius: 8px; cursor: pointer; font-size: 14px; width: 100%; }

.photo-grid { display: flex; flex-direction: row; flex-wrap: wrap; gap: 12px; margin-bottom: 16px; }
.photo-item { position: relative; flex-shrink: 0; cursor: grab; user-select: none; }
.photo-item:active { cursor: grabbing; }
.photo-item.drag-over { outline: 2px dashed #c8a4a4; border-radius: 10px; opacity: 0.7; }
.drag-handle { position: absolute; top: 4px; left: 4px; color: #fff; font-size: 14px; z-index: 2; text-shadow: 0 1px 3px rgba(0,0,0,0.5); pointer-events: none; }
.new-preview { border: 2px solid #c8a4a4; border-radius: 10px; padding: 2px; cursor: default; }
.new-badge { position: absolute; bottom: 4px; left: 4px; background: #c8a4a4; color: #fff; font-size: 9px; padding: 2px 4px; border-radius: 4px; font-weight: bold; }
.thumb-img { width: 100px; height: 100px; object-fit: cover; border-radius: 8px; display: block; }
.btn-delete-photo { position: absolute; top: -6px; right: -6px; background: #666; color: #fff; border: 2px solid #fff; border-radius: 50%; width: 24px; height: 24px; font-size: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; z-index: 2; }
.btn-delete-photo:hover { background: #e87f7f; }
.upload-area { display: flex; align-items: center; gap: 12px; }
.file-input { display: none; }
.btn-upload { background: #f5f0ec; color: #7a6a60; border: 1px solid #e0d0d0; padding: 10px 18px; border-radius: 8px; cursor: pointer; font-size: 14px; }
.upload-hint { font-size: 12px; color: #aaa; }

.form-actions { display: flex; justify-content: flex-end; gap: 12px; padding: 8px 0 24px; }
.btn-cancel { padding: 12px 24px; border: 1px solid #e0d0d0; border-radius: 8px; background: #fff; font-size: 15px; cursor: pointer; color: #7a6a60; }
.btn-save { padding: 12px 32px; background: #c8a4a4; color: #fff; border: none; border-radius: 8px; font-size: 15px; cursor: pointer; }
.btn-save:hover:not(:disabled) { background: #b08080; }
.btn-save:disabled { background: #d4c4c4; cursor: default; }

@media (max-width: 600px) {
  .form-grid { grid-template-columns: 1fr; }
  .form-field.full { grid-column: 1; }
}
</style>
