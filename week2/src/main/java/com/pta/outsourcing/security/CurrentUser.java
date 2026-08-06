package com.pta.outsourcing.security;

import java.util.Set;

public record CurrentUser(
        Long id,
        String username,
        Set<String> roles,
        Set<String> permissions
) {
}
