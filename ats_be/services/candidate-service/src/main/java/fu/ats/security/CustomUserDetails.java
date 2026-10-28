package fu.ats.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final String userId;
    private final String email;
    private final String fullName;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(
            String userId,
            String email,
            String fullName,
            List<GrantedAuthority> authorities
    ) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email != null ? email : userId;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }
}
