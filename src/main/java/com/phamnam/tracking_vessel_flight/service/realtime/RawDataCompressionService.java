package com.phamnam.tracking_vessel_flight.service.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.models.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.RawVesselData;
import com.phamnam.tracking_vessel_flight.repository.RawAircraftDataRepository;
import com.phamnam.tracking_vessel_flight.repository.RawVesselDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class RawDataCompressionService {

    @Value("${raw.data.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${raw.data.compression.threshold-bytes:1024}")
    private int compressionThresholdBytes;

    @Value("${raw.data.compression.algorithm:gzip}")
    private String compressionAlgorithm;

    @Value("${raw.data.compression.auto-compress-after-hours:24}")
    private int autoCompressAfterHours;

    private final ObjectMapper objectMapper;
    private final RawAircraftDataRepository rawAircraftDataRepository;
    private final RawVesselDataRepository rawVesselDataRepository;

    // Statistics
    private final AtomicLong totalCompressedRecords = new AtomicLong(0);
    private final AtomicLong totalOriginalSize = new AtomicLong(0);
    private final AtomicLong totalCompressedSize = new AtomicLong(0);
    private final AtomicLong compressionErrors = new AtomicLong(0);

    /**
     * Compress JSON data if enabled and above threshold
     */
    public String compressJsonData(String jsonData) {
        if (!compressionEnabled || jsonData == null || jsonData.isEmpty()) {
            return jsonData;
        }

        // Only compress if data is above threshold
        if (jsonData.getBytes(StandardCharsets.UTF_8).length < compressionThresholdBytes) {
            return jsonData;
        }

        try {
            byte[] compressed = compressString(jsonData);
            String encoded = Base64.getEncoder().encodeToString(compressed);
            return "COMPRESSED:" + encoded;
        } catch (Exception e) {
            log.warn("Failed to compress JSON data: {}", e.getMessage());
            return jsonData; // Return original if compression fails
        }
    }

    /**
     * Decompress JSON data if it was compressed
     */
    public String decompressJsonData(String data) {
        if (data == null || !data.startsWith("COMPRESSED:")) {
            return data;
        }

        try {
            String encoded = data.substring("COMPRESSED:".length());
            byte[] compressed = Base64.getDecoder().decode(encoded);
            return decompressString(compressed);
        } catch (Exception e) {
            log.warn("Failed to decompress JSON data: {}", e.getMessage());
            return data; // Return original if decompression fails
        }
    }

    private byte[] compressString(String data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data.getBytes(StandardCharsets.UTF_8));
        }
        return bos.toByteArray();
    }

    private String decompressString(byte[] compressed) throws IOException {
        try (java.util.zip.GZIPInputStream gzipInputStream = new java.util.zip.GZIPInputStream(
                new java.io.ByteArrayInputStream(compressed))) {
            return new String(gzipInputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Check if compression is enabled
     */
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    /**
     * Get compression threshold
     */
    public int getCompressionThreshold() {
        return compressionThresholdBytes;
    }

    /**
     * Compress object to JSON and then compress the JSON
     */
    @Async("taskExecutor")
    public CompletableFuture<String> compressObject(Object object) {
        try {
            String jsonData = objectMapper.writeValueAsString(object);
            String compressedData = compressJsonData(jsonData);
            return CompletableFuture.completedFuture(compressedData);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object for compression: {}", e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Decompress data and convert back to object
     */
    @Async("taskExecutor")
    public <T> CompletableFuture<T> decompressToObject(String compressedData, Class<T> targetClass) {
        try {
            String jsonData = decompressJsonData(compressedData);
            if (jsonData == null) {
                return CompletableFuture.completedFuture(null);
            }

            T object = objectMapper.readValue(jsonData, targetClass);
            return CompletableFuture.completedFuture(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize decompressed data: {}", e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Compress bytes using the configured algorithm
     */
    private byte[] compressBytes(byte[] data) throws IOException {
        if ("gzip".equals(compressionAlgorithm)) {
            return compressWithGzip(data);
        } else {
            throw new IllegalArgumentException("Unsupported compression algorithm: " + compressionAlgorithm);
        }
    }

    /**
     * Decompress bytes using the configured algorithm
     */
    private byte[] decompressBytes(byte[] compressedData) throws IOException {
        if ("gzip".equals(compressionAlgorithm)) {
            return decompressWithGzip(compressedData);
        } else {
            throw new IllegalArgumentException("Unsupported compression algorithm: " + compressionAlgorithm);
        }
    }

    /**
     * GZIP compression
     */
    private byte[] compressWithGzip(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {

            gzipOut.write(data);
            gzipOut.finish();
            return baos.toByteArray();
        }
    }

    /**
     * GZIP decompression
     */
    private byte[] decompressWithGzip(byte[] compressedData) throws IOException {
        try (java.util.zip.GZIPInputStream gzipIn = new java.util.zip.GZIPInputStream(
                new java.io.ByteArrayInputStream(compressedData));
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Scheduled task to automatically compress old raw data
     * Runs every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours
    @Async("scheduledTaskExecutor")
    public void autoCompressOldData() {
        if (!compressionEnabled) {
            log.debug("Compression disabled, skipping auto-compression");
            return;
        }

        log.info("🗜️ Starting automatic compression of old raw data...");

        LocalDateTime compressionCutoff = LocalDateTime.now().minusHours(autoCompressAfterHours);

        // This would typically involve database operations to compress old records
        // For now, we'll just log the intent
        log.info("Would compress raw data older than {} (cutoff: {})",
                autoCompressAfterHours + " hours", compressionCutoff);

        // Implement actual database compression logic
        compressOldRawDataRecords(compressionCutoff);

        log.info("✅ Auto-compression check completed");
    }

    /**
     * Compress old raw data records in database
     */
    @Transactional
    private void compressOldRawDataRecords(LocalDateTime cutoff) {
        try {
            // Find uncompressed aircraft data older than cutoff
            List<RawAircraftData> oldAircraftData = rawAircraftDataRepository
                    .findByReceivedAtBefore(cutoff);

            // Find uncompressed vessel data older than cutoff
            List<RawVesselData> oldVesselData = rawVesselDataRepository
                    .findByReceivedAtBefore(cutoff);

            int aircraftCompressed = compressAircraftDataBatch(oldAircraftData);
            int vesselCompressed = compressVesselDataBatch(oldVesselData);

            log.info("📦 Compressed {} aircraft and {} vessel records older than {}",
                    aircraftCompressed, vesselCompressed, cutoff);

        } catch (Exception e) {
            log.error("❌ Failed to compress old raw data: {}", e.getMessage(), e);
            compressionErrors.incrementAndGet();
        }
    }

    /**
     * Compress aircraft data in batch
     */
    private int compressAircraftDataBatch(List<RawAircraftData> dataList) {
        int compressedCount = 0;

        for (RawAircraftData data : dataList) {
            try {
                if (data.getRawJson() != null && !data.getRawJson().startsWith("COMPRESSED:")) {
                    String originalJson = data.getRawJson();
                    int originalSize = originalJson.getBytes(StandardCharsets.UTF_8).length;

                    if (originalSize >= compressionThresholdBytes) {
                        String compressedJson = compressJsonData(originalJson);
                        data.setRawJson(compressedJson);

                        rawAircraftDataRepository.save(data);

                        int compressedSize = compressedJson.getBytes(StandardCharsets.UTF_8).length;
                        updateCompressionStats(originalSize, compressedSize);
                        compressedCount++;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to compress aircraft data {}: {}", data.getId(), e.getMessage());
                compressionErrors.incrementAndGet();
            }
        }

        return compressedCount;
    }

    /**
     * Compress vessel data in batch
     */
    private int compressVesselDataBatch(List<RawVesselData> dataList) {
        int compressedCount = 0;

        for (RawVesselData data : dataList) {
            try {
                if (data.getRawJson() != null && !data.getRawJson().startsWith("COMPRESSED:")) {
                    String originalJson = data.getRawJson();
                    int originalSize = originalJson.getBytes(StandardCharsets.UTF_8).length;

                    if (originalSize >= compressionThresholdBytes) {
                        String compressedJson = compressJsonData(originalJson);
                        data.setRawJson(compressedJson);

                        rawVesselDataRepository.save(data);

                        int compressedSize = compressedJson.getBytes(StandardCharsets.UTF_8).length;
                        updateCompressionStats(originalSize, compressedSize);
                        compressedCount++;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to compress vessel data {}: {}", data.getId(), e.getMessage());
                compressionErrors.incrementAndGet();
            }
        }

        return compressedCount;
    }

    /**
     * Update compression statistics
     */
    private void updateCompressionStats(int originalSize, int compressedSize) {
        totalCompressedRecords.incrementAndGet();
        totalOriginalSize.addAndGet(originalSize);
        totalCompressedSize.addAndGet(compressedSize);
    }

    /**
     * Get compression statistics
     */
    public Map<String, Object> getCompressionStats() {
        double compressionRatio = totalOriginalSize.get() > 0
                ? (double) totalCompressedSize.get() / totalOriginalSize.get()
                : 0.0;

        double spaceSavings = totalOriginalSize.get() > 0 ? 1.0 - compressionRatio : 0.0;

        return Map.of(
                "compressionEnabled", compressionEnabled,
                "algorithm", compressionAlgorithm,
                "thresholdBytes", compressionThresholdBytes,
                "autoCompressAfterHours", autoCompressAfterHours,
                "totalCompressedRecords", totalCompressedRecords.get(),
                "totalOriginalSizeBytes", totalOriginalSize.get(),
                "totalCompressedSizeBytes", totalCompressedSize.get(),
                "compressionRatio", compressionRatio,
                "spaceSavingsPercent", spaceSavings * 100,
                "compressionErrors", compressionErrors.get());
    }

    /**
     * Reset compression statistics
     */
    public void resetStats() {
        totalCompressedRecords.set(0);
        totalOriginalSize.set(0);
        totalCompressedSize.set(0);
        compressionErrors.set(0);
        log.info("Compression statistics reset");
    }

    /**
     * Test compression with sample data
     */
    public Map<String, Object> testCompression(String sampleData) {
        long startTime = System.currentTimeMillis();

        String compressedData = compressJsonData(sampleData);
        String decompressedData = decompressJsonData(compressedData);

        long compressionTime = System.currentTimeMillis() - startTime;

        boolean dataIntegrity = sampleData.equals(decompressedData);

        int originalSize = sampleData.getBytes(StandardCharsets.UTF_8).length;
        int compressedSize = compressedData.getBytes(StandardCharsets.UTF_8).length;
        double ratio = (double) compressedSize / originalSize;

        return Map.of(
                "originalSize", originalSize,
                "compressedSize", compressedSize,
                "compressionRatio", ratio,
                "spaceSavings", (1.0 - ratio) * 100,
                "compressionTimeMs", compressionTime,
                "dataIntegrity", dataIntegrity,
                "algorithm", compressionAlgorithm);
    }
}