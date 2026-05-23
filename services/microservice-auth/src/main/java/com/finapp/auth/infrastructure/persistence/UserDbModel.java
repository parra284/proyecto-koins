package com.finapp.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity — physical ORM model mapped to the {@code users} table
 * in the isolated {@code koins_auth_db} database.
 *
 * <p>
 * This class exists exclusively in the infrastructure layer and
 * carries all Hibernate/JPA annotations. The domain {@code User}
 * aggregate is never contaminated by ORM concerns.
 * </p>
 */
@Entity
@Table(name = "users")
public class UserDbModel {

   @Id
   @Column(name = "user_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
   private UUID userId;

   @Column(name = "email", nullable = false, unique = true, length = 254)
   private String email;

   @Column(name = "password_hash", nullable = false, length = 72)
   private String passwordHash;

   @Column(name = "role", nullable = false, length = 20)
   private String role;

   @Column(name = "failed_login_attempts", nullable = false)
   private int failedLoginAttempts;

   @Column(name = "lockout_expiration")
   private LocalDateTime lockoutExpiration;

   /*
    * ────────────────────────────────────────────────────────
    * JPA requires a no-arg constructor
    * ────────────────────────────────────────────────────────
    */

   public UserDbModel() {
   }

   /*
    * ────────────────────────────────────────────────────────
    * Accessors (read/write — required by JPA)
    * ────────────────────────────────────────────────────────
    */

   public UUID getUserId() {
      return userId;
   }

   public void setUserId(UUID userId) {
      this.userId = userId;
   }

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public String getPasswordHash() {
      return passwordHash;
   }

   public void setPasswordHash(String passwordHash) {
      this.passwordHash = passwordHash;
   }

   public String getRole() {
      return role;
   }

   public void setRole(String role) {
      this.role = role;
   }

   public int getFailedLoginAttempts() {
      return failedLoginAttempts;
   }

   public void setFailedLoginAttempts(int failedLoginAttempts) {
      this.failedLoginAttempts = failedLoginAttempts;
   }

   public LocalDateTime getLockoutExpiration() {
      return lockoutExpiration;
   }

   public void setLockoutExpiration(LocalDateTime lockoutExpiration) {
      this.lockoutExpiration = lockoutExpiration;
   }
}
