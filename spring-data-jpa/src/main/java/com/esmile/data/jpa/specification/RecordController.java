package com.esmile.data.jpa.specification;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {
    private final RecordService recordService;

    @GetMapping("/search")
    public Page<Record> searchRecords(RecordSearchDTO searchDTO, Pageable pageable) {
        return recordService.searchRecords(searchDTO, pageable);
    }
}