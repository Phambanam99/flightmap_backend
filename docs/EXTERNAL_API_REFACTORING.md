# Modern JSON-to-DTO Mapping Refactoring

## Overview
This document explains the refactoring of external API services from manual JsonNode parsing to modern ObjectMapper direct mapping.

## Problem with Old Approach
The original implementation used manual JsonNode parsing with verbose field-by-field extraction:

```java
// OLD WAY - Manual JsonNode parsing ❌
private VesselTrackingRequest parseVesselFromChinaports(JsonNode data) {
    return VesselTrackingRequest.builder()
            .mmsi(getTextSafely(data, "mmsi"))
            .latitude(getDoubleSafely(data, "lat"))
            .longitude(getDoubleSafely(data, "lon"))
            // ... 20+ more fields with manual parsing
            .build();
}

// Required multiple helper methods for safe parsing
private String getTextSafely(JsonNode node, String... fieldNames) {
    // 15+ lines of null checking and type conversion
}
private Double getDoubleSafely(JsonNode node, String... fieldNames) {
    // 20+ lines of type conversion logic
}
// ... similar methods for Integer, etc.
```

**Problems:**
- 300+ lines of boilerplate code per service
- Error-prone manual type conversion
- Difficult to maintain and test
- No compile-time type safety
- Inconsistent field mapping across services

## New Modern Approach
Use ObjectMapper with DTOs for direct JSON mapping:

```java
// NEW WAY - Direct ObjectMapper mapping ✅

// 1. Define clean DTOs with Jackson annotations
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChinaportsResponse {
    @JsonProperty("vessels")
    private List<ChinaportsVesselData> vessels;
}

@Data  
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChinaportsVesselData {
    @JsonProperty("mmsi")
    private String mmsi;
    
    @JsonProperty("lat")
    private Double lat;
    // ... all fields with proper Jackson annotations
}

// 2. Simple parsing with ObjectMapper
private List<VesselTrackingRequest> parseChinaportsResponse(String responseBody) {
    ChinaportsResponse response = objectMapper.readValue(responseBody, ChinaportsResponse.class);
    
    return response.getVessels().stream()
            .map(externalApiMapper::fromChinaports)
            .filter(Objects::nonNull)
            .toList();
}

// 3. Clean mapping with dedicated mapper
@Component
public class ExternalApiMapper {
    public VesselTrackingRequest fromChinaports(ChinaportsVesselData data) {
        return VesselTrackingRequest.builder()
                .mmsi(data.getMmsi())
                .latitude(data.getLat())
                // ... simple field mapping
                .build();
    }
}
```

## Benefits

### 1. **Massive Code Reduction**
- **Before**: 300+ lines per service with manual parsing
- **After**: ~50 lines total (DTO + mapper + parsing)
- **Reduction**: 80%+ less code

### 2. **Type Safety & Validation**
```java
// Jackson handles all type conversion automatically
// No more manual String→Double conversion
// @JsonProperty ensures field mapping consistency
```

### 3. **Better Error Handling**
```java
// Jackson provides detailed parsing errors
// ObjectMapper handles null values gracefully
// @JsonIgnoreProperties(ignoreUnknown = true) handles API changes
```

### 4. **Maintainability**
- Single source of truth for field mapping
- Easy to add new fields
- Consistent patterns across all services
- Testable DTOs

### 5. **Performance**
- Jackson's native parsing is optimized
- No manual type checking overhead
- Stream processing with lazy evaluation

## Implementation Pattern

### Step 1: Create External API DTOs
```java
// One response DTO per external API
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class [ApiName]Response {
    @JsonProperty("vessels") // or "flights", "data", etc.
    private List<[ApiName]VesselData> vessels;
}

// One data DTO per external API format  
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class [ApiName]VesselData {
    @JsonProperty("mmsi")
    private String mmsi;
    // ... map all expected fields
}
```

### Step 2: Create Unified Mapper
```java
@Component
public class ExternalApiMapper {
    public VesselTrackingRequest from[ApiName](ApiVesselData data) {
        // Simple field mapping with any necessary transformations
    }
}
```

### Step 3: Refactor Service Parsing
```java
// Replace manual JsonNode parsing with:
ApiResponse response = objectMapper.readValue(responseBody, ApiResponse.class);
return response.getVessels().stream()
        .map(externalApiMapper::fromApiName)
        .filter(Objects::nonNull)
        .toList();
```

## Services Ready for Refactoring

1. **✅ ChinaportsApiService** - Already refactored
2. **🔄 MarineTrafficV2ApiService** - DTOs created, needs service refactoring
3. **📋 VesselFinderApiService** - Needs DTOs + refactoring  
4. **📋 ExternalApiService** - Needs DTOs + refactoring
5. **📋 AdsbExchangeApiService** - Needs DTOs + refactoring (for flights)

## Testing Benefits
```java
// Easy to test with clean DTOs
@Test
void testChinaportsMapping() {
    ChinaportsVesselData testData = new ChinaportsVesselData();
    testData.setMmsi("123456789");
    testData.setLat(10.5);
    
    VesselTrackingRequest result = mapper.fromChinaports(testData);
    
    assertThat(result.getMmsi()).isEqualTo("123456789");
    assertThat(result.getLatitude()).isEqualTo(10.5);
}
```

## Conclusion
This refactoring provides a modern, maintainable, and efficient approach to handling external API responses. The pattern should be applied to all remaining external API services for consistency and improved code quality.
