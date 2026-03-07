package com.manage_expense.notification.template;

import com.manage_expense.helper.Helper;

public class AccountDeletionScheduledTemplate {
    private static final String TEMPLATE = """
            <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <title>Account Deletion Scheduled</title>
                </head>
                <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
                  <table width="100%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff; margin:40px 0; border-radius:8px; overflow:hidden;">
                          <!-- Header -->
                          <tr>
                            <td style="background:#b91c1c; padding:20px; color:#ffffff; text-align:center;">
                              <h1 style="margin:0; font-size:22px;">Account Deletion Scheduled</h1>
                            </td>
                          </tr>
                          <!-- Content -->
                          <tr>
                            <td style="padding:30px; color:#333;">
                              <p style="font-size:16px;">
                                Hello <strong>{{userName}}</strong>,
                              </p>
                              <p style="font-size:14px; line-height:1.6;">
                                This email is to inform you that your <strong>Expense Management</strong> account
                                has been scheduled for permanent deletion.
                              </p>
                              <div style="margin:25px 0; padding:15px; background:#fef2f2; border-left:4px solid #b91c1c;">
                                <p style="margin:0; font-size:14px; color:#7f1d1d;">
                                  ⚠️ Your account will be permanently deleted after <strong>30 days</strong>.
                                </p>
                              </div>
                              <!-- NEW IMPORTANT MESSAGE -->
                              <p style="font-size:14px; color:#374151; font-weight:600; margin-top:20px;">
                                Please log in to re-activate your account within the next 30 days to prevent
                                permanent deletion.
                              </p>
                              <p style="font-size:14px; color:#555;">
                                During this period, your account will remain inactive.
                                Re-activating your account will immediately restore access to your data.
                              </p>
                              <div style="margin:30px 0; text-align:center;">
                                <a href="{{REACTIVATE_URL}}" style="
                                  display:inline-block;
                                  background:#2563eb;
                                  color:#ffffff;
                                  text-decoration:none;
                                  padding:12px 24px;
                                  border-radius:6px;
                                  font-size:14px;
                                  font-weight:bold;">
                                  Re-activate Account
                                </a>
                              </div>
                              <p style="font-size:14px; color:#555;">
                                If you did not request this action, please contact our support team immediately.
                              </p>
                              <p style="margin-top:30px; font-size:14px;">
                                Regards,<br/>
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

    public static String build(String userName, String REACTIVATE_URL) {
        userName = (userName == null || userName.isBlank()) ? "User" : Helper.extractUsernameFromEmail(userName);
        return TEMPLATE
                .replace("{{userName}}", userName)
                .replace("{{REACTIVATE_URL}}", REACTIVATE_URL);
    }
}
