package com.sharenote.academic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "academic_classes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_academic_class_key",
                columnNames = {"institution", "degree_program", "year", "semester", "subject_class"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public AcademicClass(String institution, String degreeProgram, String year, String semester, String subjectClass) {
        this.institution = institution;
        this.degreeProgram = degreeProgram;
        this.year = year;
        this.semester = semester;
        this.subjectClass = subjectClass;
    }

}
