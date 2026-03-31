package cn.leo.taskplatform.auth;

import cn.leo.taskplatform.mapper.AuthQueryMapper;
import cn.leo.taskplatform.redis.RedisKeyConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthPermissionService {

    private static final Duration PERMISSION_CACHE_TTL = Duration.ofHours(12);

    private final StringRedisTemplate stringRedisTemplate;
    private final AuthQueryMapper authQueryMapper;
    private final ObjectMapper objectMapper;

    public Set<String> loadPermissionCodes(AuthUser authUser) {
        return loadPermissionCodes(authUser.getTenantId(), authUser.getUserId());
    }

    public Set<String> loadPermissionCodes(String tenantId, String userId) {
        String key = buildPermissionKey(tenantId, userId);
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null && !cached.isBlank()) {
            return deserialize(cached);
        }

        Set<String> permissionCodes = new LinkedHashSet<>(authQueryMapper.findPermissionCodes(tenantId, userId));
        cachePermissionCodes(tenantId, userId, permissionCodes);
        return permissionCodes;
    }

    public void cachePermissionCodes(String tenantId, String userId, Set<String> permissionCodes) {
        String key = buildPermissionKey(tenantId, userId);
        stringRedisTemplate.opsForValue().set(key, serialize(permissionCodes), PERMISSION_CACHE_TTL);
    }

    private String buildPermissionKey(String tenantId, String userId) {
        return RedisKeyConstants.AUTH_PERMISSION + tenantId + ":" + userId;
    }

    private String serialize(Set<String> permissionCodes) {
        try {
            return objectMapper.writeValueAsString(permissionCodes);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("权限缓存序列化失败", ex);
        }
    }

    private Set<String> deserialize(String value) {
        try {
            List<String> permissions = objectMapper.readValue(value, new TypeReference<>() {
            });
            return new LinkedHashSet<>(permissions);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("权限缓存反序列化失败", ex);
        }
    }
}
