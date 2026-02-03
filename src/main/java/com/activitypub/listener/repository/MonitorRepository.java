package com.activitypub.listener.repository;

import com.activitypub.listener.model.Monitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitorRepository extends MongoRepository<Monitor, String>, MonitorRepositoryCustom {
    
    @Query("{ 'isDeleted': false }")
    Page<Monitor> findByNotDeleted(Pageable pageable);
    
    @Query("{ 'isDeleted': false, 'name': { $regex: ?0, $options: 'i' } }")
    Page<Monitor> findByNotDeletedAndNameContaining(String search, Pageable pageable);
    
    Optional<Monitor> findByIdAndIsDeletedFalse(String id);
    
    List<Monitor> findByIsDeletedFalseAndPausedFalseAndIsApproved(
        Monitor.ApprovalStatus isApproved
    );
}
