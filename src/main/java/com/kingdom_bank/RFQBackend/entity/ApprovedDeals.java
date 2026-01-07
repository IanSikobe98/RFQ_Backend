package com.kingdom_bank.RFQBackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "approved_deals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovedDeals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne
    @JoinColumn(
            name = "STATUS_ID",
            foreignKey = @ForeignKey(name = "FK_ORDER_STATUS")
    )
    private Status status;

    @ManyToOne
    @JoinColumn(name = "ORDER_ID")
    private Order order;

    @Column(name = "ORDER_CODE", length = 50)
    private String orderCode;

    @ManyToOne
    @JoinColumn(
            name = "ORDER_STATUS"
    )
    private Status orderStatus;

    @Column(name = "BOUGHT_CURRENCY", length = 10)
    private String boughtCurrency;

    @Column(name = "SOLD_CURRENCY", length = 10)
    private String soldCurrency;

    @Column(name = "SOLD_AMOUNT", precision = 20, scale = 4)
    private BigDecimal soldAmount;

    @Column(name = "`COMMENT`", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "BOUGHT_AMOUNT", precision = 20, scale = 4)
    private BigDecimal boughtAmount;

    @Column(name = "EXCHANGE_RATE", precision = 20, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "TREASURY_RATE", precision = 20, scale = 6)
    private BigDecimal treasuryRate;

    @Column(name = "DEALER_CODE", length = 50)
    private String dealerCode;


    @Column(name = "CIF_ACCOUNT_CODE", length = 50)
    private String cifAccountCode;

    @Column(name = "EXECUTED_AMOUNT", precision = 20, scale = 4)
    private BigDecimal executedAmount;

    @Column(name = "VALUE_DATE")
    private String valueDate;

    @Column(name = "ACCOUNT_NUMBER", length = 50)
    private String accountNumber;

    @Column(name = "DATE_ADDED", updatable = false)
    private LocalDateTime dateAdded;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;
}
