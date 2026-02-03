package com.activitypub.listener.repository;

import com.activitypub.listener.model.CollectedActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectedActivityRepository extends MongoRepository<CollectedActivity, String> {

    Optional<CollectedActivity> findByActivityId(String activityId);

    Page<CollectedActivity> findByActorIdOrderByPublishedAtDesc(String actorId, Pageable pageable);

    List<CollectedActivity> findByActorIdAndPublishedAtAfterOrderByPublishedAtDesc(
            String actorId, LocalDateTime since, Pageable pageable);

    Page<CollectedActivity> findByMonitorIdOrderByPublishedAtDesc(String monitorId, Pageable pageable);

    boolean existsByActivityId(String activityId);
}
