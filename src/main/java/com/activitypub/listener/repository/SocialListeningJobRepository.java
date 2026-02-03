package com.activitypub.listener.repository;

import com.activitypub.listener.model.SocialListeningJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialListeningJobRepository extends MongoRepository<SocialListeningJob, String> {
    Optional<SocialListeningJob> findByIdAndMonitorId(String id, String monitorId);
}
