package com.manage_expense.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String sessionId;

    @Column(nullable = false)
    private String deviceId;

    private String deviceName;

    private String ipAddress;

    @Column(nullable = false)
    private boolean revoked = false;

    private Instant createdAt;

    private Instant lastAccessedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User user;

    @OneToOne(mappedBy = "userSession", orphanRemoval = true)
    @JsonIgnore
    private RefreshToken refreshToken;
}
