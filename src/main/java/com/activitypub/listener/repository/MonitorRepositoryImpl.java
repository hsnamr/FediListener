package com.activitypub.listener.repository;

import com.activitypub.listener.model.Monitor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
public class MonitorRepositoryImpl implements MonitorRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Monitor> findMonitors(
            String search,
            String monitorTypeId,
            Long productId,
            String dataSourceId,
            Monitor.ApprovalStatus isApproved,
            Boolean paused,
            Long userId,
            String sortBy,
            String orderBy,
            Pageable pageable) {

        Criteria criteria = Criteria.where("isDeleted").is(false);

        if (userId != null) {
            criteria.and("userId").is(userId);
        }
        if (search != null && !search.trim().isEmpty()) {
            criteria.and("name").regex(Pattern.quote(search.trim()), "i");
        }
        if (monitorTypeId != null && !monitorTypeId.isEmpty()) {
            criteria.and("monitorType.$id").is(monitorTypeId);
        }
        if (productId != null) {
            criteria.and("productId").is(productId);
        }
        if (dataSourceId != null && !dataSourceId.isEmpty()) {
            Criteria dsCriteria = new Criteria().orOperator(
                    Criteria.where("keywords.dataSource.$id").is(dataSourceId),
                    Criteria.where("accountAnalyses.dataSource.$id").is(dataSourceId),
                    Criteria.where("regionals.dataSource.$id").is(dataSourceId),
                    Criteria.where("managedAccounts.dataSource.$id").is(dataSourceId)
            );
            criteria.andOperator(dsCriteria);
        }
        if (isApproved != null) {
            criteria.and("isApproved").is(isApproved.name());
        }
        if (paused != null) {
            criteria.and("paused").is(paused);
        }

        Query query = new Query(criteria);

        String sortField = normalizeSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(orderBy) ? Sort.Direction.DESC : Sort.Direction.ASC;
        query.with(Sort.by(direction, sortField));

        long total = mongoTemplate.count(query, Monitor.class);
        query.with(pageable);
        List<Monitor> content = mongoTemplate.find(query, Monitor.class);

        return new PageImpl<>(content, pageable, total);
    }

    private static String normalizeSortField(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) return "createdAt";
        switch (sortBy.toLowerCase()) {
            case "id":
                return "_id";
            case "name":
                return "name";
            case "created_at":
            case "createdat":
                return "createdAt";
            case "updated_at":
            case "updatedat":
                return "updatedAt";
            case "monitor_type":
            case "monitortype":
                return "monitorType.name";
            case "status":
                return "isApproved";
            case "paused":
                return "paused";
            case "data_sources_count":
                return "keywords";
            default:
                return "createdAt";
        }
    }
}
