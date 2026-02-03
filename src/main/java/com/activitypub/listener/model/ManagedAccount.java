package com.activitypub.listener.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Managed account or page for a monitor. §3.7, §6.2.4.
 */
@Document(collection = "managed_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagedAccount {

    @Id
    private String id;

    private String monitorId;

    @DBRef
    private DataSource dataSource;

    private String accountName;

    private String accountId;

    @Builder.Default
    private Boolean shouldCollect = true;

    /** ACCOUNT or PAGE */
    private String type;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
