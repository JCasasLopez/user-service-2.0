package dev.jcasaslopez.user.enums;

public enum RedisKeyPrefix {
    LOGIN_ATTEMPTS("login_attempts"),
    BLACKLIST("blacklist"),
    REFRESH_TOKEN("refresh_token");

    private final String prefix;

    RedisKeyPrefix(String prefix) {
        this.prefix = prefix;
    }

    // String redisKey = RedisKeyPrefix.LOGIN_ATTEMPTS.of(username); -> login_attempts:username1
    public String of(String keyPart) {
        return prefix + ":" + keyPart;
    }

    // String prefix = RedisKeyPrefix.LOGIN_ATTEMPTS.prefix(); -> login_attempts
    public String prefix() {
        return prefix;
    }
}