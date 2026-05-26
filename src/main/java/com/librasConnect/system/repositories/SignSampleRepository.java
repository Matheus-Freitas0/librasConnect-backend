package com.librasConnect.system.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.librasConnect.system.models.SignSample;

public interface SignSampleRepository extends JpaRepository<SignSample, String> {

    @EntityGraph(attributePaths = "sign")
    @Query("SELECT s FROM SignSample s")
    List<SignSample> findAllWithSign();

    @EntityGraph(attributePaths = "sign")
    Optional<SignSample> findWithSignById(String id);

    List<SignSample> findBySign_IdOrderByCreatedAtAsc(String signId);

    long countBySign_Id(String signId);

    @Query("SELECT s.twoHandFrameRatio FROM SignSample s WHERE s.sign.id = :signId ORDER BY s.createdAt ASC")
    List<Double> findTwoHandFrameRatiosBySign_Id(String signId);

    boolean existsBySign_IdAndId(String signId, String sampleId);

    long count();
}
