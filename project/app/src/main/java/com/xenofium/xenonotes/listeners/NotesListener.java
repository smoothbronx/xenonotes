package com.xenofium.xenonotes.listeners;

import com.xenofium.xenonotes.entities.Note;

public interface NotesListener {
    void onNoteClicked (Note note, int position);

}
