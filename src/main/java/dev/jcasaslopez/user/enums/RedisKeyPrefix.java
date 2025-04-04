package dev.jcasaslopez.user.enums;

public enum RedisKeyPrefix {
    LOGIN_ATTEMPTS("login_attempts"),
    WHITELIST("whitelist"),
    BLACKLIST("blacklist");

    private final String prefix;

    RedisKeyPrefix(String prefix) {
        this.prefix = prefix;
    }

    // String redisKey = RedisKeyPrefix.LOGIN_ATTEMPTS.of(username);
    public String of(String keyPart) {
        return prefix + ":" + keyPart;
    }

    // String prefix = RedisKeyPrefix.WHITELIST.prefix();
    public String prefix() {
        return prefix;
    }
}