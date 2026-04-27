package com.kingdom_bank.RFQBackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Time;
import java.time.LocalTime;
import java.util.Date;


@Entity
@Table(name = "availability_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day", nullable = false, length = 50)
    private String day;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "date_added")
    private Date dateAdded;

    @Column(name = "date_updated")
    private Date dateUpdated;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // Foreign Key Mapping
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", referencedColumnName = "status_id")
    private Status status;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        this.dateAdded = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateUpdated = new Date();
    }
}