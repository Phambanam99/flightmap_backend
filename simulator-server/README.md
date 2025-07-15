# Flight & Vessel Simulator Server

Một server Node.js để mô phỏng dữ liệu của 5 chuyến bay và 5 con tàu liên tục, cung cấp API endpoints tương thích với các nguồn dữ liệu thực tế.

## Cài đặt

```bash
cd simulator-server
npm install
```

## Chạy Server

```bash
# Chạy trong development mode (với nodemon)
npm run dev

# Hoặc chạy production mode
npm start
```

Server sẽ chạy trên port 3001.

## API Endpoints

### Aircraft Data Sources
- **FlightRadar24**: `GET http://localhost:3001/api/mock/flightradar24`
- **ADS-B Exchange**: `GET http://localhost:3001/api/mock/adsbexchange`

### Vessel Data Sources
- **MarineTraffic**: `GET http://localhost:3001/api/mock/marinetraffic`
- **VesselFinder**: `GET http://localhost:3001/api/mock/vesselfinder`
- **Chinaports**: `GET http://localhost:3001/api/mock/chinaports`
- **MarineTraffic V2**: `GET http://localhost:3001/api/mock/marinetrafficv2`

### Monitoring
- **Health Check**: `GET http://localhost:3001/health`
- **API Status**: `GET http://localhost:3001/api/status`

## Dữ liệu mô phỏng

### 5 Chuyến bay:
1. **VN123** - Vietnam Airlines A320: SGN → HAN
2. **VJ456** - VietJet A321: DAD → SGN  
3. **QH789** - Bamboo Airways ATR72: HAN → PQC
4. **DL123** - Delta B777: NRT → SGN
5. **SQ999** - Singapore Airlines A350: SIN → HAN

### 5 Con tàu:
1. **EVER GIVEN VIETNAM** - Container Ship: TP.HCM → Singapore
2. **MAERSK SAIGON** - Container Ship: Hải Phòng → Hong Kong
3. **COSCO VIETNAM** - Bulk Carrier: Đà Nẵng → Shanghai
4. **HAPAG MEKONG** - Container Ship: Cần Thơ → Bangkok
5. **YANG MING VIETNAM** - Container Ship: Quy Nhon → Manila

## Đặc điểm

- ✅ Dữ liệu cập nhật liên tục mỗi 3 giây
- ✅ Chuyển động thực tế theo tuyến đường
- ✅ Thêm độ biến thiên ngẫu nhiên cho tính thực tế
- ✅ Tương thích với format API của các nguồn thực
- ✅ CORS enabled cho frontend
- ✅ Logging chi tiết mỗi request

## Cấu hình Backend

Backend sẽ được cấu hình để gọi những endpoints này thay vì API thực tế, với interval 3 giây.
