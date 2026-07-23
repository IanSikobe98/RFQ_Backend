package com.kingdom_bank.RFQBackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "branches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "branchCode")
    private String branchCode;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "date_created")
    private Date dateCreated;

    @Column(name = "date_updated")
    private Date dateUpdated;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @ManyToOne
    @JoinColumn(name = "status_id", referencedColumnName = "status_id")
    private Status statusId;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        this.dateCreated = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateUpdated = new Date();
    }
}