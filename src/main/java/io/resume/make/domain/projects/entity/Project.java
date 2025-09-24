package io.resume.make.domain.projects.entity;

import io.resume.make.domain.projects.entity.vo.TeamInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "projects", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id")
})
public class Project {

    @Id
    @GeneratedValue
    @Column(name = "project_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID user;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Convert(converter = JsonConverter.class)
    @Column(name = "tech_stack", columnDefinition = "JSON")
    private List<String> techStack;

    @Convert(converter = TeamInfoConverter.class)
    @Column(name = "team_info", columnDefinition = "JSON")
    private TeamInfo teamInfo;

    @Column(name = "my_role")
    private String myRole;

    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

}
