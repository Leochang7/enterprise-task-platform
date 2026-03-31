package cn.leo.taskplatform.auth;

import cn.leo.taskplatform.exception.AuthException;

public final class UserContextHolder {

    private static final ThreadLocal<AuthUser> USER_CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(AuthUser authUser) {
        USER_CONTEXT.set(authUser);
    }

    public static AuthUser get() {
        return USER_CONTEXT.get();
    }

    public static AuthUser getRequiredUser() {
        AuthUser authUser = get();
        if (authUser == null) {
            throw new AuthException("AUTH_401", "未登录或登录已失效");
        }
        return authUser;
    }

    public static void clear() {
        USER_CONTEXT.remove();
    }
}
