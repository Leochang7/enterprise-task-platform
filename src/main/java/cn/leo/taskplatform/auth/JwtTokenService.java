package cn.leo.taskplatform.auth;

import cn.leo.taskplatform.enums.RoleCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class JwtTokenService {

    private final JwtProperties jwtProperties;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String createAccessToken(AccountLoginRecord record, String sessionId, String tokenVersion, Set<String> roleCodes) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getExpireSeconds());

        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(record.getUserId())
                .claim("accountName", record.getUsername())
                .claim("userName", resolveDisplayName(record))
                .claim("tenantId", record.getTenantId())
                .claim("tenantCode", record.getTenantCode())
                .claim("deptId", record.getDeptId())
                .claim("email", record.getEmail())
                .claim("avatarUrl", record.getAvatarUrl())
                .claim("sessionId", sessionId)
                .claim("tokenVersion", tokenVersion)
                .claim("roleCodes", roleCodes)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(getKey())
                .compact();
    }

    public AuthUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return AuthUser.builder()
                .userId(claims.getSubject())
                .accountName(claims.get("accountName", String.class))
                .userName(claims.get("userName", String.class))
                .tenantId(claims.get("tenantId", String.class))
                .tenantCode(claims.get("tenantCode", String.class))
                .deptId(claims.get("deptId", String.class))
                .email(claims.get("email", String.class))
                .avatarUrl(claims.get("avatarUrl", String.class))
                .sessionId(claims.get("sessionId", String.class))
                .tokenVersion(claims.get("tokenVersion", String.class))
                .roleCodes(extractRoles(claims.get("roleCodes", List.class)))
                .build();
    }

    public long getExpireSeconds() {
        return jwtProperties.getExpireSeconds();
    }

    private SecretKey getKey() {
        byte[] secretBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("jwt.secret 长度至少为 32 字节");
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }

    private Set<String> extractRoles(List<?> roleValues) {
        Set<String> roleCodes = new LinkedHashSet<>();
        if (roleValues == null) {
            return roleCodes;
        }
        for (Object roleValue : roleValues) {
            if (roleValue != null) {
                roleCodes.add(roleValue.toString().toUpperCase());
            }
        }
        return RoleCode.normalize(roleCodes);
    }

    private String resolveDisplayName(AccountLoginRecord record) {
        if (record.getRealName() != null && !record.getRealName().isBlank()) {
            return record.getRealName();
        }
        if (record.getNickname() != null && !record.getNickname().isBlank()) {
            return record.getNickname();
        }
        return record.getUsername();
    }
}
