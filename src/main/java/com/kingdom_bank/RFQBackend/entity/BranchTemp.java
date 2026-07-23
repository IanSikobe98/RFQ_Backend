package com.kingdom_bank.RFQBackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "branches_temp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchTemp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", length = 100)
    private String action;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "date_approved")
    private Date dateApproved;

    @Column(name = "comment", length = 100)
    private String comment;

    @Column(name = "entity_status")
    private Integer entityStatus;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "date_created")
    private Date dateCreated;

    @Column(name = "date_updated")
    private Date dateUpdated;

    @ManyToOne
    @JoinColumn(name = "created_by", referencedColumnName = "user_id")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "updated_by", referencedColumnName = "user_id")
    private User updatedBy;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private Status status;

    @Column(name = "branch_name")
    private String branchName;

    @ManyToOne
    @JoinColumn(name = "branch", referencedColumnName = "id")
    private Branch branch;

    @Transient
    private String entityStatusName;


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