# Entity Details API Guide

## 📋 **Tổng quan**

**Entity Details API** được thiết kế để **hiển thị thông tin chi tiết** của tàu/máy bay cho **người dùng cuối**, bao gồm:
- **📊 Merged Data** - Thông tin chính sau khi tổng hợp từ tất cả sources
- **🔍 Source Breakdown** - Dữ liệu từ từng external source riêng biệt
- **📈 Source Summary** - Tóm tắt chất lượng từng nguồn

**Mục đích:** Cung cấp **transparency** và cho phép người dùng thấy nguồn gốc của dữ liệu.

## 🎯 **Use Cases**

### **1. Chi tiết máy bay**
- ✅ Xem thông tin tổng hợp của một aircraft
- ✅ So sánh data từ FlightRadar24 vs ADS-B Exchange
- ✅ Kiểm tra quality score từng source
- ✅ Timeline tracking với source attribution

### **2. Chi tiết tàu**
- ✅ Xem thông tin tổng hợp của một vessel
- ✅ So sánh data từ MarineTraffic, VesselFinder, Chinaports, MarineTrafficV2
- ✅ Xem destination/route từ sources khác nhau
- ✅ Verify vessel information accuracy

### **3. Data Source Transparency**
- ✅ Hiển thị nguồn gốc của từng thông tin
- ✅ Quality indicators cho từng source
- ✅ Latest update time từ từng API
- ✅ Priority ranking của sources

## 📡 **API Endpoints**

### **1. Aircraft Details**

#### **Get Full Aircraft Details**
```bash
GET /api/entities/aircraft/{hexident}/details?hours=24&includeSourceData=true

# Ví dụ
GET /api/entities/aircraft/A12345/details?hours=24&includeSourceData=true
```

**Parameters:**
- `hexident` - Aircraft identifier
- `hours` - Hours to look back (default: 24)  
- `includeSourceData` - Include raw source breakdown (default: true)

**Response:**
```json
{
  "success": true,
  "message": "Aircraft details retrieved successfully",
  "data": {
    "hexident": "A12345",
    "timeWindow": {
      "start": "2024-01-01T00:00:00",
      "end": "2024-01-01T24:00:00",
      "hoursBack": 24
    },
    "processedData": {
      "recordCount": 52,
      "latestRecord": {
        "timestamp": "2024-01-01T23:45:00",
        "hexident": "A12345",
        "callsign": "VN123",
        "latitude": 10.762622,
        "longitude": 106.660172,
        "altitude": 35000,
        "speed": 450,
        "track": 090,
        "dataSource": "Merged Data"
      },
      "allRecords": [
        // ... up to 50 most recent records
      ]
    },
    "sourceData": {
      "flightradar24": {
        "source": "flightradar24",
        "totalRecords": 28,
        "priority": 1,
        "records": [
          {
            "receivedAt": "2024-01-01T23:45:30",
            "hexident": "A12345",
            "callsign": "VN123",
            "latitude": 10.762622,
            "longitude": 106.660172,
            "altitude": 35000,
            "speed": 450,
            "dataQuality": 0.95,
            "dataSource": "flightradar24"
          }
          // ... up to 20 records
        ]
      },
      "adsbexchange": {
        "source": "adsbexchange",
        "totalRecords": 24,
        "priority": 2,
        "records": [
          {
            "receivedAt": "2024-01-01T23:44:15",
            "hexident": "A12345",
            "callsign": "VN123",
            "latitude": 10.762435,
            "longitude": 106.660098,
            "altitude": 35000,
            "speed": 448,
            "dataQuality": 0.88,
            "dataSource": "adsbexchange"
          }
          // ... up to 20 records
        ]
      }
    },
    "sourceSummary": {
      "flightradar24": {
        "recordCount": 28,
        "averageQuality": 0.95,
        "latestUpdate": "2024-01-01T23:45:30",
        "priority": 1
      },
      "adsbexchange": {
        "recordCount": 24,
        "averageQuality": 0.88,
        "latestUpdate": "2024-01-01T23:44:15",
        "priority": 2
      }
    }
  }
}
```

### **2. Vessel Details**

#### **Get Full Vessel Details**
```bash
GET /api/entities/vessels/{mmsi}/details?hours=24&includeSourceData=true

# Ví dụ
GET /api/entities/vessels/123456789/details?hours=24&includeSourceData=true
```

**Response Structure** tương tự aircraft với 4 vessel sources:
```json
{
  "data": {
    "mmsi": "123456789",
    "processedData": {
      "latestRecord": {
        "timestamp": "2024-01-01T23:30:00",
        "mmsi": "123456789",
        "latitude": 10.762622,
        "longitude": 106.660172,
        "speed": 12.5,
        "course": 180,
        "navigationStatus": "Under way using engine",
        "dataSource": "Merged Data"
      }
    },
    "sourceData": {
      "marinetraffic": {
        "source": "marinetraffic",
        "totalRecords": 15,
        "priority": 1,
        "records": [
          {
            "receivedAt": "2024-01-01T23:30:45",
            "mmsi": "123456789",
            "vesselName": "CONTAINER SHIP ABC",
            "latitude": 10.762622,
            "longitude": 106.660172,
            "speed": 12.5,
            "destination": "HONG KONG",
            "vesselType": "Container Ship",
            "dataQuality": 0.92,
            "dataSource": "marinetraffic"
          }
        ]
      },
      "vesselfinder": {
        "source": "vesselfinder",
        "totalRecords": 12,
        "priority": 2,
        "records": [...]
      },
      "chinaports": {
        "source": "chinaports",
        "totalRecords": 8,
        "priority": 3,
        "records": [...]
      },
      "marinetrafficv2": {
        "source": "marinetrafficv2",
        "totalRecords": 10,
        "priority": 4,
        "records": [...]
      }
    }
  }
}
```

## 🎨 **Frontend Display Examples**

### **1. Aircraft Information Panel**
```html
<!-- Main Information (từ processedData.latestRecord) -->
<div class="main-info">
  <h2>Aircraft A12345 - VN123</h2>
  <p>Position: 10.762622, 106.660172</p>
  <p>Altitude: 35,000 ft | Speed: 450 kt</p>
  <p>Last Update: 2024-01-01 23:45:00</p>
</div>

<!-- Source Breakdown Tabs -->
<div class="source-tabs">
  <tab name="FlightRadar24" badge="28 records" quality="95%">
    <table>
      <tr><td>23:45:30</td><td>35,000 ft</td><td>450 kt</td></tr>
      <tr><td>23:44:30</td><td>35,100 ft</td><td>448 kt</td></tr>
    </table>
  </tab>
  
  <tab name="ADS-B Exchange" badge="24 records" quality="88%">
    <table>
      <tr><td>23:44:15</td><td>35,000 ft</td><td>448 kt</td></tr>
    </table>
  </tab>
</div>

<!-- Source Quality Indicators -->
<div class="source-quality">
  <span class="source-badge high">FlightRadar24: 95%</span>
  <span class="source-badge good">ADS-B Exchange: 88%</span>
</div>
```

### **2. Vessel Information Panel**
```html
<!-- Main Information -->
<div class="main-info">
  <h2>CONTAINER SHIP ABC (123456789)</h2>
  <p>Position: 10.762622, 106.660172</p>
  <p>Speed: 12.5 kt | Course: 180°</p>
  <p>Destination: HONG KONG</p>
  <p>Status: Under way using engine</p>
</div>

<!-- Multi-Source Comparison -->
<div class="source-comparison">
  <div class="source-column">
    <h4>MarineTraffic (Priority 1)</h4>
    <p>15 records | Quality: 92%</p>
    <p>Destination: HONG KONG</p>
    <p>Type: Container Ship</p>
  </div>
  
  <div class="source-column">
    <h4>VesselFinder (Priority 2)</h4>
    <p>12 records | Quality: 87%</p>
    <p>Destination: HK PORT</p>
    <p>Type: Container Vessel</p>
  </div>
  
  <div class="source-column">
    <h4>Chinaports (Priority 3)</h4>
    <p>8 records | Quality: 85%</p>
    <p>Region: China Sea</p>
  </div>
</div>
```

## 📊 **Data Quality Indicators**

### **Quality Levels for Display**
```javascript
function getQualityBadge(quality) {
  if (quality >= 0.9) return {
    class: 'badge-excellent',
    text: 'Excellent',
    color: 'green'
  };
  if (quality >= 0.8) return {
    class: 'badge-good', 
    text: 'Good',
    color: 'blue'
  };
  if (quality >= 0.7) return {
    class: 'badge-fair',
    text: 'Fair', 
    color: 'orange'
  };
  return {
    class: 'badge-poor',
    text: 'Poor',
    color: 'red'
  };
}
```

### **Source Priority Display**
```javascript
function getPriorityBadge(priority) {
  const priorities = {
    1: { text: 'Primary', color: 'gold' },
    2: { text: 'Secondary', color: 'silver' },
    3: { text: 'Tertiary', color: 'bronze' },
    4: { text: 'Backup', color: 'gray' }
  };
  return priorities[priority] || { text: 'Unknown', color: 'gray' };
}
```

## 🔍 **Interactive Features**

### **1. Source Toggle**
```javascript
// Cho phép user toggle on/off từng source
function toggleSource(sourceName, isEnabled) {
  if (isEnabled) {
    showSourceData(sourceName);
    highlightSourceOnMap(sourceName);
  } else {
    hideSourceData(sourceName);
    unhighlightSourceOnMap(sourceName);
  }
}
```

### **2. Time Range Selector**
```javascript
// Cho phép user chọn time range
function updateTimeRange(hours) {
  const url = `/api/entities/aircraft/${hexident}/details?hours=${hours}`;
  fetchAndUpdateDisplay(url);
}
```

### **3. Source Comparison View**
```javascript
// So sánh data từ multiple sources side-by-side
function showSourceComparison(timestamp) {
  const sourcesAtTime = getAllSourcesAtTimestamp(timestamp);
  displayComparisonTable(sourcesAtTime);
}
```

## 📱 **Mobile-Friendly Display**

### **Responsive Design**
```css
/* Desktop: Side-by-side sources */
@media (min-width: 768px) {
  .source-breakdown {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
  }
}

/* Mobile: Stacked tabs */
@media (max-width: 767px) {
  .source-breakdown {
    display: block;
  }
  
  .source-tab {
    margin-bottom: 1rem;
    border: 1px solid #ddd;
    border-radius: 4px;
  }
}
```

## ⚙️ **Configuration Options**

### **Display Preferences**
```javascript
const displayConfig = {
  maxRecordsPerSource: 20,      // Limit records shown per source
  showDataQuality: true,        // Show quality indicators
  showSourcePriority: true,     // Show priority badges
  showLastUpdate: true,         // Show latest update times
  enableSourceToggle: true,     // Allow source on/off
  defaultTimeRange: 24,         // Default hours to display
  refreshInterval: 30000        // Auto-refresh every 30s
};
```

### **Field Selection**
```javascript
// Aircraft fields to display
const aircraftFields = [
  'timestamp', 'latitude', 'longitude', 
  'altitude', 'speed', 'callsign', 'dataQuality'
];

// Vessel fields to display  
const vesselFields = [
  'timestamp', 'latitude', 'longitude',
  'speed', 'destination', 'vesselName', 'dataQuality'
];
```

## 🎯 **Best Practices**

### **1. User Experience**
- ✅ **Main info prominent** - Merged data ở vị trí nổi bật nhất
- ✅ **Source details collapsible** - Raw data có thể thu gọn/mở rộng
- ✅ **Quality indicators clear** - Dễ nhận biết quality tốt/xấu
- ✅ **Update time visible** - Luôn hiển thị thời gian cập nhật

### **2. Performance**
- ✅ **Pagination** - Limit số records hiển thị
- ✅ **Lazy loading** - Load source details khi cần
- ✅ **Caching** - Cache data để tránh reload liên tục
- ✅ **Progressive loading** - Load main info trước, source details sau

### **3. Data Transparency**
- ✅ **Source attribution** - Luôn ghi rõ nguồn
- ✅ **Quality scores** - Hiển thị confidence level
- ✅ **Update frequency** - Cho biết tần suất cập nhật
- ✅ **Data age** - Hiển thị tuổi của dữ liệu

## 💡 **Implementation Examples**

### **React Component Structure**
```jsx
function EntityDetails({ entityId, entityType }) {
  const [data, setData] = useState(null);
  const [showSources, setShowSources] = useState(true);
  const [timeRange, setTimeRange] = useState(24);

  return (
    <div className="entity-details">
      {/* Main Information */}
      <MainInfo data={data?.processedData?.latestRecord} />
      
      {/* Controls */}
      <Controls 
        showSources={showSources}
        onToggleSources={setShowSources}
        timeRange={timeRange}
        onTimeRangeChange={setTimeRange}
      />
      
      {/* Source Breakdown */}
      {showSources && (
        <SourceBreakdown 
          sourceData={data?.sourceData}
          sourceSummary={data?.sourceSummary}
        />
      )}
    </div>
  );
}
```

### **Vue.js Template**
```vue
<template>
  <div class="entity-details">
    <!-- Main Info Card -->
    <v-card class="main-info mb-4">
      <v-card-title>{{ entityTitle }}</v-card-title>
      <v-card-text>
        <entity-main-info :data="processedData.latestRecord" />
      </v-card-text>
    </v-card>

    <!-- Source Tabs -->
    <v-tabs v-if="showSources">
      <v-tab 
        v-for="(source, name) in sourceData" 
        :key="name"
        :badge="source.totalRecords"
      >
        {{ name }}
        <quality-badge :quality="sourceSummary[name].averageQuality" />
      </v-tab>

      <v-tab-item v-for="(source, name) in sourceData" :key="name">
        <source-data-table :records="source.records" />
      </v-tab-item>
    </v-tabs>
  </div>
</template>
```

## 📋 **Summary**

**Entity Details API** cung cấp:

| **Feature** | **Benefit** |
|-------------|-------------|
| **Merged Data Display** | Thông tin chính đã tối ưu cho user |
| **Source Breakdown** | Transparency về nguồn gốc dữ liệu |
| **Quality Indicators** | Confidence level của từng source |
| **Time Range Flexibility** | Xem data theo thời gian tùy chọn |
| **Mobile Responsive** | Hoạt động tốt trên mọi device |

**Perfect cho:** Entity detail pages, debugging interface, data transparency, user trust building!

**Kết quả:** Users có thể thấy cả **thông tin tổng hợp** và **nguồn gốc chi tiết** của mọi dữ liệu trong hệ thống. 