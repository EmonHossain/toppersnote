package com.sharenote.academic;

import com.sharenote.academic.dto.AcademicClassResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/academic")
public class AcademicClassController {

    private final AcademicClassService academicClassService;

    public AcademicClassController(AcademicClassService academicClassService) {
        this.academicClassService = academicClassService;
    }

    @GetMapping("/classes/me")
    public ResponseEntity<List<AcademicClassResponse>> getMyClasses() {
        return ResponseEntity.ok(academicClassService.getMyClasses());
    }

    @GetMapping("/degree-programs")
    public ResponseEntity<List<String>> getDegreePrograms() {
        return ResponseEntity.ok(academicClassService.getDegreePrograms());
    }

    @GetMapping("/years")
    public ResponseEntity<List<String>> getYears(@RequestParam("degreeProgram") String degreeProgram) {
        return ResponseEntity.ok(academicClassService.getYears(degreeProgram));
    }

    @GetMapping("/semesters")
    public ResponseEntity<List<String>> getSemesters(
            @RequestParam("degreeProgram") String degreeProgram,
            @RequestParam("year") String year
    ) {
        return ResponseEntity.ok(academicClassService.getSemesters(degreeProgram, year));
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<String>> getSubjects(
            @RequestParam("degreeProgram") String degreeProgram,
            @RequestParam("year") String year,
            @RequestParam("semester") String semester
    ) {
        return ResponseEntity.ok(academicClassService.getSubjects(degreeProgram, year, semester));
    }
}
