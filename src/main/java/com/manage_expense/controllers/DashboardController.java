package com.manage_expense.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "6. Dashboard API", description = "dashboard related APIs for retrieving summary and analytics data about user's expenses, budgets, and financial health. This includes endpoints for getting monthly summaries, budget vs actual comparisons, spending trends, and other insights to help users manage their finances effectively.")
public class DashboardController {

}
