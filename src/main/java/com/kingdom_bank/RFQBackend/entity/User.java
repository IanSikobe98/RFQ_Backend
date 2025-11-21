package com.kingdom_bank.RFQBackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Entity
@Table(name = "USERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Integer userId;

    @Column(name = "USERNAME", length = 20, nullable = false)
    private String username;

    @Column(name = "PHONE", length = 25, nullable = false)
    private String phone;

    @Column(name = "EMAIL", length = 25, nullable = false)
    private String email;

    @ManyToOne
    @JoinColumn(name = "ROLE_ID", referencedColumnName = "ROLE_ID")
    private Role role;

    @Column(name = "DEPARTMENT_ID")
    private Integer departmentId;

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
}
