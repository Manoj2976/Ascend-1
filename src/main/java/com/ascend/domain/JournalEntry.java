package com.ascend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "journal_entries", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "entry_date"}))
@Getter @Setter @NoArgsConstructor
public class JournalEntry {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate date;

    /** 0..4 index into the mood emoji row. */
    private Integer mood;

    @Column(name = "entry_text", length = 10000)
    private String text = "";

    @Column(length = 5000)
    private String learned = "";

    @Column(name = "plan_text", length = 5000)
    private String plan = "";
}
