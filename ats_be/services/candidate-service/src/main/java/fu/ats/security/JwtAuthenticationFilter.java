package fu.ats.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID = "X-User-Id";
    private static final String USER_EMAIL = "X-User-Email";
    private static final String USER_NAME = "X-User-Name";
    private static final String USER_ROLES = "X-User-Roles";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String userId = request.getHeader(USER_ID);

        if (StringUtils.hasText(userId)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<GrantedAuthority> authorities = parseAuthorities(
                    request.getHeader(USER_ROLES)
            );

            CustomUserDetails userDetails = new CustomUserDetails(
                    userId,
                    request.getHeader(USER_EMAIL),
                    request.getHeader(USER_NAME),
                    authorities
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            authorities
                    );
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> parseAuthorities(String rawRoles) {
        if (!StringUtils.hasText(rawRoles)) {
            return List.of();
        }

        return Arrays.stream(rawRoles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(role -> role.startsWith("ROLE_")
                        ? role.substring("ROLE_".length())
                        : role)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
