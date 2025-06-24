# Raw Data Storage Strategy Guide

## 📋 **Tổng quan**

Hệ thống flight/vessel tracking đã được nâng cấp với **chiến lược lưu trữ raw data toàn diện** để giải quyết vấn đề audit, traceability và data lineage từ các external APIs.

## ❌ **Vấn đề trước đây**

### **Flow cũ:**
```
External APIs → DataFusion → Database (Chỉ lưu data đã merge)
```

### **Hậu quả:**
- ❌ Không có audit trail cho từng source
- ❌ Không trace được data đến từ API nào
- ❌ Không thể debug/analyze quality của từng API riêng
- ❌ Không thể replay data từ specific source
- ❌ Khó troubleshoot khi một source có vấn đề

## ✅ **Giải pháp mới: Dual Storage Strategy**

### **Flow mới:**
```
External APIs → Raw Data Storage → DataFusion → Processed Data Storage
                      ↓
                 Audit & Analytics
```

## 🏗️ **Kiến trúc hệ thống**

### **1. Raw Data Models**

#### **RawAircraftData**
```java
@Entity
@Table(name = "raw_aircraft_data")
public class RawAircraftData {
    // Source tracking
    private String dataSource;      // flightradar24, adsbexchange
    private Integer sourcePriority;
    private Long apiResponseTime;
    
    // Aircraft data
    private String hexident;
    private String callsign;
    private Double latitude;
    private Double longitude;
    private Integer altitude;
    
    // Quality & metadata
    private Double dataQuality;
    private LocalDateTime originalTimestamp;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    private Long fusionResultId;
    
    // Raw data preservation
    private String rawJson;
    private String apiEndpoint;
    private Boolean isValid;
    private String validationErrors;
    
    // Retention policy
    private final Integer retentionDays = 30;
}
```

#### **RawVesselData**
```java
@Entity
@Table(name = "raw_vessel_data")
public class RawVesselData {
    // Source tracking
    private String dataSource;      // marinetraffic, vesselfinder, chinaports, marinetrafficv2
    private Integer sourcePriority;
    private Long apiResponseTime;
    
    // Vessel data
    private String mmsi;
    private String imo;
    private String vesselName;
    private Double latitude;
    private Double longitude;
    
    // Extended vessel info
    private String destination;
    private String cargoType;
    private Boolean dangerousCargo;
    private Boolean securityAlert;
    
    // Similar metadata fields as aircraft...
}
```

### **2. Storage Flow**

```java
@Service
public class MultiSourceExternalApiService {
    
    @Scheduled(fixedRate = 30000)
    public void collectAndProcessMultiSourceData() {
        // 1. Collect from all sources
        Map<String, CompletableFuture<List<Data>>> futures = collectFromAllSources();
        
        // 2. Store raw data từng source riêng biệt
        futures.forEach((source, future) -> {
            List<Data> data = future.join();
            long responseTime = measureResponseTime();
            
            // Store raw data for audit
            rawDataStorageService.storeRawData(source, data, apiEndpoint, responseTime);
        });
        
        // 3. Perform data fusion
        List<MergedData> fusedData = dataFusionService.merge(allSourcesData);
        
        // 4. Store processed data
        realTimeDataProcessor.process(fusedData);
    }
}
```

## 📊 **Lợi ích của chiến lược mới**

### **1. Complete Audit Trail**
- ✅ Trace được mỗi record đến từ source nào
- ✅ Track API response time từng source
- ✅ Lưu raw JSON để debug
- ✅ Link raw data với fusion result

### **2. Data Quality Analysis**
```sql
-- Quality by source
SELECT data_source, AVG(data_quality), COUNT(*) 
FROM raw_aircraft_data 
WHERE received_at >= NOW() - INTERVAL '24 hours'
GROUP BY data_source;

-- Response time analysis
SELECT data_source, AVG(api_response_time_ms), MAX(api_response_time_ms)
FROM raw_vessel_data 
GROUP BY data_source;
```

### **3. Source Health Monitoring**
```java
// Monitor source availability
@GetMapping("/api/raw-data/health")
public Map<String, Object> getSourceHealth() {
    return Map.of(
        "aircraftSources", rawAircraftRepo.getLastDataReceiptBySource(),
        "vesselSources", rawVesselRepo.getLastDataReceiptBySource()
    );
}
```

### **4. Troubleshooting & Debug**
```java
// Find duplicates across sources
List<RawAircraftData> findDuplicatesForAircraft(String hexident);

// Analyze data coverage by geographic bounds
List<Object[]> getCoverageByGeographicBounds(minLat, maxLat, minLon, maxLon);

// Get raw data for specific time period
List<RawVesselData> findByDataSourceAndReceivedAtBetween(source, start, end);
```

## 🔧 **Configuration**

### **application.properties**
```properties
# Raw Data Storage Configuration
raw.data.storage.enabled=true
raw.data.compression.enabled=true
raw.data.retention.days=30

# Data Fusion Configuration (unchanged)
data.fusion.enabled=true
data.fusion.deduplication.enabled=true
data.fusion.priority.flightradar24=1
data.fusion.priority.adsbexchange=2
data.fusion.priority.marinetraffic=1
data.fusion.priority.vesselfinder=2
data.fusion.priority.chinaports=3
data.fusion.priority.marinetrafficv2=4
```

## 📡 **API Endpoints**

### **Raw Data Management**
```bash
# Get statistics
GET /api/raw-data/statistics?start=2024-01-01T00:00:00&end=2024-01-02T00:00:00

# Get raw data by source
GET /api/raw-data/aircraft/flightradar24?page=0&size=20
GET /api/raw-data/vessels/marinetraffic?page=0&size=20

# Quality analysis
GET /api/raw-data/quality-analysis?start=2024-01-01T00:00:00

# Source health check
GET /api/raw-data/health

# Find duplicates
GET /api/raw-data/aircraft/A12345/duplicates?hours=24
GET /api/raw-data/vessels/123456789/duplicates?hours=24

# Manual cleanup
POST /api/raw-data/cleanup
```

## 🗄️ **Database Schema**

### **Indexes for Performance**
```sql
-- Raw Aircraft Data
CREATE INDEX idx_raw_aircraft_source_time ON raw_aircraft_data(data_source, received_at);
CREATE INDEX idx_raw_aircraft_hexident ON raw_aircraft_data(hexident);
CREATE INDEX idx_raw_aircraft_received_at ON raw_aircraft_data(received_at);

-- Raw Vessel Data  
CREATE INDEX idx_raw_vessel_source_time ON raw_vessel_data(data_source, received_at);
CREATE INDEX idx_raw_vessel_mmsi ON raw_vessel_data(mmsi);
CREATE INDEX idx_raw_vessel_received_at ON raw_vessel_data(received_at);
```

### **Data Retention**
- **Raw data**: Tự động xóa sau 30 ngày (configurable)
- **Processed data**: Theo policy hiện tại
- **Cleanup schedule**: Daily at 2 AM

## 📈 **Monitoring & Analytics**

### **Real-time Metrics**
```java
// Source performance metrics
Map<String, Object> metrics = rawDataStorageService.getRawDataStatistics(start, end);

// Contains:
// - Records count by source
// - Data quality scores by source  
// - API response times by source
// - Coverage analysis by geographic bounds
// - Health status of each source
```

## 🚀 **Deployment Considerations**

### **Storage Requirements**
- **Raw data**: ~2-3x storage increase (30 days retention)
- **Indexes**: Additional 20-30% for query performance
- **JSON storage**: Compressed to reduce size

### **Performance Impact**
- **Minimal**: Raw storage is async, non-blocking
- **Benefits**: Better fusion quality, faster troubleshooting
- **Trade-off**: Storage cost vs operational visibility

## 🛠️ **Operations Guide**

### **Daily Tasks**
```bash
# Check source health
curl GET /api/raw-data/health

# Monitor data quality
curl GET /api/raw-data/quality-analysis
```

### **Troubleshooting**
```bash
# Find source issues
curl GET /api/raw-data/statistics

# Investigate specific aircraft/vessel
curl GET /api/raw-data/aircraft/A12345/duplicates
curl GET /api/raw-data/vessels/123456789/duplicates

# Manual cleanup if needed
curl POST /api/raw-data/cleanup
```

## 🎯 **Best Practices**

### **1. Regular Monitoring**
- Check source health daily
- Monitor data quality trends weekly
- Review API response times for SLA compliance

### **2. Retention Policy**
- Adjust retention days based on audit requirements
- Consider archiving critical data before cleanup
- Monitor storage growth trends

### **3. Performance Optimization**
- Use pagination for large queries
- Add indexes for new query patterns
- Monitor database performance impact

## 📋 **Summary**

Hệ thống mới cung cấp **complete data lineage** và **comprehensive audit trail** cho tất cả external API data:

| **Before** | **After** |
|------------|-----------|
| ❌ No audit trail | ✅ Complete raw data storage |
| ❌ No source traceability | ✅ Track every data point to source |
| ❌ No quality analysis by source | ✅ Quality metrics per API |
| ❌ Difficult troubleshooting | ✅ Easy debug with raw data |
| ❌ No replay capability | ✅ Can replay from any source |

**Kết quả**: Hệ thống **6 external APIs** với **intelligent data fusion** + **comprehensive raw data audit** đảm bảo tính minh bạch và khả năng troubleshoot cao nhất. 