package io.resume.make.domain.user.repository;

import io.resume.make.domain.user.dto.UserProjectSummaryDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserQueryRepositoryImpl implements UserQueryRepository {
    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<UserProjectSummaryDto> fetchProjectSummary(UUID userId) {
        return em.createQuery("""
                            select new io.resume.make.domain.user.dto.UserProjectSummaryDto(
                                u.id,
                                u.name,
                                p.id,
                                p.projectName,
                                ps.id,
                                ps.title,
                                ps.orderIdx
                            )
                            from User u
                            left join Project p on p.user = u.id
                            left join ProblemSolving ps on ps.projectId = p.id
                            where u.id = :userId
                            order by ps.orderIdx
                        """, UserProjectSummaryDto.class)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .findFirst();
    }
}
