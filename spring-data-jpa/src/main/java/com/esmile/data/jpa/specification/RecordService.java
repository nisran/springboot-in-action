package com.esmile.data.jpa.specification;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordService {
    private final RecordRepository recordRepository;

    public Page<Record> searchRecords(RecordSearchDTO searchDTO, Pageable pageable) {
        return recordRepository.findAll(RecordSpecification.createSpecification(searchDTO), pageable);
    }
}