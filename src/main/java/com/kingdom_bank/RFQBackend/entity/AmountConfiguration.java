package com.kingdom_bank.RFQBackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "amount_configuration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmountConfiguration {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "counter_nominal_amount")
    private BigDecimal counterNominalAmount;

}