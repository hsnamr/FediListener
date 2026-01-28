package com.activitypub.listener.repository;

import com.activitypub.listener.model.DataSource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceRepository extends MongoRepository<DataSource, String> {
    Optional<DataSource> findBySource(String source);
    List<DataSource> findByActiveTrue();
}
