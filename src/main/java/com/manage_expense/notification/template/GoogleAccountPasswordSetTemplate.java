package com.manage_expense.notification.template;

import com.manage_expense.helper.Helper;

public class GoogleAccountPasswordSetTemplate {

    private static final String TEMPLATE = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8" />
            <title>Password Set Successfully</title>
        </head>
        <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
            <table width="100%" cellpadding="0" cellspacing="0">
                <tr>
                    <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff; margin:40px 0; border-radius:8px; overflow:hidden;">
                            <!-- Header -->
                            <tr>
                                <td style="background:#2563eb; padding:20px; color:#ffffff; text-align:center;">
                                    <h1 style="margin:0; font-size:22px;">
                                        Password Set Successfully
                                    </h1>
                                </td>
                            </tr>

                            <!-- Content -->
                            <tr>
                                <td style="padding:30px; color:#333;">
                                    <p style="font-size:16px;">
                                        Hi <strong>{{userName}}</strong>,
                                    </p>

                                    <p style="font-size:14px; line-height:1.6;">
                                        Your password for <strong>Expense Management System</strong>
                                        has been successfully set.
                                    </p>

                                    <div style="margin:25px 0; padding:15px;
                                                background:#eff6ff; border-left:4px solid #2563eb;">
                                        <p style="margin:0; font-size:14px; color:#1e3a8a;">
                                            🔐 You can now sign in using your email and password
                                            in addition to Google sign-in.
                                        </p>
                                    </div>

                                    <p style="font-size:14px; color:#555;">
                                        If you made this change, no further action is required.
                                    </p>

                                    <p style="font-size:14px; color:#b91c1c;">
                                        <strong>Didn’t make this change?</strong><br/>
                                        Please reset your password immediately.
                                    </p>

                                    <p style="margin-top:30px; font-size:14px;">
                                        Regards,<br/>
                                        <strong>Expense Management Team</strong>
                                    </p>
                                </td>
                            </tr>

                            <!-- Footer -->
                            <tr>
                                <td style="background:#f8fafc; padding:15px;
                                           text-align:center; font-size:12px; color:#777;">
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

    public static String build(String userName) {
        userName = (userName == null || userName.isBlank())
                ? "User"
                : Helper.extractUsernameFromEmail(userName);

        return TEMPLATE.replace("{{userName}}", userName);
    }
}
