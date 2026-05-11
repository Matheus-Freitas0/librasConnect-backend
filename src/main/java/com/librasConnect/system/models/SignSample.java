package com.librasConnect.system.models;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sign_sample")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignSample {

    @Id
    @Column(length = 80)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_id", nullable = false)
    private Sign sign;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    @Column(name = "frame_count", nullable = false)
    private int frameCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode frames;
}
