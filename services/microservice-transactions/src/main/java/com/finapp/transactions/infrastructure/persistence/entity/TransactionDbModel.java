package com.finapp.transactions.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity — physical ORM mapping for the {@code transactions} table in MariaDB.
 *
 * <p>This class is <em>strictly infrastructure</em>. It carries JPA/Hibernate
 * annotations that the domain layer must never depend on. The adapter layer
 * ({@link com.finapp.transactions.adapter.output.persistence.MariaDbTransactionRepository})
 * handles the bidirectional mapping between this model and the pure domain entity.</p>
 */
@Entity
@Table(name = "transactions")
public class TransactionDbModel {

    @Id
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(name = "description", length = 500)
    private String description;

    /* ── Constructors ─────────────────────────────────────── */

    public TransactionDbModel() {
        // Required by JPA
    }

    /* ── Accessors ────────────────────────────────────────── */

    public UUID getId()               { return id; }
    public void setId(UUID id)        { this.id = id; }

    public String getUserId()             { return userId; }
    public void setUserId(String userId)  { this.userId = userId; }

    public BigDecimal getAmount()              { return amount; }
    public void setAmount(BigDecimal amount)   { this.amount = amount; }

    public String getType()            { return type; }
    public void setType(String type)   { this.type = type; }

    public String getCategory()                { return category; }
    public void setCategory(String category)   { this.category = category; }

    public LocalDateTime getDate()                 { return date; }
    public void setDate(LocalDateTime date)        { this.date = date; }

    public String getDescription()                     { return description; }
    public void setDescription(String description)     { this.description = description; }
}
