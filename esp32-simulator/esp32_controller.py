import json
import random
import time
import threading
import websocket

# 서버 주소
WS_SERVER = "ws://3.34.129.82:3000/data"
DEVICE_TYPE = "ESP32"
DEVICE_ID = "esp32-001"

# 상태 및 설정
current_case = 1
METRIC_INTERVAL = 3  # 초
SENSOR_INTERVAL = 2  # 초

# 시뮬레이션 상태 변수
approaching = True
distance_m = random.uniform(1.0, 20.0)
velocity_kmh = 0.0

# radar_detected 연산 함수 (복잡한 계산으로 로컬 처리 비용 시뮬레이션)
def calculate_radar_detected(velocity, distance):
    time.sleep(0.2)  # 로컬 연산 delay
    risk = (1 / (distance + 0.1)) + (velocity ** 2) / 10000
    normalized = min(1.0, max(0.0, 1.0 - (distance / 30) + risk / 5))
    return round(normalized, 4)

# 차량 접근/이탈 시나리오 생성
def generate_scenario():
    global approaching, distance_m, velocity_kmh

    if random.random() < 0.15:
        approaching = not approaching

    if approaching:
        velocity_kmh = random.uniform(5, 60)
        distance_m = max(0.5, distance_m - (velocity_kmh / 3600.0) * 0.5)
    else:
        velocity_kmh = -random.uniform(5, 40)
        distance_m = min(50.0, distance_m + (abs(velocity_kmh) / 3600.0) * 0.5)

    distance_m += random.uniform(-0.2, 0.2)
    return round(distance_m, 3), round(velocity_kmh, 2)

# WebSocket 수신 핸들러
def on_message(ws, message):
    global current_case
    try:
        data = json.loads(message)
        if "case" in data:
            current_case = int(data["case"])
            print(f"📥 서버로부터 case 수신됨 → CASE {current_case}")
    except Exception as e:
        print("❌ 메시지 처리 실패:", e)

# WebSocket 연결 성공 시
def on_open(ws):
    print("✅ WebSocket 연결됨")
    ws.send(json.dumps({"deviceType": DEVICE_TYPE}))
    threading.Thread(target=send_sensor_data, args=(ws,), daemon=True).start()
    threading.Thread(target=send_metrics, args=(ws,), daemon=True).start()

# WebSocket 에러/종료 처리
def on_error(ws, error): print("❌ WebSocket 오류:", error)
def on_close(ws, code, msg): print(f"🔌 WebSocket 종료: {code} {msg}")

# 센서 데이터 전송
def send_sensor_data(ws):
    global current_case
    while True:
        timestamp = int(time.time() * 1000)
        dist, vel = generate_scenario()
        radar = calculate_radar_detected(vel, dist)

        if current_case in [1, 2]:
            payload = {
                "timestamp": timestamp,
                "radar_detected": radar
            }
        elif current_case in [3, 4]:
            payload = {
                "timestamp": timestamp,
                "velocity": vel,
                "distance": dist
            }
        else:
            print("⚠️ 잘못된 CASE 번호:", current_case)
            time.sleep(SENSOR_INTERVAL)
            continue

        try:
            ws.send(json.dumps(payload))
            print(f"📡 센서 전송 (CASE {current_case}): {payload}")
        except Exception as e:
            print("❌ 센서 전송 실패:", e)

        time.sleep(SENSOR_INTERVAL)

# metric 전송
def send_metrics(ws):
    while True:
        dist, vel = generate_scenario()
        metric = {
            "timestamp": int(time.time() * 1000),
            "deviceId": DEVICE_ID,
            "deviceType": "ESP32",
            "latency": round(random.uniform(30, 120), 2),
            "cpuLoad": round(random.uniform(0.2, 0.9), 2),
            "batteryLevel": round(random.uniform(0.3, 1.0), 2)
        }

        try:
            ws.send(json.dumps(metric))
            print(f"📤 metric 전송: {metric}")
        except Exception as e:
            print("❌ metric 전송 실패:", e)

        time.sleep(METRIC_INTERVAL)

# WebSocket 실행
def run_simulator():
    ws = websocket.WebSocketApp(
        WS_SERVER,
        on_open=on_open,
        on_message=on_message,
        on_error=on_error,
        on_close=on_close
    )
    ws.run_forever()

if __name__ == "__main__":
    run_simulator()
