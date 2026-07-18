package com.legacy.archaeology.domain.reviews;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    List<ReviewEntity> findAllByTargetTypeAndTargetIdOrderByReviewedAtDesc(
            String targetType, String targetId);
}
