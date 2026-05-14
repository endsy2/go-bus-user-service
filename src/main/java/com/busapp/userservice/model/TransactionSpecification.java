package com.busapp.userservice.model;

import com.busapp.userservice.dto.request.TransactionFilterRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {

    public static Specification<WalletTransaction> filterBy(TransactionFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getWalletId() != null) {
                predicates.add(cb.equal(root.get("wallet").get("id"), filter.getWalletId()));
            }

            if (filter.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getReferenceId() != null && !filter.getReferenceId().isBlank()) {
                predicates.add(cb.equal(root.get("referenceId"), filter.getReferenceId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
