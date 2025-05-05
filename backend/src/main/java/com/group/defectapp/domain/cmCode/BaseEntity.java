package com.group.defectapp.domain.cmCode;


import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @CreationTimestamp
    @Column(updatable = false, name = "first_reg_dtm", columnDefinition = "DATETIME")
    private LocalDateTime createdAt;


    @UpdateTimestamp
    @Column(insertable = false, name = "fnl_udt_dtm", columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

}
