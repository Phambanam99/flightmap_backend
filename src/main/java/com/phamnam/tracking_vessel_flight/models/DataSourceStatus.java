package com.phamnam.tracking_vessel_flight.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_source_status")
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DataSourceStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id", nullable = false)
    @JsonBackReference
    private DataSource dataSource;

    @Column(name = "check_time", nullable = false)
    private LocalDateTime checkTime;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "response_time")
    private Long responseTime; // in milliseconds

    @Column(name = "data_points_count")
    @Builder.Default
    private Integer dataPointsCount = 0;

    @Column(name = "error_message")
    @Lob
    private String errorMessage;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "bytes_received")
    @Builder.Default
    private Long bytesReceived = 0L;

    @Column(name = "duplicate_count")
    @Builder.Default
    private Integer duplicateCount = 0;

    @Column(name = "invalid_count")
    @Builder.Default
    private Integer invalidCount = 0;

    @Column(name = "processing_time")
    private Long processingTime; // Time to process the data in milliseconds

    public enum Status {
        SUCCESS,
        FAILURE,
        TIMEOUT,
        RATE_LIMITED,
        CIRCUIT_BREAKER_OPEN,
        MAINTENANCE
    }
}