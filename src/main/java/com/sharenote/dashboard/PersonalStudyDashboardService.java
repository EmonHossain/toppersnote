package com.sharenote.dashboard;

import com.sharenote.academic.AcademicClass;
import com.sharenote.academic.ClassRegistration;
import com.sharenote.academic.ClassRegistrationRepository;
import com.sharenote.dashboard.dto.DashboardClassResponse;
import com.sharenote.dashboard.dto.DashboardExamReminderResponse;
import com.sharenote.dashboard.dto.DashboardNotificationResponse;
import com.sharenote.dashboard.dto.DashboardRecommendedNoteResponse;
import com.sharenote.dashboard.dto.DashboardStudyGroupResponse;
import com.sharenote.dashboard.dto.DashboardSummaryResponse;
import com.sharenote.dashboard.dto.PersonalStudyDashboardResponse;
import com.sharenote.lifecycle.ExamReminder;
import com.sharenote.lifecycle.ExamReminderRepository;
import com.sharenote.note.CurrentUserNotFoundException;
import com.sharenote.note.Note;
import com.sharenote.note.NoteRepository;
import com.sharenote.notification.Notification;
import com.sharenote.notification.NotificationRepository;
import com.sharenote.notification.NotificationType;
import com.sharenote.quality.NoteQualityScoringService;
import com.sharenote.quality.dto.NoteQualityScoreResponse;
import com.sharenote.studygroup.StudyGroup;
import com.sharenote.studygroup.StudyGroupRepository;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonalStudyDashboardService {

    private static final String ANONYMOUS_DISPLAY_NAME = "Anonymous";

    private final UserRepository userRepository;
    private final ClassRegistrationRepository classRegistrationRepository;
    private final ExamReminderRepository examReminderRepository;
    private final NotificationRepository notificationRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final NoteRepository noteRepository;
    private final NoteQualityScoringService noteQualityScoringService;
    private final DashboardProperties dashboardProperties;
    private final Clock clock = Clock.systemUTC();

    // getMyDashboard: Builds the authenticated user's bounded study dashboard snapshot.
    @Transactional(readOnly = true)
    public PersonalStudyDashboardResponse getMyDashboard() {
        User currentUser = getCurrentUser();
        Instant generatedAt = Instant.now(clock);
        List<ClassRegistration> activeRegistrations = classRegistrationRepository.findActiveByUserId(currentUser.getId());
        List<AcademicClass> activeClasses = activeRegistrations.stream()
                .map(ClassRegistration::getAcademicClass)
                .toList();
        List<DashboardClassResponse> classResponses = activeClasses.stream()
                .limit(dashboardProperties.normalizedMaxClasses())
                .map(this::toClassResponse)
                .toList();

        List<DashboardExamReminderResponse> upcomingExams = examReminderRepository
                .findUpcomingActiveByAcademicContext(
                        currentUser.getInstitution(),
                        currentUser.getDegreeProgram(),
                        currentUser.getCurrentYear(),
                        currentUser.getCurrentSemester(),
                        LocalDate.now(clock),
                        dashboardProperties.normalizedMaxUpcomingExams()
                )
                .stream()
                .map(this::toExamResponse)
                .toList();
        List<Notification> unreadSuggestionNotifications = notificationRepository
                .findByRecipientIdAndTypeAndReadAtIsNullOrderByCreatedAtDesc(
                        currentUser.getId(),
                        NotificationType.TAKE_A_LOOK,
                        dashboardProperties.normalizedMaxUnreadSuggestions()
                );
        List<DashboardNotificationResponse> unreadSuggestions = unreadSuggestionNotifications.stream()
                .map(this::toNotificationResponse)
                .toList();
        List<DashboardNotificationResponse> recentNotifications = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(
                        currentUser.getId(),
                        dashboardProperties.normalizedMaxRecentNotifications()
                )
                .stream()
                .map(this::toNotificationResponse)
                .toList();
        List<DashboardStudyGroupResponse> activeStudyGroups = studyGroupRepository
                .findByMemberIdOrderByCreatedAtDesc(
                        currentUser.getId(),
                        dashboardProperties.normalizedMaxActiveStudyGroups()
                )
                .stream()
                .map(this::toStudyGroupResponse)
                .toList();
        List<DashboardRecommendedNoteResponse> recommendedNotes = buildRecommendedNotes(currentUser, activeClasses, generatedAt);

        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                activeRegistrations.size(),
                upcomingExams.size(),
                notificationRepository.countByRecipientIdAndReadAtIsNull(currentUser.getId()),
                notificationRepository.countByRecipientIdAndTypeAndReadAtIsNull(currentUser.getId(), NotificationType.TAKE_A_LOOK),
                activeStudyGroups.size(),
                recommendedNotes.size()
        );

        log.info(
                "Personal dashboard generated userId={} classCount={} examCount={} recommendedNoteCount={}",
                currentUser.getId(),
                activeRegistrations.size(),
                upcomingExams.size(),
                recommendedNotes.size()
        );

        return new PersonalStudyDashboardResponse(
                currentUser.getId(),
                formatName(currentUser),
                currentUser.getInstitution(),
                currentUser.getDegreeProgram(),
                currentUser.getCurrentYear(),
                currentUser.getCurrentSemester(),
                generatedAt,
                summary,
                classResponses,
                upcomingExams,
                recommendedNotes,
                unreadSuggestions,
                recentNotifications,
                activeStudyGroups
        );
    }

    // buildRecommendedNotes: Ranks visible class notes using engagement and freshness.
    private List<DashboardRecommendedNoteResponse> buildRecommendedNotes(
            User currentUser,
            List<AcademicClass> activeClasses,
            Instant generatedAt
    ) {
        List<Note> candidateNotes = noteRepository.findVisibleLatestNotesForClasses(
                currentUser.getInstitution(),
                currentUser.getDegreeProgram(),
                activeClasses,
                dashboardProperties.normalizedRecommendationCandidateLimit()
        );
        if (candidateNotes.isEmpty()) {
            return List.of();
        }

        Map<Long, NoteQualityScoreResponse> qualityScores = noteQualityScoringService.scoreNotesForRanking(candidateNotes);

        return candidateNotes.stream()
                .map(note -> toRecommendedNoteResponse(note, qualityScores.get(note.getId())))
                .sorted(Comparator
                        .comparingLong(DashboardRecommendedNoteResponse::recommendationScore).reversed()
                        .thenComparing(DashboardRecommendedNoteResponse::createdAt, Comparator.reverseOrder()))
                .limit(dashboardProperties.normalizedMaxRecommendedNotes())
                .toList();
    }

    // toRecommendedNoteResponse: Converts a note and quality score into a recommendation DTO.
    private DashboardRecommendedNoteResponse toRecommendedNoteResponse(
            Note note,
            NoteQualityScoreResponse score
    ) {
        return new DashboardRecommendedNoteResponse(
                note.getId(),
                note.getSubjectClass(),
                note.getSemester(),
                note.getYear(),
                note.getOriginalFileName(),
                note.getContentType(),
                note.getFileSize(),
                publicUploaderId(note),
                publicUploaderName(note),
                note.isAnonymousUpload(),
                score == null ? 0 : score.upvoteCount(),
                score == null ? 0 : score.viewCount(),
                score == null ? 0 : score.downloadCount(),
                score == null ? 0 : score.score(),
                note.getCreatedAt()
        );
    }

    // toClassResponse: Converts an academic class into a dashboard DTO.
    private DashboardClassResponse toClassResponse(AcademicClass academicClass) {
        return new DashboardClassResponse(
                academicClass.getId(),
                academicClass.getInstitution(),
                academicClass.getDegreeProgram(),
                academicClass.getYear(),
                academicClass.getSemester(),
                academicClass.getSubjectClass()
        );
    }

    // toExamResponse: Converts an exam reminder into a dashboard DTO.
    private DashboardExamReminderResponse toExamResponse(ExamReminder reminder) {
        return new DashboardExamReminderResponse(
                reminder.getId(),
                reminder.getSubjectClass(),
                reminder.getYear(),
                reminder.getSemester(),
                reminder.getExamDate(),
                reminder.getDetails(),
                reminder.getUpdatedAt()
        );
    }

    // toNotificationResponse: Converts a notification into a dashboard DTO.
    private DashboardNotificationResponse toNotificationResponse(Notification notification) {
        Note note = notification.getNote();
        return new DashboardNotificationResponse(
                notification.getId(),
                notification.getType().name(),
                note == null ? null : note.getId(),
                note == null ? null : note.getSubjectClass(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    // toStudyGroupResponse: Converts a study group into a dashboard DTO.
    private DashboardStudyGroupResponse toStudyGroupResponse(StudyGroup studyGroup) {
        User creator = studyGroup.getCreator();
        return new DashboardStudyGroupResponse(
                studyGroup.getId(),
                studyGroup.getName(),
                studyGroup.getDescription(),
                studyGroup.getInstitution(),
                studyGroup.getDegreeProgram(),
                creator.getId(),
                formatName(creator),
                studyGroup.getMembers().size(),
                studyGroup.getCreatedAt()
        );
    }

    // getCurrentUser: Loads the authenticated user represented by the JWT subject.
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CurrentUserNotFoundException();
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(CurrentUserNotFoundException::new);
    }

    // publicUploaderId: Masks anonymous uploader ids from dashboard recommendations.
    private Long publicUploaderId(Note note) {
        return note.isAnonymousUpload() ? null : note.getUploadedBy().getId();
    }

    // publicUploaderName: Masks anonymous uploader names from dashboard recommendations.
    private String publicUploaderName(Note note) {
        return note.isAnonymousUpload() ? ANONYMOUS_DISPLAY_NAME : formatName(note.getUploadedBy());
    }

    // formatName: Builds a display name without exposing email addresses.
    private String formatName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
