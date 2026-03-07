package com.manage_expense.services.services_template;

import com.manage_expense.entities.RefreshToken;
import com.manage_expense.entities.User;
import com.manage_expense.entities.UserSession;

public interface RefreshTokenService {
    RefreshToken generateRefreshToken(User user, UserSession userSession);
    RefreshToken findByToken(String refreshToken);
    boolean verifyRefreshToken(RefreshToken refreshToken);
}
