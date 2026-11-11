package com.ascend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_user", columnList = "user_id"),
        @Index(name = "idx_tasks_user_date", columnList = "user_id, task_date")})
@Getter @Setter @NoArgsConstructor
public class Task {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String title;

    /** First day this task occurs. */
    @Column(name = "task_date", nullable = false)
    private LocalDate date;

    /** Optional time of day. Reminders only run for tasks that have one. */
    @Column(name = "due_time")
    private LocalTime time;

    @Column(length = 40)
    private String category = "Personal";

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_rule", nullable = false, length = 10)
    private Repeat repeat = Repeat.NONE;

    @Column(name = "goal_id")
    private UUID goalId;

    @Column(length = 2000)
    private String notes = "";

    /** Days on which this task (or one of its repeats) was ticked. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "task_done_dates", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "done_date")
    private Set<LocalDate> doneDates = new HashSet<>();
}
