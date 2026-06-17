package com.sharenote.dashboard;

import com.sharenote.dashboard.dto.PersonalStudyDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Personal Dashboard", description = "JWT-secured personalized study dashboard endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class PersonalStudyDashboardController {

    private final PersonalStudyDashboardService dashboardService;

    // getMyDashboard: Returns the authenticated user's personalized study dashboard.
    @GetMapping("/me")
    @Operation(summary = "Get my personal study dashboard")
    public ResponseEntity<PersonalStudyDashboardResponse> getMyDashboard() {
        return ResponseEntity.ok(dashboardService.getMyDashboard());
    }
}
