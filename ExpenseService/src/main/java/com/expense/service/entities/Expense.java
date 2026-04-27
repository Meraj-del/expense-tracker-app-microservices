package com.expense.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expense")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Expense {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "amount")
    private Double  amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "merchant")
    private String merchant;

    @Column(name = "created_at",updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @PrePersist
    private void prePresist() {

        Timestamp now = Timestamp.from(Instant.now());

        if (this.externalId == null) {
            this.externalId = UUID.randomUUID().toString();
        }

        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    private void generateUpdatedAt() {
        this.updatedAt = Timestamp.from(Instant.now());
    }

}
