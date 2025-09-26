package io.resume.make.domain.user.dto;

import java.util.UUID;

public record UserProjectSummaryDto(
        UUID userId,
        String userName,
        UUID projectId,
        String projectName,
        UUID problemSolvingId,
        String problemSolvingTitle,
        Integer problemSolvingOrder
) { }