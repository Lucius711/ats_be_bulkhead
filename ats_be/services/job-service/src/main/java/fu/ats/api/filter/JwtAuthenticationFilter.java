package fu.ats.api.filter;

import fu.ats.api.dto.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        String email = request.getHeader("X-User-Email");
        String userName = request.getHeader("X-User-Name");
        String rawRoles = request.getHeader("X-User-Roles");

        if (!StringUtils.hasText(userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        String roles = StringUtils.hasText(rawRoles)
                ? rawRoles.toUpperCase(Locale.ROOT)
                : "";

        List<GrantedAuthority> grantedAuthorities = Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map((roleName) -> {
            return new SimpleGrantedAuthority("ROLE_" + roleName);
        }).collect(Collectors.toList());

        log.info("Email {}", email);
        log.info("UserName {}", userName);
        log.info("Roles {}", roles);

        CustomUserDetails userDetails = new CustomUserDetails(userId, email, userName, null, grantedAuthorities);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // Store to context
        SecurityContextHolder.getContext().setAuthentication(authentication);


        filterChain.doFilter(request, response);

    }
}
