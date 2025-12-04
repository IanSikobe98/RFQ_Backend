package com.kingdom_bank.RFQBackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;


@Entity
@Table(name = "users_temp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsersTemp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "USER_ID", referencedColumnName = "USER_ID")
    private User user;

    @Column(name = "USERNAME", nullable = false, length = 20)
    private String username;

    @Column(name = "PHONE", nullable = false, length = 25)
    private String phone;

    @Column(name = "EMAIL", nullable = false, length = 25)
    private String email;

    @ManyToOne
    @JoinColumn(name = "ROLE_ID", referencedColumnName = "ROLE_ID")
    private Role role;

    @Column(name = "ENTITY_STATUS")
    private Integer entityStatus;

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

    @Column(name = "ACTION", length = 100)
    private String action;

    @Column(name = "COMMENT", length = 100)
    private String comment;
}
