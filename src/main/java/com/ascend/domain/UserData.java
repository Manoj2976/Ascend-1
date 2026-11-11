package com.ascend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Per-user data that does not need its own tables: the time table and the diet log are stored as JSON
 * exactly as the React app keeps them. Also holds the sync revision, which goes up on every full save.
 */
@Entity
@Table(name = "user_data")
@Getter @Setter @NoArgsConstructor
public class UserData {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "timetable_json", columnDefinition = "text")
    private String timetableJson;

    @Column(name = "diet_json", columnDefinition = "text")
    private String dietJson;

    @Column(nullable = false)
    private long revision = 0;

    public UserData(UUID userId) {
        this.userId = userId;
    }
}
