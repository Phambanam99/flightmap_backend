# Data Comparison API Guide

## 📋 **Tổng quan**

**Data Comparison API** cung cấp khả năng **so sánh chi tiết** giữa:
- **Raw data từ từng external source** (FlightRadar24, ADS-B Exchange, MarineTraffic, VesselFinder, Chinaports, MarineTrafficV2)  
- **Processed data sau khi fusion/merge**

Điều này giúp **đánh giá hiệu quả data fusion**, **debug quality issues**, và **optimize source configuration**.

## 🎯 **Mục đích sử dụng**

### **1. Data Fusion Validation**
- ✅ Verify fusion không làm mất thông tin quan trọng
- ✅ So sánh quality trước và sau fusion
- ✅ Đánh giá compression ratio (raw vs processed)

### **2. Source Performance Analysis**
- ✅ Phân tích contribution của từng source
- ✅ Monitor response time từng API
- ✅ Identify low-quality hoặc problematic sources

### **3. Quality Optimization**
- ✅ Fine-tune fusion parameters
- ✅ Adjust source priorities
- ✅ Optimize polling frequencies

## 📡 **API Endpoints**

### **1. Individual Entity Comparison**

#### **Compare Aircraft Data**
```bash
GET /api/data-comparison/aircraft/{hexident}?hours=24

# Ví dụ
GET /api/data-comparison/aircraft/A12345?hours=24
```

**Response:**
```json
{
  "success": true,
  "message": "Aircraft data comparison completed successfully",
  "data": {
    "aircraft": "A12345",
    "timeWindow": {
      "start": "2024-01-01T00:00:00",
      "end": "2024-01-01T24:00:00"
    },
    "rawDataSources": {
      "flightradar24": {
        "recordCount": 45,
        "averageQuality": 0.92,
        "averageResponseTime": 1250.5,
        "latestTimestamp": "2024-01-01T23:45:00",
        "validRecords": 44,
        "sourcePriority": 1
      },
      "adsbexchange": {
        "recordCount": 38,
        "averageQuality": 0.88,
        "averageResponseTime": 1800.2,
        "latestTimestamp": "2024-01-01T23:40:00",
        "validRecords": 37,
        "sourcePriority": 2
      }
    },
    "processedData": {
      "recordCount": 52,
      "timeRange": {
        "earliest": "2024-01-01T00:15:00",
        "latest": "2024-01-01T23:45:00"
      }
    },
    "fusionMetrics": {
      "totalRawRecords": 83,
      "processedRecords": 52,
      "compressionRatio": 0.626,
      "sourcesContributing": 2,
      "fusionEfficiency": 0.626
    },
    "dataQualityComparison": {
      "averageRawDataQuality": 0.90,
      "qualityBySource": {
        "flightradar24": 0.92,
        "adsbexchange": 0.88
      },
      "estimatedProcessedQuality": 0.99
    }
  }
}
```

#### **Compare Vessel Data**
```bash
GET /api/data-comparison/vessels/{mmsi}?hours=24

# Ví dụ
GET /api/data-comparison/vessels/123456789?hours=24
```

**Response Structure** tương tự aircraft với 4 vessel sources:
- `marinetraffic`
- `vesselfinder` 
- `chinaports`
- `marinetrafficv2`

### **2. Overall Fusion Effectiveness**

```bash
GET /api/data-comparison/fusion-effectiveness?start=2024-01-01T00:00:00&end=2024-01-02T00:00:00
```

**Response:**
```json
{
  "success": true,
  "message": "Fusion effectiveness analysis completed successfully",
  "data": {
    "period": {
      "start": "2024-01-01T00:00:00",
      "end": "2024-01-02T00:00:00"
    },
    "aircraftFusion": {
      "qualityStatsBySource": [
        ["flightradar24", 0.92, 1250],
        ["adsbexchange", 0.88, 980]
      ],
      "recordCountsBySource": [
        ["flightradar24", 1250],
        ["adsbexchange", 980]
      ],
      "overallEffectiveness": 0.89
    },
    "vesselFusion": {
      "qualityStatsBySource": [
        ["marinetraffic", 0.90, 850],
        ["vesselfinder", 0.87, 720],
        ["chinaports", 0.85, 300],
        ["marinetrafficv2", 0.92, 450]
      ],
      "recordCountsBySource": [
        ["marinetraffic", 850],
        ["vesselfinder", 720],
        ["chinaports", 300],
        ["marinetrafficv2", 450]
      ],
      "overallEffectiveness": 0.88
    },
    "overallMetrics": {
      "totalSources": 6,
      "aircraftSources": 2,
      "vesselSources": 4,
      "fusionEnabled": true,
      "recommendedOptimizations": [
        "Monitor source response times regularly",
        "Adjust priority weights based on quality metrics",
        "Consider increasing polling frequency for high-quality sources"
      ]
    }
  }
}
```

### **3. Source Contribution Analysis**

```bash
GET /api/data-comparison/source-contribution?start=2024-01-01T00:00:00&end=2024-01-02T00:00:00
```

**Response:**
```json
{
  "success": true,
  "message": "Source contribution analysis completed successfully",
  "data": {
    "period": {
      "start": "2024-01-01T00:00:00",
      "end": "2024-01-02T00:00:00"
    },
    "aircraftSources": {
      "entityType": "aircraft",
      "totalRecords": 2230,
      "sourceContributions": {
        "flightradar24": {
          "recordCount": 1250,
          "percentage": 56.05,
          "contributionLevel": "HIGH"
        },
        "adsbexchange": {
          "recordCount": 980,
          "percentage": 43.95,
          "contributionLevel": "HIGH"
        }
      }
    },
    "vesselSources": {
      "entityType": "vessel",
      "totalRecords": 2320,
      "sourceContributions": {
        "marinetraffic": {
          "recordCount": 850,
          "percentage": 36.64,
          "contributionLevel": "MEDIUM"
        },
        "vesselfinder": {
          "recordCount": 720,
          "percentage": 31.03,
          "contributionLevel": "MEDIUM"
        },
        "marinetrafficv2": {
          "recordCount": 450,
          "percentage": 19.40,
          "contributionLevel": "LOW"
        },
        "chinaports": {
          "recordCount": 300,
          "percentage": 12.93,
          "contributionLevel": "LOW"
        }
      }
    },
    "recommendations": [
      "Review low-contributing sources for cost optimization",
      "Increase retention period for high-quality sources", 
      "Consider load balancing between similar sources",
      "Implement quality-based source selection"
    ]
  }
}
```

## 📊 **Key Metrics Explained**

### **Fusion Metrics**
| Metric | Description | Good Range |
|--------|-------------|------------|
| **compressionRatio** | processed / total_raw | 0.5 - 0.8 |
| **fusionEfficiency** | How well fusion works | 0.7 - 1.0 |
| **sourcesContributing** | Number of active sources | 2+ |

### **Contribution Levels**
| Level | Percentage | Action |
|-------|------------|--------|
| **HIGH** | >40% | Monitor closely |
| **MEDIUM** | 20-40% | Normal operation |
| **LOW** | 5-20% | Consider optimization |
| **MINIMAL** | <5% | Review necessity |

### **Quality Metrics**
| Metric | Description | Good Range |
|--------|-------------|------------|
| **averageQuality** | Data quality score | 0.8 - 1.0 |
| **averageResponseTime** | API response time (ms) | <2000ms |
| **validRecords** | Records passing validation | >95% |

## 🔍 **Phân tích kết quả**

### **Scenario 1: Fusion hiệu quả**
```json
{
  "fusionMetrics": {
    "compressionRatio": 0.65,        // Good: Reduced duplicates
    "fusionEfficiency": 0.85,        // Good: High efficiency  
    "sourcesContributing": 4         // Good: Multiple sources
  },
  "dataQualityComparison": {
    "averageRawDataQuality": 0.88,
    "estimatedProcessedQuality": 0.97  // Improvement after fusion
  }
}
```
**✅ Kết luận:** Fusion working well, improves quality

### **Scenario 2: Có vấn đề cần optimize**
```json
{
  "fusionMetrics": {
    "compressionRatio": 0.95,        // ⚠️ Too high: Not removing duplicates
    "fusionEfficiency": 0.45,        // ❌ Low: Poor fusion
    "sourcesContributing": 1         // ❌ Only 1 source working
  }
}
```
**❌ Kết luận:** Check source health, fusion parameters

### **Scenario 3: Source imbalance**
```json
{
  "sourceContributions": {
    "source1": {"percentage": 85, "contributionLevel": "HIGH"},
    "source2": {"percentage": 10, "contributionLevel": "LOW"},
    "source3": {"percentage": 5, "contributionLevel": "MINIMAL"}
  }
}
```
**⚠️ Kết luận:** Over-reliance on 1 source, review others

## 🛠️ **Troubleshooting Guide**

### **Problem: Low Fusion Efficiency**
```bash
# Check individual aircraft/vessel
GET /api/data-comparison/aircraft/A12345

# Analyze source contribution  
GET /api/data-comparison/source-contribution
```

**Possible Causes:**
- One source down → Check `/api/raw-data/health`
- Poor data quality → Check `/api/raw-data/quality-analysis`
- Wrong fusion parameters → Review fusion config

### **Problem: High Compression Ratio (>0.9)**
```bash
# Check for data overlap
GET /api/data-comparison/fusion-effectiveness
```

**Possible Causes:**
- Not enough deduplication
- Similar data from multiple sources  
- Fusion tolerance too loose

### **Problem: Source Contributing Nothing**
```bash
# Check raw data statistics
GET /api/raw-data/statistics

# Check source health
GET /api/raw-data/health
```

**Possible Causes:**
- API endpoint down
- Authentication issues
- Geographic coverage mismatch

## 📈 **Optimization Workflow**

### **Daily Monitoring**
```bash
# 1. Check overall effectiveness
curl GET "/api/data-comparison/fusion-effectiveness" | jq '.data.overallMetrics'

# 2. Review source contributions  
curl GET "/api/data-comparison/source-contribution" | jq '.data.recommendations'

# 3. Check source health
curl GET "/api/raw-data/health"
```

### **Weekly Analysis**
```bash
# 1. Analyze specific entities with issues
curl GET "/api/data-comparison/aircraft/PROBLEMATIC_ID"

# 2. Review quality trends
curl GET "/api/raw-data/quality-analysis"

# 3. Update fusion parameters if needed
# (Update application.properties and restart)
```

### **Monthly Review**
```bash
# 1. Source performance analysis
curl GET "/api/data-comparison/source-contribution?start=MONTH_START&end=MONTH_END"

# 2. Cost-benefit analysis of each source
# 3. Consider adding/removing sources
# 4. Adjust priorities based on contribution data
```

## 🔧 **Configuration Tuning**

### **Based on Comparison Results**

#### **If FlightRadar24 consistently outperforms ADS-B Exchange:**
```properties
# Increase FlightRadar24 priority
data.fusion.priority.flightradar24=1
data.fusion.priority.adsbexchange=3

# Increase polling frequency for better source
external.api.flightradar24.poll-interval=20000
external.api.adsbexchange.poll-interval=45000
```

#### **If Chinaports has minimal contribution:**
```properties
# Consider disabling or reducing frequency
external.api.chinaports.enabled=false
# Or reduce polling
external.api.chinaports.poll-interval=300000
```

#### **If fusion efficiency is low:**
```properties
# Tighten fusion parameters
fusion.aircraft.position.tolerance.km=0.3
fusion.aircraft.time.tolerance.minutes=1
fusion.vessel.position.tolerance.km=0.5
fusion.vessel.time.tolerance.minutes=3
```

## 📋 **Best Practices**

### **1. Regular Monitoring**
- ✅ Check fusion effectiveness daily
- ✅ Monitor source contribution weekly
- ✅ Review individual entity issues as needed

### **2. Quality-based Optimization**
- ✅ Prioritize high-quality sources
- ✅ Reduce polling for poor sources
- ✅ Adjust fusion parameters based on results

### **3. Cost Optimization**
- ✅ Disable minimal-contributing sources
- ✅ Balance API costs vs data quality
- ✅ Use geographic-based source selection

### **4. Proactive Issue Detection**
- ✅ Set up alerts for low fusion efficiency
- ✅ Monitor source health automatically  
- ✅ Track quality degradation trends

## 🎯 **Success Metrics**

| Metric | Target | Status |
|--------|---------|---------|
| **Fusion Efficiency** | >0.7 | Monitor daily |
| **Average Quality** | >0.85 | Monitor weekly |
| **Sources Contributing** | All 6 active | Monitor daily |
| **Response Times** | <2000ms avg | Monitor hourly |
| **Valid Records** | >95% | Monitor daily |

**Kết quả:** API này cung cấp **complete visibility** vào quá trình data fusion, giúp **optimize performance** và **ensure data quality** từ tất cả 6 external sources. 