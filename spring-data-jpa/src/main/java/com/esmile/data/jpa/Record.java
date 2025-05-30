package com.esmile.data.jpa;

import jakarta.persistence.*;
import jakarta.persistence.metamodel.StaticMetamodel;
import lombok.Data;

import java.time.LocalDateTime;

@StaticMetamodel(Record.class)
@Entity
@Data
@Table(name = "records")
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String type;
    private String status;
    private LocalDateTime createdDate;

}
