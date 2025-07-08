# 🗄️ Data Storage Optimization Guide

## 📋 **Tổng quan**

Hệ thống tracking có thể tạo ra **lượng dữ liệu rất lớn**:
- **49,019 aircraft** × 30 giây = ~142 triệu records/ngày  
- **30,000 vessels** × 60 giây = ~43 triệu records/ngày
- **Tổng: ~185 triệu raw records/ngày**

## ⚠️ **Vấn đề Storage**

### **Trước khi tối ưu:**
```
Raw Data:     185M records/day × 30 days = 5.5B records
Storage:      ~500GB/month chỉ cho raw data
DB Load:      Quá tải với continuous inserts
```

### **Sau tối ưu:**
```
Raw Data:     Disabled cho development
              2% sampling cho production = 100M records/month  
Storage:      ~10GB/month cho raw data
DB Load:      Chỉ focus vào processed data
```

## 🛠️ **Cấu hình Environment**

### **Development (Mặc định)**
```properties
# application.properties
raw.data.storage.enabled=false          # Tắt raw storage
raw.data.sampling.rate=0.05             # 5% nếu enable
raw.data.retention.days=7               # 7 ngày retention
```

### **Production**
```bash
# Chạy với production profile
java -jar app.jar --spring.profiles.active=prod

# Hoặc set environment
export SPRING_PROFILES_ACTIVE=prod
```

```properties
# application-prod.properties  
raw.data.storage.enabled=true           # Enable cho audit
raw.data.sampling.rate=0.02             # Chỉ 2% data
raw.data.retention.days=7               # Ngắn hạn
raw.data.smart-filtering.enabled=true   # Smart filtering
```

## 📊 **Monitoring & Statistics**

### **Kiểm tra Database Size**
```sql
-- Check table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check raw data retention
SELECT 
    data_source,
    COUNT(*) as records,
    MIN(received_at) as oldest,
    MAX(received_at) as newest
FROM raw_aircraft_data 
GROUP BY data_source;
```

### **API Endpoints cho Monitoring**
```bash
# Storage statistics
curl http://localhost:8080/api/raw-data/statistics

# Health check
curl http://localhost:8080/api/raw-data/health

# Manual cleanup (if needed)
curl -X POST http://localhost:8080/api/raw-data/cleanup
```

## 🚀 **Optimization Strategies**

### **1. Smart Filtering (Production)**
Chỉ lưu raw data khi:
- Thay đổi vị trí > 200m
- Thay đổi altitude > 200ft  
- Emergency status thay đổi
- Source quality thay đổi

### **2. Sampling Strategy**
```properties
# Chỉ lưu 2% random raw data
raw.data.sampling.rate=0.02

# Hoặc priority-based sampling
raw.data.sampling.priority=emergency,military,special
```

### **3. Retention Policies**
```sql
-- TimescaleDB tự động cleanup
-- Raw data: 7 days
-- Processed data: 30 days  
-- Alert data: 1 year
```

### **4. Compression**
```properties
# Compress raw data sau 1 ngày
raw.data.compression.enabled=true
timescale.compression.interval=1d
```

## 🎯 **Khuyến nghị theo Use Case**

### **Development/Testing**
```properties
raw.data.storage.enabled=false
# Focus vào testing processed data flow
```

### **Staging**  
```properties
raw.data.storage.enabled=true
raw.data.sampling.rate=0.1             # 10% cho testing
raw.data.retention.days=3              # Ngắn hạn
```

### **Production**
```properties
raw.data.storage.enabled=true
raw.data.sampling.rate=0.02            # 2% cho audit
raw.data.retention.days=7              # Compliance minimum
raw.data.smart-filtering.enabled=true  # Optimize storage
```

### **High-Volume Production**
```properties
raw.data.storage.enabled=false         # Disable completely
# Rely on processed data + external API logs for audit
```

## 📈 **Performance Impact**

### **Database Load Reduction**
```
Before: 185M inserts/day (raw) + 10M inserts/day (processed)
After:  10M inserts/day (processed only)
Reduction: 94% insert load
```

### **Storage Reduction**  
```
Before: 500GB/month
After:  10GB/month  
Reduction: 98% storage
```

### **Query Performance**
```
Raw tables:     Không bị bloat
Processed data: Faster queries
Indexes:        Smaller và efficient hơn
```

## 🛡️ **Data Governance**

### **Audit Compliance**
- **Processed data**: Đầy đủ cho business logic
- **Source tracking**: Metadata trong processed records
- **Error logs**: Captured trong application logs
- **API logs**: External API calls logged separately

### **Rollback Strategy**
```bash
# Nếu cần enable raw storage lại
curl -X POST /actuator/env \
  -d '{"name":"raw.data.storage.enabled","value":"true"}'

# Restart không cần thiết với Spring Boot Actuator
```

## 🔧 **Troubleshooting**

### **Nếu Database Full**
```bash
# 1. Manual cleanup ngay lập tức
curl -X POST /api/raw-data/cleanup

# 2. Giảm retention
curl -X POST /actuator/env \
  -d '{"name":"raw.data.retention.days","value":"1"}'

# 3. Disable raw storage
curl -X POST /actuator/env \
  -d '{"name":"raw.data.storage.enabled","value":"false"}'
```

### **Performance Issues**
```sql
-- Check for table bloat
SELECT 
    schemaname,
    tablename,
    n_dead_tup,
    n_live_tup,
    round(n_dead_tup::float/n_live_tup::float*100,2) as dead_ratio
FROM pg_stat_user_tables 
WHERE n_live_tup > 0
ORDER BY dead_ratio DESC;

-- Manual vacuum if needed  
VACUUM ANALYZE raw_aircraft_data;
VACUUM ANALYZE raw_vessel_data;
```

## ✅ **Best Practices**

1. **Development**: Raw storage OFF
2. **Production**: Raw storage ON với sampling  
3. **Monitoring**: Daily size checks
4. **Cleanup**: Automated với manual backup
5. **Scaling**: Horizontal partitioning nếu cần

Với configuration này, bạn sẽ có hệ thống **efficient, scalable** và vẫn **audit-compliant** khi cần! 🚀 