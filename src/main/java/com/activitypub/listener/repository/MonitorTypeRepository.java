package com.activitypub.listener.repository;

import com.activitypub.listener.model.MonitorType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonitorTypeRepository extends MongoRepository<MonitorType, String> {
    Optional<MonitorType> findByName(String name);
}
