package com.librasConnect.system.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.librasConnect.system.models.SignSample;

public interface SignSampleRepository extends JpaRepository<SignSample, String> {

    @EntityGraph(attributePaths = "sign")
    @Query("SELECT s FROM SignSample s")
    List<SignSample> findAllWithSign();

    List<SignSample> findBySign_IdOrderByCreatedAtAsc(String signId);

    long countBySign_Id(String signId);

    boolean existsBySign_IdAndId(String signId, String sampleId);

    long count();
}
