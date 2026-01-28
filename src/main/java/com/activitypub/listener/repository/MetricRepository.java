package com.activitypub.listener.repository;

import com.activitypub.listener.model.Metric;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetricRepository extends MongoRepository<Metric, String> {
    Optional<Metric> findByName(String name);
    List<Metric> findByActiveTrue();
}
