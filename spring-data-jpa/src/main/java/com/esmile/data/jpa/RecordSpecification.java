package com.esmile.data.jpa;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;


public class RecordSpecification {
    public static Specification<Record> createSpecification(RecordSearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<>();

            if (StringUtils.hasText(searchDTO.getName())) {
                predicates.add(criteriaBuilder.like(root.get(Record_.name), "%" + searchDTO.getName() + "%"));
            }

            if (StringUtils.hasText(searchDTO.getType())) {
                predicates.add(criteriaBuilder.equal(root.get(Record_.type), searchDTO.getType()));
            }

            if (StringUtils.hasText(searchDTO.getStatus())) {
                predicates.add(criteriaBuilder.equal(root.get(Record_.status), searchDTO.getStatus()));
            }

            if (searchDTO.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Record_.createdDate), searchDTO.getStartDate()));
            }

            if (searchDTO.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(Record_.createdDate), searchDTO.getEndDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}