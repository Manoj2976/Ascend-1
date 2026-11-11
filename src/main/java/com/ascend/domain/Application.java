package com.ascend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** A company the user is tracking in the placement pipeline. */
@Entity
@Table(name = "applications", indexes = @Index(name = "idx_apps_user", columnList = "user_id"))
@Getter @Setter @NoArgsConstructor
public class Application {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 120)
    private String company;

    @Column(name = "job_role", length = 120)
    private String role = "";

    @Column(nullable = false, length = 20)
    private String status = "Wishlist";

    @Column(name = "applied_on")
    private LocalDate date;

    @Column(length = 1000)
    private String link = "";

    @Column(length = 2000)
    private String notes = "";
}
