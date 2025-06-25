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

    @Value("${raw.data.compression.enabled:false}")
    private boolean compressionEnabled;

    @Value("${raw.data.compression.algorithm:gzip}")
    private String compressionAlgorithm;

    @Value("${raw.data.compression.threshold-size:1024}")
    private int compressionThresholdBytes;

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
     * Compress JSON data if compression is enabled and data exceeds threshold
     */
    public String compressJsonData(String jsonData) {
        if (!compressionEnabled || jsonData == null || jsonData.isEmpty()) {
            return jsonData;
        }

        byte[] originalBytes = jsonData.getBytes(StandardCharsets.UTF_8);

        // Only compress if data size exceeds threshold
        if (originalBytes.length < compressionThresholdBytes) {
            log.trace("Data size {} bytes below threshold {}, skipping compression",
                    originalBytes.length, compressionThresholdBytes);
            return jsonData;
        }

        try {
            byte[] compressedBytes = compressBytes(originalBytes);
            String compressedData = Base64.getEncoder().encodeToString(compressedBytes);

            // Add compression metadata
            String result = "COMPRESSED:" + compressionAlgorithm + ":" + compressedData;

            // Update statistics
            totalCompressedRecords.incrementAndGet();
            totalOriginalSize.addAndGet(originalBytes.length);
            totalCompressedSize.addAndGet(compressedBytes.length);

            double compressionRatio = (double) compressedBytes.length / originalBytes.length;
            log.trace("Compressed {} bytes to {} bytes (ratio: {:.2f})",
                    originalBytes.length, compressedBytes.length, compressionRatio);

            return result;

        } catch (IOException e) {
            compressionErrors.incrementAndGet();
            log.error("Failed to compress data: {}", e.getMessage());
            return jsonData; // Return original data on compression failure
        }
    }

    /**
     * Decompress JSON data if it was compressed
     */
    public String decompressJsonData(String data) {
        if (data == null || !data.startsWith("COMPRESSED:")) {
            return data; // Not compressed data
        }

        try {
            String[] parts = data.split(":", 3);
            if (parts.length != 3) {
                log.warn("Invalid compressed data format: {}", data.substring(0, Math.min(data.length(), 50)));
                return data;
            }

            String algorithm = parts[1];
            String compressedData = parts[2];

            if (!algorithm.equals(compressionAlgorithm)) {
                log.warn("Unsupported compression algorithm: {}", algorithm);
                return data;
            }

            byte[] compressedBytes = Base64.getDecoder().decode(compressedData);
            byte[] decompressedBytes = decompressBytes(compressedBytes);

            return new String(decompressedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Failed to decompress data: {}", e.getMessage());
            return data; // Return original data on decompression failure
        }
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