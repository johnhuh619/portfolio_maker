package io.resume.make.domain.projects.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "problem_solving", indexes = {
        @Index(name = "idx_project_id", columnList = "project_id")
})
public class ProblemSolving {

    @Id
    @GeneratedValue
    @Column(name = "ps_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(length = 300, nullable = false)
    private String title;

    @Column
    private String problem;

    @Column
    private String solution;

    @Column
    private String result;

    @Column(name = "order_index", nullable = false)
    private Integer orderIdx = 0;
}
