Tôi sẽ vẽ sơ đồ hoạt động của toàn bộ backend dựa trên phân tích code của bạn:
Ran tool

Ran tool

## Giải thích chi tiết sơ đồ hoạt động Backend

### 1. **External Data Sources (Nguồn dữ liệu bên ngoài)**
- **FlightRadar24, ADS-B Exchange**: API cung cấp dữ liệu máy bay
- **MarineTraffic, VesselFinder, Chinaports, MarineTraffic V2**: API cung cấp dữ liệu tàu thuyền

### 2. **Data Collection Layer (Lớp thu thập dữ liệu)**
- **MultiSourceExternalApiService**: Chạy định kỳ mỗi 30 giây (có thể cấu hình)
- Thu thập dữ liệu song song từ nhiều nguồn API khác nhau
- Có timeout và xử lý lỗi cho từng nguồn

### 3. **Data Processing Layer (Lớp xử lý dữ liệu)**
- **DataFusionService**: Gộp dữ liệu từ nhiều nguồn, loại bỏ trùng lặp
- **RealTimeDataProcessor**: Kiểm tra, chuẩn hóa và gửi dữ liệu vào Kafka
- **RawDataStorageService**: Lưu dữ liệu thô để phục vụ audit/phân tích

### 4. **Kafka Message Broker**
- Hệ thống sử dụng nhiều topic để phân loại dữ liệu:
  - `raw-*`: Dữ liệu thô từ API
  - `processed-*`: Dữ liệu đã xử lý
  - `alerts`, `realtime-positions`, `websocket-updates`: Các loại thông báo

### 5. **Kafka Consumers**
- **KafkaConsumerService**: Xử lý các message từ topic cũ
- **TrackingKafkaConsumer**: Xử lý các message từ topic mới, có error handling tốt hơn

### 6. **Business Logic Services**
- **FlightTrackingService**: Xử lý logic tracking máy bay, tạo Aircraft và Flight
- **ShipTrackingService**: Xử lý logic tracking tàu, tạo Ship và Voyage
- Cả hai đều có hàm `processNewTrackingData()` là điểm xử lý trung tâm

### 7. **Data Storage**
- **PostgreSQL/TimescaleDB**: Lưu trữ dữ liệu chính với time-series optimization
- **Redis Cache**: Cache dữ liệu hot (vị trí hiện tại, session)

### 8. **API Layer**
- **REST Controllers**: Cung cấp API cho client
- **WebSocket Service**: Gửi real-time updates cho client

### 9. **Luồng dữ liệu chính**
1. **Thu thập**: MultiSourceExternalApiService gọi các API bên ngoài
2. **Gộp dữ liệu**: DataFusionService loại bỏ trùng lặp
3. **Xử lý**: RealTimeDataProcessor chuẩn hóa và gửi vào Kafka
4. **Consume**: Kafka Consumer nhận và xử lý message
5. **Lưu trữ**: Services lưu vào DB và cache
6. **Phân phối**: WebSocket gửi updates real-time cho client

### 10. **Điểm đáng chú ý**
- **Async processing**: Nhiều xử lý bất đồng bộ để tăng hiệu năng
- **Multiple data sources**: Hỗ trợ nhiều nguồn dữ liệu để tăng độ tin cậy
- **Real-time updates**: WebSocket cho phép cập nhật real-time
- **Scalable architecture**: Kafka cho phép scale horizontal dễ dàng

Sơ đồ này thể hiện một kiến trúc microservices với event-driven architecture, phù hợp cho hệ thống tracking real-time quy mô lớn.