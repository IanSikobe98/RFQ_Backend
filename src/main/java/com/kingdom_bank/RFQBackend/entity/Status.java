package com.kingdom_bank.RFQBackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "STATUS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STATUS_ID")
    private Integer statusId;

    @Column(name = "STATUS_NAME", length = 50, nullable = false)
    private String statusName;

    @Column(name = "DESCRIPTION", length = 250)
    private String description;
}
