package com.librasConnect.system.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.librasConnect.system.models.Sign;

public interface SignRepository extends JpaRepository<Sign, String> {
}
