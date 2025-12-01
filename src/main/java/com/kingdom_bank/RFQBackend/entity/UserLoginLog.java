package com.kingdom_bank.RFQBackend.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "USER_LOGIN_LOGS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginLog {

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "USER_ID", referencedColumnName = "USER_ID")
    private User user;

    @ManyToOne
    @JoinColumn(name = "AUTH_STATUS", referencedColumnName = "STATUS_ID", nullable = false)
    private Status authStatus;

    @ManyToOne
    @JoinColumn(name = "OTP_AUTH_STATUS", referencedColumnName = "STATUS_ID")
    private Status otpAuthStatus;

    @Column(name = "OTP", length = 255)
    private String otp;

    @Column(name = "OTP_EXPIRY_TIME")
    private Date otpExpiryTime;

    @Column(name = "SESSION_ID", length = 255)
    private String sessionId;

    @Column(name = "SESSION_START_TIME")
    private Date sessionStartTime;

    @Column(name = "LOG_OUT_TIME")
    private Date logOutTime;

    @ManyToOne
    @JoinColumn(name = "STATUS", referencedColumnName = "STATUS_ID", nullable = false)
    private Status status;

    @Column(name = "CREATE_DATE", nullable = false)
    private Date createDate;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "DATE_UPDATED")
    private Date dateUpdated;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;
}
