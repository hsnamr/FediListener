package com.activitypub.listener.repository;

import com.activitypub.listener.model.FediverseInstance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FediverseInstanceRepository extends MongoRepository<FediverseInstance, String> {
    Optional<FediverseInstance> findByInstanceUrl(String instanceUrl);
    List<FediverseInstance> findByIsActiveTrue();
    List<FediverseInstance> findByInstanceType(String instanceType);
}
