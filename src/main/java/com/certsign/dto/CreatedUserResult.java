package com.certsign.dto;

import com.certsign.model.User;

public record CreatedUserResult(User user, String temporaryPassword) {
}
