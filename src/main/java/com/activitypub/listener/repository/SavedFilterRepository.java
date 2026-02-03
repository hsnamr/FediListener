package com.activitypub.listener.repository;

import com.activitypub.listener.model.SavedFilter;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedFilterRepository extends MongoRepository<SavedFilter, String> {
    List<SavedFilter> findByMonitorIdOrderByCreatedAtDesc(String monitorId);
    long countByMonitorId(String monitorId);
}
