package com.librasConnect.system.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sign")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sign {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 60)
    private String label;

    @Column(length = 280)
    private String description;

    @Column(name = "is_bimanual", nullable = false)
    private boolean bimanual;

    @Column(name = "is_static", nullable = false)
    private boolean staticForm;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "sign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SignSample> samples = new ArrayList<>();
}
