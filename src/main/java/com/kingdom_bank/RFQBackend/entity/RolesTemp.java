package com.kingdom_bank.RFQBackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Entity
@Table(name = "roles_temp")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolesTemp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "ROLE_NAME", length = 50)
    private String roleName;

    @Column(name = "ROLE_DESCRIPTION", length = 255)
    private String roleDescription;

    @Column(name = "PERMISSIONS",length = 255)
    private String permissions;

    @Column(name = "ENTITY_STATUS")
    private Integer entityStatus;

    @Column(name = "DATE_ADDED", columnDefinition = "datetime")
    private Date dateAdded;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "DATE_UPDATED", columnDefinition = "datetime")
    private Date dateUpdated;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    @Column(name = "DATE_APPROVED", columnDefinition = "datetime")
    private Date dateApproved;

    @Column(name = "APPROVED_BY", length = 100)
    private String approvedBy;

    @Column(name = "ACTION", length = 100)
    private String action;

    @Column(name = "COMMENT", length = 100)
    private String comment;

    @ManyToOne
    @JoinColumn(name = "STATUS_ID", referencedColumnName = "STATUS_ID")
    private Status status;

    @ManyToOne
    @JoinColumn(name = "ROLE_ID", referencedColumnName = "ROLE_ID")
    private Role role;


    @Transient
    private String entityStatusName;
}
