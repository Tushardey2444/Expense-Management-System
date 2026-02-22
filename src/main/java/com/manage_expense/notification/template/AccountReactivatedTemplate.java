package com.manage_expense.notification.template;

import com.manage_expense.helper.Helper;

public class AccountReactivatedTemplate {
    private static final String TEMPLATE = """
            <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <title>Account Reactivated</title>
                </head>
                <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
                  <table width="100%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff; margin:40px 0; border-radius:8px; overflow:hidden;">
                          <!-- Header -->
                          <tr>
                            <td style="background:#15803d; padding:20px; color:#ffffff; text-align:center;">
                              <h1 style="margin:0; font-size:22px;">Account Reactivated Successfully</h1>
                            </td>
                          </tr>
                          <!-- Content -->
                          <tr>
                            <td style="padding:30px; color:#333;">
                              <p style="font-size:16px;">
                                Hello <strong>{{userName}}</strong>,
                              </p>
                              <p style="font-size:14px; line-height:1.6;">
                                Great news! Your <strong>Expense Management</strong> account has been successfully
                                reactivated within the allowed 30-day recovery period.
                              </p>
                              <div style="margin:25px 0; padding:15px; background:#f0fdf4; border-left:4px solid #15803d;">
                                <p style="margin:0; font-size:14px; color:#14532d;">
                                  ✔ Your account data has been fully restored and is ready to use.
                                </p>
                              </div>
                              <p style="font-size:14px; color:#555;">
                                You can now log in and continue managing your expenses just like before.
                              </p>
                              <div style="margin:30px 0; text-align:center;">
                                <a href="{{LOGIN_URL}}" style="
                                  display:inline-block;
                                  background:#2563eb;
                                  color:#ffffff;
                                  text-decoration:none;
                                  padding:12px 24px;
                                  border-radius:6px;
                                  font-size:14px;
                                  font-weight:bold;">
                                  Login to Your Account
                                </a>
                              </div>
                              <p style="font-size:14px; color:#555;">
                                If you did not request this reactivation or notice any unusual activity.
                              </p>
                              <p style="margin-top:30px; font-size:14px;">
                                Welcome back,<br/>
                                <strong>Expense Management Team</strong>
                              </p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="background:#f8fafc; padding:15px; text-align:center; font-size:12px; color:#777;">
                              © 2026 Expense Management. All rights reserved.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
        """;

    public static String build(String userName, String LOGIN_URL) {
        userName = (userName == null || userName.isBlank()) ? "User" : Helper.extractUsernameFromEmail(userName);
        return TEMPLATE
                .replace("{{userName}}", userName)
                .replace("{{LOGIN_URL}}", LOGIN_URL);
    }
}
