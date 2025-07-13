# 🔄 Multi-Source Data Fusion Architecture Guide

## 📋 Overview

Hệ thống tracking vessel/flight hỗ trợ tích hợp và hợp nhất dữ liệu từ nhiều nguồn API external khác nhau. Điều này giúp tăng độ chính xác, độ tin cậy và tính đầy đủ của dữ liệu tracking.

## 🏗️ Architecture Components

### 1. **External API Sources**

#### ✈️ Aircraft Sources (2):
- **FlightRadar24**: Dữ liệu máy bay realtime (Priority 1)
- **ADS-B Exchange**: Community-driven ADS-B data (Priority 2)

#### 🚢 Vessel Sources (4):
- **MarineTraffic**: Dữ liệu tàu thủy realtime (Priority 1)
- **VesselFinder**: Global vessel tracking (Priority 2) 
- **Chinaports**: Dữ liệu tàu thủy từ các cảng Trung Quốc (Priority 3)
- **MarineTraffic V2**: Phiên bản nâng cấp của MarineTraffic (Priority 4)

### 2. **Data Fusion Service**

Service này chịu trách nhiệm:
- **Hợp nhất dữ liệu** từ nhiều nguồn cho cùng một đối tượng
- **Khử trùng lặp** dựa trên thời gian và vị trí
- **Tính toán chất lượng dữ liệu** dựa trên độ tin cậy của nguồn
- **Ưu tiên nguồn dữ liệu** theo cấu hình

### 3. **Multi-Source External API Service**

Service này quản lý:
- Thu thập dữ liệu song song từ tất cả các nguồn
- Điều phối việc hợp nhất dữ liệu
- Xử lý lỗi và failover giữa các nguồn

## 🔧 Configuration

### Application Properties
```properties
# Data fusion configuration
data.fusion.enabled=true
data.fusion.deduplication.enabled=true
data.fusion.deduplication.time-window=30000  # 30 seconds
data.fusion.quality.threshold=0.5

# Source priorities (lower number = higher priority)
data.fusion.priority.flightradar24=1
data.fusion.priority.adsbexchange=2
data.fusion.priority.marinetraffic=1
data.fusion.priority.vesselfinder=2
data.fusion.priority.chinaports=3
data.fusion.priority.marinetrafficv2=4
```

## 🔄 Data Flow

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  FlightRadar24  │  │  ADS-B Exchange │  │  MarineTraffic  │  │   Chinaports    │  │MarineTraffic V2 │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │                    │                    │
         └──────────┬─────────┘                    └──────────┬─────────┴────────────────────┘
                    │                                         │
             ┌──────▼──────┐                           ┌──────▼──────┐
             │  Aircraft   │                           │   Vessel    │
             │  Collector  │                           │  Collector  │
             └──────┬──────┘                           └──────┬──────┘
                    │                                         │
             ┌──────▼──────────────────────────────────────────▼──────┐
             │              Data Fusion Service                       │
             │  • Deduplication across all sources                    │
             │  • Priority-based merging (1-4)                        │
             │  • Quality scoring with source reliability              │
             │  • Geographic coverage optimization                     │
             └──────────────────────┬───────────────────────────────────┘
                                    │
                             ┌──────▼──────┐
                             │   Kafka     │
                             │  Producer   │
                             └──────┬──────┘
                                    │
                             ┌──────▼──────┐
                             │  Database   │
                             │   & Redis   │
                             └─────────────┘
```

## 🎯 Fusion Algorithm

### 1. **Deduplication**
- Kiểm tra cache để phát hiện dữ liệu trùng lặp
- So sánh vị trí: nếu < 100m và trong 30s → duplicate
- Skip duplicate để tối ưu performance

### 2. **Priority-based Merging**
- Sắp xếp nguồn theo priority (1 = cao nhất)
- Sử dụng nguồn priority cao nhất làm base
- Bổ sung fields còn thiếu từ nguồn priority thấp hơn

### 3. **Position Averaging**
- Nếu có nhiều nguồn cung cấp vị trí gần đây (< 30s)
- Tính trung bình vị trí để tăng độ chính xác

### 4. **Quality Scoring**
```
Quality = Base Quality + Agreement Bonus - Age Penalty

- Base Quality: Từ nguồn dữ liệu gốc
- Agreement Bonus: +5% cho mỗi nguồn đồng thuận (max 20%)
- Age Penalty: -20% nếu dữ liệu > 60s (aircraft) hoặc > 120s (vessel)
```

## 📡 API Endpoints

### Check Status
```bash
GET /api/data-sources/status
```

Response:
```json
{
  "currentSources": {
    "flightRadar24": {
      "enabled": true,
      "available": true
    },
    "marineTraffic": {
      "enabled": true,
      "available": true
    }
  },
  "newSources": {
    "adsbexchange": {
      "enabled": true,
      "available": true,
      "coverage": "Global ADS-B data with focus on US/Europe",
      "dataSource": "Community-driven ADS-B receivers",
      "priority": 2
    },
    "vesselfinder": {
      "enabled": true,
      "available": true,
      "coverage": "Global vessel tracking with enhanced commercial vessel data",
      "specialization": "Focus on commercial and cargo vessels",
      "priority": 2
    },
    "chinaports": {
      "enabled": true,
      "available": true,
      "coverage": "China Sea and major Chinese ports",
      "priority": 3
    },
    "marinetrafficv2": {
      "enabled": true,
      "available": true,
      "version": "v2",
      "improvements": "Enhanced data quality and additional fields",
      "priority": 4
    }
  },
  "dataFusion": {
    "enabled": true,
    "deduplicationEnabled": true,
    "activeSources": 6,
    "aircraftSources": 2,
    "vesselSources": 4
  }
}
```

### Manual Trigger Collection
```bash
# Collect all data
POST /api/data-sources/collect/all

# Collect aircraft only
POST /api/data-sources/collect/aircraft

# Collect vessel only
POST /api/data-sources/collect/vessel
```

### Get Fusion Configuration
```bash
GET /api/data-sources/fusion/config
```

## 🚀 Implementation Guide for New APIs

### 1. Add Configuration
```properties
external.api.newapi.enabled=true
external.api.newapi.base-url=https://api.newapi.com
external.api.newapi.api-key=your-api-key
external.api.newapi.poll-interval=30000
data.fusion.priority.newapi=3
```

### 2. Create API Service
```java
@Service
public class NewApiService {
    @Async
    public CompletableFuture<List<AircraftTrackingRequest>> fetchAircraftData() {
        // Implementation
    }
}
```

### 3. Register in MultiSourceExternalApiService
```java
// Add to collectAllAircraftData()
futures.put("newapi", newApiService.fetchAircraftData());
```

## 📊 Benefits

1. **Increased Reliability**: Nếu một API fail, vẫn có dữ liệu từ nguồn khác
2. **Better Coverage**: Mỗi API có thể cover các vùng khác nhau
3. **Enhanced Accuracy**: Trung bình hóa vị trí từ nhiều nguồn
4. **Data Completeness**: Bổ sung thông tin còn thiếu từ các nguồn khác
5. **Quality Control**: Chỉ xử lý dữ liệu đạt ngưỡng chất lượng

## 🔍 Monitoring

### Logs
```
INFO: Collected 150 aircraft from flightradar24
INFO: Collected 120 aircraft from adsbexchange
DEBUG: Fused aircraft data for ABC123 from 2 sources with quality 0.85
INFO: Processed 145 merged aircraft records from multiple sources
```

### Metrics to Monitor
- Number of records from each source
- Fusion rate (merged vs total)
- Deduplication rate
- Average data quality score
- API response times

## ⚠️ Important Notes

1. **API Rate Limits**: Cẩn thận với rate limits của mỗi API
2. **Cost Management**: Nhiều API tính phí theo request
3. **Data Rights**: Đảm bảo tuân thủ ToS của mỗi API
4. **Performance**: Fusion process thêm latency (~10-50ms)
5. **Storage**: Lưu source metadata để trace data origin

## 🔮 Future Enhancements

1. **Machine Learning**: Dự đoán nguồn nào đáng tin cậy nhất cho từng vùng
2. **Adaptive Priority**: Tự động điều chỉnh priority dựa trên performance
3. **Conflict Resolution**: Thuật toán phức tạp hơn cho data conflicts
4. **Real-time Quality Metrics**: Dashboard theo dõi chất lượng từng nguồn
5. **Smart Caching**: Cache fusion results để giảm processing 