package fu.ats.api.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private static final long serialVersionUID = 1L;
    private String userId;
    private String email;
    private String fullName;
    private  String password;
    private List<GrantedAuthority> grantedAuthorities;

    public CustomUserDetails(String userId, String email, String fullName, String password, List<GrantedAuthority> grantedAuthorities) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.password = password;
        this.grantedAuthorities = grantedAuthorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return grantedAuthorities;
    }

    @Override
    public String getPassword() {
        return password;
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
