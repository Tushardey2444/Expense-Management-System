package com.manage_expense.notification.template;

import com.manage_expense.helper.Helper;

public class ReactivateAccountOtpTemplate {
    private static final String TEMPLATE = """
            <!DOCTYPE html>
                       <html>
                       <head>
                         <meta charset="UTF-8" />
                         <title>Re-activate Account</title>
                       </head>
                       <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
                         <table width="100%" cellpadding="0" cellspacing="0">
                           <tr>
                             <td align="center">
                               <table width="600" cellpadding="0" cellspacing="0"
                                      style="background:#ffffff; margin:40px 0; border-radius:8px; overflow:hidden;">
                                 <!-- Header -->
                                 <tr>
                                   <td style="background:#0f766e; padding:20px; color:#ffffff; text-align:center;">
                                     <h1 style="margin:0; font-size:22px;">Re-activate Your Account</h1>
                                   </td>
                                 </tr>
                                 <!-- Content -->
                                 <tr>
                                   <td style="padding:30px; color:#333;">
                                     <p style="font-size:16px;">
                                       Hello <strong>{{userName}}</strong>,
                                     </p>
                                     <p style="font-size:14px; line-height:1.6;">
                                       You recently requested to re-activate your <strong>Expense Management</strong> account.
                                       To confirm this action, please use the One-Time Password (OTP) below.
                                     </p>
                                     <div style="margin:30px 0; text-align:center;">
                                       <span style="
                                         display:inline-block;
                                         font-size:28px;
                                         letter-spacing:6px;
                                         font-weight:bold;
                                         background:#f0fdfa;
                                         padding:15px 25px;
                                         border-radius:6px;
                                         color:#115e59;">
                                         {{OTP_CODE}}
                                       </span>
                                     </div>
                                     <p style="font-size:14px; color:#555;">
                                       This OTP is valid for <strong>10 minutes</strong>.
                                     </p>
                                     <p style="font-size:14px; color:#555;">
                                       If you did not request account re-activation, you can safely ignore this email.
                                       Your account will remain inactive.
                                     </p>
                                     <div style="margin-top:25px; padding:15px; background:#f8fafc; border-left:4px solid #0f766e;">
                                       <p style="margin:0; font-size:13px; color:#475569;">
                                         🔐 For your security, never share this OTP with anyone.
                                       </p>
                                     </div>
                                     <p style="margin-top:30px; font-size:14px;">
                                       Regards,<br/>
                                       <strong>Expense Management Team</strong>
                                     </p>
                                   </td>
                                 </tr>
                                 <!-- Footer -->
                                 <tr>
                                   <td style="background:#f8fafc; padding:15px; text-align:center; font-size:12px; color:#777;">
                                     © 2026 Expense Management Team. All rights reserved.
                                   </td>
                                 </tr>
                               </table>
                             </td>
                           </tr>
                         </table>
                       </body>
                       </html>
        """;

    public static String build(String userName, String otp) {
        userName = (userName == null || userName.isBlank()) ? "User" : Helper.extractUsernameFromEmail(userName);
        return TEMPLATE
                .replace("{{userName}}", userName)
                .replace("{{OTP_CODE}}", otp);
    }
}
