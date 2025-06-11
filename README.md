# 📘 AuDAR: Adaptive Partial Offloading 기반 안전 시스템

---


## 🔰 프로젝트 개요
![Audar_logo](https://github.com/user-attachments/assets/e3136c6f-9d60-4f35-858d-0d8f97086219)
- AuDAR는 실시간  안전 경고 시스템입니다.

이 시스템은 **ESP32 기반 mmWave 레이더**와 **Android 스마트폰의 음향 감지 기능**을 융합하여, **게임이론 기반 Adaptive Partial Offloading** 전략으로 위험도를 판단하고, 사용자에게 실시간으로 경고합니다.

> 이 프로젝트는 단순한 구현을 넘어, 논문 실험, 스마트폰-센서 통신, 클라우드 기반 연산 구조, WebSocket 실시간 동기화, AI 추론 모델 연동까지 포함된 종합 시스템입니다.
> 

---

## 📁 디렉토리 구조

```

AUDAR/
├── android/                # Android 클라이언트 앱 (Audio 탐지)
├── server/                 # Spring Boot 기반 백엔드 (WebSocket + 오프로드 판단)
├── esp32-simulator/        # ESP32 mmWave 센서 데이터 WebSocket 송신 시뮬레이터
└── ai-server/ (Google Drive)
      ├── case2-server/     # Audio offloading (Android)
      ├── case3-server/     # Radar offloading (ESP32)
      └── case4-server/     # Full offloading (Audio + Radar)

```

> 📦 AI 서버는 용량 문제로 Google Drive에 업로드되어 있습니다:
> 
> 
> 🔗 https://drive.google.com/drive/folders/1jwIPet7s06YkqoukV8epfIfaURe3KroB?usp=drive_link
> 

---

## 🧠 시스템 구성 및 아키텍처

### 🌐 전체 아키텍처

1. **ESP32**: mmWave 센서 기반 거리/속도 측정 → WebSocket으로 서버 송신
2. **Android**: 실시간 AudioRecord로 차량소리 감지 (TFLite 모델 사용)
3. **Spring Boot 서버**:
    - WebSocket으로 센서 수신
    - SlidingWindow 기반 timestamp 병합
    - 위험도 연산 (Rule/AI)
    - 게임이론 기반 오프로드 결정
4. **FastAPI AI 서버** (Case2/3/4):
    - Android/ESP32 데이터 받아 위험도 추론
    - TFLite 모델 사용

---

## 🧭 핵심 개념 및 원리

### 🎯 Adaptive Partial Offloading

- Android/ESP32가 주기적으로 자신의 **latency, CPU, 배터리** 등을 서버에 전송
- 서버는 **유틸리티 함수 기반 게임이론 알고리즘**으로 각 디바이스가 **Local 연산할지, Offload할지** 결정
- 총 4가지 경우(Case1~4)로 분기

```java

uLocal = (1 - cpuLoad) * 0.6 + batteryLevel * 0.4
uOffload = (1 - latency / 1000) * 0.7

```

### 🔄 Sliding Window 동기화

- 서버는 timestamp 기준으로 Android/ESP32 데이터를 **±400ms 윈도우**로 병합
- 병합 후 케이스별 Service 로직에 따라 Danger 판단

---

## 🚀 실행 방법

### ✅ 1. Spring Boot 서버 실행

### 🔧 환경설정

- `application.yml` 또는 `application-prod.yml`에 다음 정보 설정

```yaml

spring:
  datasource:
    url: jdbc:mysql://your-rds-endpoint:3306/audar
    username: root
    password: yourpassword

cloud:
  aws:
    credentials:
      instanceProfile: true  # IAM Role 사용 시
    s3:
      bucket: audar-bucket
    region:
      static: ap-northeast-2

```

### 🐳 Docker로 실행

```bash

cd server
./gradlew build
docker build -t audar-server .
docker run -d -p 3000:3000 --name audar-server audar-server

```

- 포트: `3000`
- WebSocket 주소: `ws://<EC2-IP>:3000/data`

---

### ✅ 2. Android 앱 실행

### 📱 요구사항

- Android Studio 최신 버전
- `assets/` 폴더에 `car_detection_raw_audio_model.tflite` 추가
- WebSocket 주소는 `WebSocketManager.kt` 내에 설정

```kotlin

val SERVER_URL = "ws://<EC2-IP>:3000/data"

```

### 🔧 주요 클래스

- `ForegroundService`: 실시간 오디오 분석 및 서버 전송
- `WebSocketManager`: WebSocket 연결 관리
- `MetricSender`: CPU/배터리/latency 전송

> ⚠️ Android 12 이상에서 백그라운드 권한 주의 (ForegroundService 사용)
> 

---

### ✅ 3. ESP32 시뮬레이터 실행

```bash

cd esp32-simulator
pip install websocket-client
python esp32_simulator.py

```

- 시뮬레이터는 mmWave 센서의 거리, 속도 데이터를 생성
- WebSocket 주소: `ws://<EC2-IP>:3000/data`

📍 `esp32_offload.py`는 오프로드 결정 요청도 포함함

---

### ✅ 4. FastAPI 모델 서버 실행

```bash

cd ai-server/case2-server
uvicorn case2:app --host 0.0.0.0 --port 8002

```

- 포트 매핑:
    - Case2: `8002`
    - Case3: `8003`
    - Case4: `8004`
- 모델은 `.tflite`로 구성
- 모델 위치는 `./models/` 또는 경로 직접 지정

---

## ☁️ AWS 배포 가이드

### 🖥️ EC2 인스턴스

- Ubuntu 22.04
- 최소 스펙: `t3.medium` (2vCPU, 4GB RAM)
- 보안 그룹:
    - 3000 (Spring Boot)
    - 8002, 8003, 8004 (FastAPI)
    - 22 (SSH)
    - 80, 443 (HTTP/HTTPS)

```bash

sudo apt update
sudo apt install docker.io
sudo docker run ...

```

### 🗃️ RDS 설정

- 엔진: MySQL 8.x
- 스토리지: 20GB (gp2), 자동 스케일링 ON
- 보안 그룹: EC2 인스턴스의 IP 허용
- 테이블은 서버 실행 시 JPA로 자동 생성

### 🗂️ S3 설정

- 버킷 이름: `audar-bucket`
- 퍼블릭 접근은 제한, presigned URL 사용 가능
- IAM Role 권장 (EC2에서 `instanceProfile: true` 설정 시 키 불필요)

---

## 💡 주요 Troubleshooting

| 문제 증상 | 원인 및 해결 |
| --- | --- |
| WebSocket 연결 안됨 | EC2 보안 그룹 확인 / IP 포트 확인 |
| S3 업로드 실패 | IAM Role 권한 부족 / 버킷 이름 오타 |
| FastAPI 422 오류 | FormData 누락 (특히 audio_file, radar_detected) |
| Android 앱 꺼짐 | 권한 문제 or 모델 파일 누락 (`assets/`) |
| Sliding Window 매칭 실패 | ESP32/Android timestamp mismatch → 0.5초 이상 차이 허용 안 됨 |

---

## 📌 버전 정보

| 구성 요소 | 버전 |
| --- | --- |
| Spring Boot | 3.1.x |
| Gradle | 8.x |
| Python | 3.10 |
| FastAPI | 0.105+ |
| TFLite 모델 | TensorFlow Lite 2.11 기준 |
| Android SDK | 33+ |
| 서버 OS | Ubuntu 22.04 (EC2) |

---

## 📎 기타 참고자료

- 📁 AI Server 구글드라이브 링크:
    
    https://drive.google.com/drive/folders/1jwIPet7s06YkqoukV8epfIfaURe3KroB?usp=drive_link
    
- 📸 아키텍처 이미지:
- <img width="770" alt="image" src="https://github.com/user-attachments/assets/60c1e0f1-f7f3-44be-9186-851cfa883215" />


---

## 🙋 FAQ

**Q. Android, ESP32는 어떻게 서버에서 구분하나요?**

→ WebSocket 메시지 안에 deviceType, timestamp 필드가 존재하며, 별도 버퍼에 저장하여 SlidingWindowMatcher에서 병합합니다.

**Q. FastAPI 서버는 꼭 EC2에서 돌려야 하나요?**

→ 아니요. local에서도 가능하지만, 실험 시 latency 기준 비교를 위해 EC2 배포 권장.

**Q. WebSocket 메시지는 양방향인가요?**

→ 예. 서버는 클라이언트에 위험도(Level: LOW/MEDIUM/HIGH)를 실시간 push합니다.
