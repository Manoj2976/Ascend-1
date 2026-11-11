package com.ascend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "goals", indexes = @Index(name = "idx_goals_user", columnList = "user_id"))
@Getter @Setter @NoArgsConstructor
public class Goal {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 10)
    private GoalType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 40)
    private String unit = "";

    @Column(nullable = false)
    private int target = 1;

    /** Monthly / final targets: how much is done so far. */
    @Column(nullable = false)
    private int progress = 0;

    /** MONTHLY only, format yyyy-MM. */
    @Column(name = "goal_month", length = 7)
    private String month;

    /** FINAL only. */
    private LocalDate deadline;

    /** MONTHLY goals point at the FINAL goal they belong to. */
    @Column(name = "parent_id")
    private UUID parentId;

    /** DAILY only: how much was logged on each day. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "goal_logs", joinColumns = @JoinColumn(name = "goal_id"))
    @MapKeyColumn(name = "log_date")
    @Column(name = "amount")
    private Map<LocalDate, Integer> logs = new HashMap<>();
}
