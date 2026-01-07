package com.kingdom_bank.RFQBackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "ORDERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ORDER_ID", nullable = false, length = 100)
    private String orderId;

    @Column(name = "ACCOUNT_NUMBER", length = 50)
    private String accountNumber;

    @Column(name = "CUSTOMER_NAME", length = 255)
    private String customerName;

    @Column(name = "TELLER_CASH_ACCOUNT_NAME", length = 255)
    private String tellerCashAccountName;

    @Column(name = "CIF_ACCOUNT_CODE", length = 50)
    private String cifAccountCode;

    @Column(name = "COUNTER_NOMINAL_AMOUNT", precision = 18, scale = 2)
    private BigDecimal counterNominalAmount;

    @Column(name = "CURRENCY_PAIR", length = 10)
    private String currencyPair;

    @Column(name = "BUY_SELL", length = 10)
    private String buySell;

    @Lob
    @Column(name = "PURPOSE")
    private String purpose;

    @Column(name = "REQUEST_DATE")
    private Date requestDate;

    @Column(name = "VALUE_DATE")
    private String valueDate;

    @Lob
    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "EXPECTED_AMOUNT", precision = 18, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "DEALER_ID", length = 50)
    private String dealerId;

    @Column(name = "BRANCH_ID", length = 50)
    private String branchId;

    @Column(name = "NEGOTIATED_RATE", precision = 18, scale = 6)
    private BigDecimal negotiatedRate;

    @Column(name = "TREASURY_RATE", precision = 18, scale = 6)
    private BigDecimal treasuryRate;

    @Temporal(TemporalType.DATE)
    @Column(name = "VALID_UNTIL")
    private Date validUntil;

    @Column(name = "FROM_CURRENCY", length = 100)
    private String fromCurrency;

    @Column(name = "TO_CURRENCY", length = 100)
    private String toCurrency;


    @Column(name = "DEALER_CODE", length = 50)
    private String dealerCode;

    @Column(name = "TELLER_ID", length = 50)
    private String tellerId;

    /* =======================
       AUDIT & WORKFLOW FIELDS
       ======================= */

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

    /* =======================
       FOREIGN KEY
       ======================= */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "STATUS_ID",
        foreignKey = @ForeignKey(name = "FK_ORDER_STATUS")
    )
    private Status status;

    /* =======================
       AUTO TIMESTAMPS
       ======================= */

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
