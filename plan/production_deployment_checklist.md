# 🚀 habiDue 실속형(Economy) AWS 배포 체크리스트 (v1.1)

본 문서는 예산 3~4만 원 내외의 **가성비 실속형 아키텍처(t4g.small + Docker 통합형)** 기반의 배포 및 운영 가이드입니다.

---

## 🏗️ 1단계: AWS 인프라 구축 (Cost-Efficient Infra)
- [ ] **EC2 인스턴스 생성**: `t4g.small` (Graviton2, 2vCPU, 2GB RAM) - Amazon Linux 2023 권장.
- [ ] **보안 그룹(Security Group) 설정**: 
  - 80(HTTP), 443(HTTPS), 22(SSH) 허용.
  - 3306(MySQL), 6379(Redis)는 외부 노출 차단 (내부 Docker 네트워크 활용).
- [ ] **S3 버킷 생성**: 이미지 업로드 및 **DB 백업 덤프 파일 저장용**.
- [ ] **EBS 스토리지**: gp3 20~30GB (기본 용량으로 충분).

---

## ⚙️ 2단계: 서버 환경 최적화 (Memory & Docker)
- [ ] **⚠️ [필수] Swap Memory 설정 (4GB)**:
  - 2GB RAM에 Java + MySQL + Redis를 모두 올리기 위해 반드시 4GB 이상의 스왑 메모리 설정 필요.
  - `dd`, `mkswap`, `swapon` 명령어를 통한 가상 메모리 확보.
- [ ] **Docker & Docker Compose 설치**: 컨테이너 기반의 통합 관리를 위한 환경 구축.
- [ ] **Nginx 설치**: Reverse Proxy 설정을 통해 SSL 적용 및 프론트엔드 정적 파일 서빙.

---

## 💾 3단계: 자가 관리형 DB & 캐시 (Self-Managed)
- [ ] **Docker Compose 구성**:
  - `mysql:8.0` 컨테이너 (데이터 보존을 위한 Volume 마운트 필수).
  - `redis:alpine` 컨테이너.
  - `habidue-app` (Spring Boot) 컨테이너.
- [ ] **⚠️ [필수] DB 자동 백업 시스템 구축**:
  - [ ] `mysqldump`를 이용한 일 단위 백업 스크립트 작성.
  - [ ] AWS CLI를 활용하여 백업 파일을 S3 버킷으로 자동 전송.
  - [ ] `crontab`에 등록하여 매일 새벽 자동 실행 (데이터 유실 방지).

---

## 💻 4단계: 백엔드/프론트엔드 배포 (Deployment)
- [ ] **운영 프로파일 설정 (`application-prod.yml`)**:
  - [ ] DB 주소를 `localhost`가 아닌 Docker 서비스명(예: `db`)으로 설정.
  - [ ] S3 업로드 로직 활성화 및 IAM Access Key 설정.
- [ ] **프론트엔드 빌드**: `.env.production` API 주소 설정 후 `dist` 폴더 생성 및 Nginx 연동.
- [ ] **SSL 인증서 적용**: `certbot`을 활용한 무료 Let's Encrypt 인증서 발급 및 자동 갱신.

---

## 📈 5단계: 모니터링 및 유지보수 (Maintenance)
- [ ] **메모리 모니터링**: `free -m` 또는 `htop`으로 자원 사용량 상시 체크.
- [ ] **로그 로테이션**: 컨테이너 로그가 디스크를 가득 채우지 않도록 Docker 로그 제한 설정.
- [ ] **보안 업데이트**: AWS Inspector 또는 수동 업데이트를 통한 OS 보안 패치.

---

## 💡 미래를 위한 "미리 준비할 것" (Future-Proofing)
- [ ] **환경 변수 활용 (No Hardcoding)**: DB 접속 정보(Host, ID, PW)를 코드에 박지 말고 `.env` 파일이나 운영 프로파일의 환경 변수로 관리하세요. 나중에 RDS 등으로 이전할 때 주소만 슥 바꾸면 됩니다.
- [ ] **S3 우선 적용 (Data Migration 방지)**: 파일 업로드만큼은 처음부터 S3를 쓰세요. 로컬 서버(EC2)에 파일을 쌓기 시작하면, 나중에 수십 기가(GB)의 데이터를 옮기는 작업이 가장 큰 고비가 됩니다.

---

### 🛡️ 시니어의 핵심 조치 & 제언
1. **Swap Memory는 생명줄**: 2GB RAM에서 스왑 없이 자바와 DB를 같이 돌리면 예기치 않게 프로세스가 Kill 될 수 있습니다. 4GB 스왑 설정은 선택이 아닌 필수입니다.
2. **S3 백업은 최후의 보루**: RDS를 쓰지 않으므로 EC2 장애 시 DB도 날아갑니다. S3로 매일 덤프 파일을 던지는 백업 스크립트가 정상 작동하는지 반드시 첫날 확인하세요.
3. **IAM Role 활용**: EC2에 S3 접근 권한을 줄 때, AccessKey를 코드에 직접 넣지 말고 **EC2 IAM Role**을 부여하여 보안 사고를 원천 차단하세요.
