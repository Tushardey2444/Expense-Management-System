package com.manage_expense.entities;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manage_expense.enums.Providers;
import com.manage_expense.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Column(nullable = false)
    private boolean isPasswordUpdated;

    private Date createdAt;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private int failedAttempts;

    @Column(nullable = false)
    private Integer tokenVersion;

    @Column(nullable = false)
    private boolean accountLocked;

    private Date lockTime;

    @Version
    public long version;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private Providers provider; // LOCAL, GOOGLE

    private String providerUserId; // Google 'sub'

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "users_user_id"),
            inverseJoinColumns = @JoinColumn(name = "roles_role_id")
    )
    private Set<Role> roles;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile userProfile;

    @OneToMany(mappedBy = "user",
            orphanRemoval = true)
    @JsonIgnore
    private Set<RefreshToken> refreshTokens;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Budget> budgets;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.getRoleName())).collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.status != Status.EXPIRED && this.status != Status.PENDING && this.status != Status.DELETED;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isActive;
    }
}

/*
Even though in your entity you wrote:

@OneToMany(mappedBy = "user", orphanRemoval = true)
private Set<RefreshToken> refreshTokens = new HashSet<>();

When you use:
@Builder

Lombok does NOT initialize default values.
Builder bypasses field initialization.

So when a User is created using:
User.builder()....
refreshTokens becomes NULL.
 */
