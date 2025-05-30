package com.esmile.data.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecordRepository extends JpaRepository<Record, Long>, JpaSpecificationExecutor<Record> {
}