package com.esmile.data.jpa;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecordSearchDTO {
    private String name;
    private String type;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}