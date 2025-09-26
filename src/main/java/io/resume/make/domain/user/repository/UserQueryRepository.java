package io.resume.make.domain.user.repository;

import io.resume.make.domain.user.dto.UserProjectSummaryDto;

import java.util.Optional;
import java.util.UUID;

public interface UserQueryRepository {
    Optional<UserProjectSummaryDto> fetchProjectSummary(UUID userId);
}
