package com.activitypub.listener.repository;

import com.activitypub.listener.model.Keyword;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KeywordRepository extends MongoRepository<Keyword, String> {
    List<Keyword> findByMonitorId(String monitorId);
    
    List<Keyword> findByMonitorIdAndIsActiveTrue(String monitorId);
}
