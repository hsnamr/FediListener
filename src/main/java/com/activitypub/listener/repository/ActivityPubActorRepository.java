package com.activitypub.listener.repository;

import com.activitypub.listener.model.ActivityPubActor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityPubActorRepository extends MongoRepository<ActivityPubActor, String> {
    Optional<ActivityPubActor> findByActorId(String actorId);
    List<ActivityPubActor> findByInstanceUrl(String instanceUrl);
    Optional<ActivityPubActor> findByUsernameAndInstanceUrl(String username, String instanceUrl);
}
