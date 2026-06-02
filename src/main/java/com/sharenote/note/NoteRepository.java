package com.sharenote.note;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndSubjectClassIgnoreCaseAndSemesterIgnoreCaseAndYearIgnoreCaseAndLatestTrueOrderByCreatedAtDesc(
            String institution,
            String degreeProgram,
            String subjectClass,
            String semester,
            String year
    );

    List<Note> findTop20ByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndSubjectClassIgnoreCaseAndSemesterIgnoreCaseAndYearIgnoreCaseAndLatestTrueOrderByCreatedAtDesc(
            String institution,
            String degreeProgram,
            String subjectClass,
            String semester,
            String year
    );

    List<Note> findByIdIn(List<Long> ids);

    java.util.Optional<Note> findFirstByFileHash(String fileHash);

    @org.springframework.data.jpa.repository.Query("SELECT n FROM Note n WHERE n.id = :rootId OR n.parentNote.id = :rootId ORDER BY n.versionNumber ASC")
    List<Note> findAllVersions(@org.springframework.data.repository.query.Param("rootId") Long rootId);
}
