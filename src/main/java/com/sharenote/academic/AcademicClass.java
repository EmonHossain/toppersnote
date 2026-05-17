package com.sharenote.academic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "academic_classes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_academic_class_key",
                columnNames = {"institution", "degree_program", "year", "semester", "subject_class"}
        )
)
public class AcademicClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String institution;

    @Column(name = "degree_program", nullable = false, length = 120)
    private String degreeProgram;

    @Column(nullable = false, length = 20)
    private String year;

    @Column(nullable = false, length = 50)
    private String semester;

    @Column(name = "subject_class", nullable = false, length = 120)
    private String subjectClass;

    protected AcademicClass() {
    }

    public AcademicClass(String institution, String degreeProgram, String year, String semester, String subjectClass) {
        this.institution = institution;
        this.degreeProgram = degreeProgram;
        this.year = year;
        this.semester = semester;
        this.subjectClass = subjectClass;
    }

    public Long getId() {
        return id;
    }

    public String getInstitution() {
        return institution;
    }

    public String getDegreeProgram() {
        return degreeProgram;
    }

    public String getYear() {
        return year;
    }

    public String getSemester() {
        return semester;
    }

    public String getSubjectClass() {
        return subjectClass;
    }
}
