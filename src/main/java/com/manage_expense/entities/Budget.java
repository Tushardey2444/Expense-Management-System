package com.manage_expense.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manage_expense.enums.BudgetStatus;
import com.manage_expense.enums.Currency;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(
        name = "budgets",
        indexes = {
                @Index(name = "idx_budget_user", columnList = "user_id"),
                @Index(name = "idx_budget_category", columnList = "category_id")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int budgetId;

    @DecimalMin("0.0")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @DecimalMin("0.0")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountSpend = BigDecimal.ZERO;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(length = 200)
    private String notes;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BudgetStatus budgetStatus;

    @Column(nullable = false)
    private boolean isActive;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "budget",cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Items> items;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Transient
    public BigDecimal getRemainingAmount() {
        return amount.subtract(amountSpend);
    }
}
