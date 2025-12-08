package com.kingdom_bank.RFQBackend.entity;

import com.kingdom_bank.RFQBackend.dto.PermsDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;


@Entity
@Table(name = "ROLES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ROLE_ID")
    private Integer roleId;

    @Column(name = "ROLE_NAME", length = 50, nullable = false)
    private String roleName;

    @Column(name = "ROLE_DESCRIPTION", length = 255)
    private String roleDescription;

    @ManyToOne
    @JoinColumn(name = "STATUS_ID", referencedColumnName = "STATUS_ID")
    private Status status;

    @Column(name = "DATE_ADDED")
    private Date dateAdded;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "DATE_UPDATED")
    private Date dateUpdated;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    @Column(name = "DATE_APPROVED")
    private Date dateApproved;

    @Column(name = "APPROVED_BY", length = 100)
    private String approvedBy;

    @Transient
    private List<PermsDto> privilegeList;
}
