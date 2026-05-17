package com.sharenote.notification;

import com.sharenote.note.Note;
import com.sharenote.user.User;

import java.util.Collection;

public interface NotificationPublisher {

    void notifyNewNote(Note note);

    void notifyTakeALook(Note note, User suggestedBy, Collection<User> recipients, String suggestionMessage);
}
