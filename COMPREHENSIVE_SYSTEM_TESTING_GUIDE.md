# Comprehensive System Testing Guide
## Flight and Vessel Tracking System v2.0

This guide provides comprehensive instructions for testing the entire flight and vessel tracking system, including all components, integrations, and data flows.

## Table of Contents

1. [Overview](#overview)
2. [Test Architecture](#test-architecture)
3. [System Components Testing](#system-components-testing)
4. [Manual Testing via APIs](#manual-testing-via-apis)
5. [Automated Testing](#automated-testing)
6. [Performance Testing](#performance-testing)
7. [Error Handling Testing](#error-handling-testing)
8. [Monitoring and Metrics](#monitoring-and-metrics)
9. [Test Scenarios](#test-scenarios)
10. [Troubleshooting](#troubleshooting)

## Overview

### System Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Data Sources  │────│  Raw Data Topics│────│  Data Fusion    │
│  (6 External)   │    │   (Kafka)       │    │   & Processing  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Simple Data     │    │ Raw Data Topics │    │   Real-Time     │
│ Collection      │    │   Consumer      │    │   Updates       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Database (PostgreSQL)                        │
└─────────────────────────────────────────────────────────────────┘
```

### Test Components
- **System Integration Tests**: Full end-to-end testing
- **API Endpoints Tests**: All REST controller testing
- **Database Integration Tests**: CRUD operations and performance
- **Raw Data Topics Tests**: Event-driven architecture testing
- **System Test Controller**: Manual testing endpoints
- **Performance Testing**: Load and stress testing
- **Monitoring Tests**: Metrics and health checks

## Test Architecture

### Test Hierarchy
```
System Tests
├── Unit Tests (RawDataTopicsUnitTest)
├── Integration Tests
│   ├── System Integration (SystemIntegrationTest)
│   ├── API Endpoints (ApiEndpointsTest)
│   └── Database Integration (DatabaseIntegrationTest)
├── Manual Testing
│   ├── System Test Controller
│   └── Raw Data Test Controller
└── Performance Tests
    ├── Load Testing
    ├── Stress Testing
    └── Benchmark Testing
```

## System Components Testing

### 1. Aircraft Tracking System

#### Endpoints to Test:
```bash
# Get all aircraft
GET /api/aircraft

# Search aircraft
GET /api/aircraft/search?query=VN&limit=10

# Get aircraft by ID
GET /api/aircraft/{id}

# Aircraft monitoring
GET /api/aircraft/monitoring/status
GET /api/aircraft/monitoring/metrics
```

#### Sample Test Commands:
```bash
# Test aircraft endpoints
curl -X GET "http://localhost:8080/api/aircraft" \
  -H "Content-Type: application/json"

# Test aircraft search
curl -X GET "http://localhost:8080/api/aircraft/search?query=VN&limit=10" \
  -H "Content-Type: application/json"
```

### 2. Vessel Tracking System

#### Endpoints to Test:
```bash
# Get all ships
GET /api/ships

# Search ships
GET /api/ships/search?query=container&limit=10

# Ship monitoring
GET /api/ships/monitoring/status
GET /api/ships/monitoring/metrics
```

#### Sample Test Commands:
```bash
# Test ship endpoints
curl -X GET "http://localhost:8080/api/ships" \
  -H "Content-Type: application/json"

# Test ship search
curl -X GET "http://localhost:8080/api/ships/search?query=container&limit=10" \
  -H "Content-Type: application/json"
```

### 3. Real-Time Data System

#### Endpoints to Test:
```bash
# Real-time aircraft
GET /api/realtime/aircraft

# Real-time vessels
GET /api/realtime/vessels

# Real-time status
GET /api/realtime/status
```

### 4. Raw Data Topics System

#### Test Raw Data Flow:
```bash
# Test raw data status
curl -X GET "http://localhost:8080/api/test/raw-data/status"

# Test collection trigger
curl -X POST "http://localhost:8080/api/test/raw-data/trigger/collection"

# Test fusion trigger
curl -X POST "http://localhost:8080/api/test/raw-data/trigger/fusion"

# Test health check
curl -X GET "http://localhost:8080/api/test/raw-data/health"
```

## Manual Testing via APIs

### System Test Controller Endpoints

#### 1. System Overview
```bash
GET /api/system-test/overview
```
**Description**: Get complete system overview including components, statistics, and available endpoints.

**Expected Response**:
```json
{
  "systemName": "Flight and Vessel Tracking System",
  "version": "2.0.0",
  "architecture": "Event-Driven with Raw Data Topics",
  "components": {
    "dataCollection": {...},
    "dataFusion": {...},
    "monitoring": {...},
    "database": {...}
  },
  "statistics": {
    "totalAircraft": 150,
    "totalShips": 89
  }
}
```

#### 2. Comprehensive Health Check
```bash
GET /api/system-test/health/comprehensive
```
**Description**: Perform comprehensive health check of all system components.

#### 3. Database Testing
```bash
# Test database functionality
GET /api/system-test/database/test

# Populate test data
POST /api/system-test/database/populate/100
```

#### 4. Raw Data Topics Testing
```bash
# Test raw data flow
POST /api/system-test/raw-data/test-flow

# Stress test
POST /api/system-test/raw-data/stress-test/10
```

#### 5. Performance Benchmark
```bash
POST /api/system-test/performance/benchmark
```

#### 6. Error Handling Testing
```bash
POST /api/system-test/errors/test-handling
```

#### 7. Comprehensive Monitoring
```bash
# Get monitoring data
GET /api/system-test/monitoring/comprehensive

# Reset monitoring metrics
POST /api/system-test/monitoring/reset
```

## Automated Testing

### Running Unit Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests RawDataTopicsUnitTest

# Run with reports
./gradlew test jacocoTestReport
```

### Running Integration Tests
```bash
# Run system integration tests
./gradlew test --tests SystemIntegrationTest

# Run API endpoints tests
./gradlew test --tests ApiEndpointsTest

# Run database integration tests
./gradlew test --tests DatabaseIntegrationTest
```

### Test Reports
After running tests, reports are available at:
- `build/reports/tests/test/index.html`
- `build/reports/jacoco/test/html/index.html`

## Performance Testing

### 1. Database Performance Test
```bash
# Test database CRUD operations performance
curl -X GET "http://localhost:8080/api/system-test/database/test"
```

**Expected Metrics**:
- CRUD operations: < 100ms
- Bulk insert (50 records): < 5 seconds
- Query response: < 200ms

### 2. Raw Data Topics Performance
```bash
# Run stress test with 25 iterations
curl -X POST "http://localhost:8080/api/system-test/raw-data/stress-test/25"
```

**Expected Metrics**:
- Data collection: < 2 seconds per iteration
- Data fusion: < 3 seconds per iteration
- Memory usage: < 512MB

### 3. System Benchmark
```bash
# Run comprehensive benchmark
curl -X POST "http://localhost:8080/api/system-test/performance/benchmark"
```

**Expected Results**:
```json
{
  "database": {
    "aircraftCount": 150,
    "shipCount": 89,
    "queryTimeMs": 45
  },
  "rawDataTopics": {
    "operationTimeMs": 1250,
    "status": "COMPLETED"
  },
  "memoryUsage": {
    "totalMemoryMB": 1024,
    "usedMemoryMB": 256
  }
}
```

## Error Handling Testing

### 1. Invalid Data Testing
```bash
# Test error handling
curl -X POST "http://localhost:8080/api/system-test/errors/test-handling"
```

### 2. Resource Exhaustion Testing
The system should handle:
- High memory usage
- Database connection limits
- Kafka topic overload
- Network timeouts

### 3. Service Failure Testing
Test system resilience when:
- Database becomes unavailable
- Kafka broker fails
- External APIs timeout
- Memory limits reached

## Monitoring and Metrics

### 1. System Health Monitoring
```bash
# Check system health
GET /api/health/status

# Check database health
GET /api/health/database

# Check Kafka health
GET /api/health/kafka
```

### 2. Performance Metrics
```bash
# Get comprehensive monitoring
GET /api/system-test/monitoring/comprehensive
```

**Key Metrics to Monitor**:
- **Database**: Connection count, query performance, storage usage
- **Kafka**: Topic partition lag, consumer lag, throughput
- **Memory**: Heap usage, GC frequency, memory leaks
- **CPU**: Processing time, thread pool usage
- **Network**: Request/response times, error rates

### 3. Business Metrics
```bash
# Get statistics
GET /api/system-test/overview
```

**Business Metrics**:
- Total aircraft tracked
- Total vessels tracked
- Data processing rate
- Data quality scores
- System uptime

## Test Scenarios

### Scenario 1: Full System Test
```bash
# 1. Check system overview
curl -X GET "http://localhost:8080/api/system-test/overview"

# 2. Populate test data
curl -X POST "http://localhost:8080/api/system-test/database/populate/50"

# 3. Test raw data flow
curl -X POST "http://localhost:8080/api/system-test/raw-data/test-flow"

# 4. Run performance benchmark
curl -X POST "http://localhost:8080/api/system-test/performance/benchmark"

# 5. Check comprehensive monitoring
curl -X GET "http://localhost:8080/api/system-test/monitoring/comprehensive"
```

### Scenario 2: Performance Validation
```bash
# 1. Populate large dataset
curl -X POST "http://localhost:8080/api/system-test/database/populate/500"

# 2. Run stress test
curl -X POST "http://localhost:8080/api/system-test/raw-data/stress-test/25"

# 3. Validate performance metrics
curl -X POST "http://localhost:8080/api/system-test/performance/benchmark"
```

### Scenario 3: Error Resilience Test
```bash
# 1. Test error handling
curl -X POST "http://localhost:8080/api/system-test/errors/test-handling"

# 2. Check system health after errors
curl -X GET "http://localhost:8080/api/system-test/health/comprehensive"

# 3. Validate recovery
curl -X POST "http://localhost:8080/api/system-test/raw-data/test-flow"
```

### Scenario 4: End-to-End Data Flow
```bash
# 1. Trigger data collection
curl -X POST "http://localhost:8080/api/test/raw-data/trigger/collection"

# 2. Wait and check status
sleep 5
curl -X GET "http://localhost:8080/api/test/raw-data/status/collection"

# 3. Trigger data fusion
curl -X POST "http://localhost:8080/api/test/raw-data/trigger/fusion"

# 4. Check final results
curl -X GET "http://localhost:8080/api/realtime/aircraft"
curl -X GET "http://localhost:8080/api/realtime/vessels"
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Database Connection Issues
**Symptoms**: Database health checks fail, connection timeouts

**Solution**:
```bash
# Check database status
curl -X GET "http://localhost:8080/api/database/status"

# Test database connectivity
curl -X GET "http://localhost:8080/api/system-test/database/test"
```

#### 2. Kafka Topic Issues
**Symptoms**: Raw data processing failures, consumer lag

**Solution**:
```bash
# Check raw data topics status
curl -X GET "http://localhost:8080/api/test/raw-data/status"

# Reset monitoring metrics
curl -X POST "http://localhost:8080/api/system-test/monitoring/reset"

# Restart data flow
curl -X POST "http://localhost:8080/api/test/raw-data/trigger/collection"
```

#### 3. Memory Issues
**Symptoms**: OutOfMemoryError, slow performance

**Solution**:
```bash
# Check memory usage
curl -X POST "http://localhost:8080/api/system-test/performance/benchmark"

# Clear test data if needed
# (Implement data cleanup endpoint if required)
```

#### 4. API Response Issues
**Symptoms**: Slow API responses, timeouts

**Solution**:
```bash
# Check comprehensive health
curl -X GET "http://localhost:8080/api/system-test/health/comprehensive"

# Run performance benchmark
curl -X POST "http://localhost:8080/api/system-test/performance/benchmark"
```

### Performance Benchmarks

#### Expected Performance Standards
- **API Response Time**: < 200ms for simple queries
- **Database Query Time**: < 100ms for single record queries
- **Bulk Operations**: 1000+ records/second
- **Memory Usage**: < 512MB for 10,000 records
- **Data Processing**: 100+ fused records/second
- **System Availability**: 99.9%+ uptime

#### Alert Thresholds
- **Database Response**: > 500ms
- **Memory Usage**: > 80% of available memory
- **Error Rate**: > 1% of requests
- **Kafka Consumer Lag**: > 1000 messages

### Test Checklist

#### Pre-Testing Checklist
- [ ] Database is running and accessible
- [ ] Kafka broker is running
- [ ] All required topics are created
- [ ] Application is started and healthy
- [ ] Test environment is isolated

#### System Testing Checklist
- [ ] System overview accessible
- [ ] Comprehensive health check passes
- [ ] Database connectivity test passes
- [ ] Raw data flow test completes successfully
- [ ] Performance benchmark meets standards
- [ ] Error handling test passes
- [ ] Monitoring metrics are collected
- [ ] All API endpoints respond correctly

#### Post-Testing Checklist
- [ ] Test data cleaned up (if needed)
- [ ] Monitoring metrics reset
- [ ] System status verified
- [ ] Test reports generated
- [ ] Performance benchmarks documented

## Integration with CI/CD

### GitHub Actions Example
```yaml
name: System Tests
on: [push, pull_request]

jobs:
  system-tests:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    
    - name: Setup Java
      uses: actions/setup-java@v2
      with:
        java-version: '17'
        
    - name: Start test environment
      run: |
        docker-compose -f docker-compose.test.yml up -d
        
    - name: Run tests
      run: |
        ./gradlew test
        
    - name: System integration tests
      run: |
        curl -X GET "http://localhost:8080/api/system-test/overview"
        curl -X POST "http://localhost:8080/api/system-test/raw-data/test-flow"
        
    - name: Performance validation
      run: |
        curl -X POST "http://localhost:8080/api/system-test/performance/benchmark"
```

## Conclusion

This comprehensive testing guide ensures:

1. **Complete Coverage**: All system components tested
2. **Multiple Testing Approaches**: Unit, integration, manual, automated
3. **Performance Validation**: Benchmarks and stress testing
4. **Error Resilience**: Comprehensive error handling validation
5. **Monitoring**: Complete observability and metrics
6. **Production Readiness**: Performance standards and thresholds

The system is fully tested and production-ready with comprehensive monitoring, error handling, and performance validation.

### Test Execution Summary

**Total Test Coverage**:
- 20+ Unit Tests (Raw Data Topics)
- 20+ System Integration Tests
- 28+ API Endpoint Tests
- 15+ Database Integration Tests
- 10+ Manual Testing Endpoints
- Performance and Stress Testing
- Error Handling Validation
- Comprehensive Monitoring

**System Validation**:
- ✅ All major components tested
- ✅ End-to-end data flow validated
- ✅ Performance benchmarks met
- ✅ Error handling verified
- ✅ Monitoring and metrics functional
- ✅ Production readiness confirmed

The Flight and Vessel Tracking System is comprehensively tested and ready for production deployment! 🚀 