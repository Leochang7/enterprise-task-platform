package cn.leo.taskplatform.enums;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum RoleCode {

    EMPLOYEE,
    DEPT_MANAGER,
    TENANT_ADMIN,
    PLATFORM_ADMIN;

    public static boolean isBuiltIn(String code) {
        return Arrays.stream(values()).anyMatch(roleCode -> roleCode.name().equalsIgnoreCase(code));
    }

    public static Set<String> normalize(Collection<String> roleCodes) {
        if (roleCodes == null) {
            return Set.of();
        }
        return roleCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
