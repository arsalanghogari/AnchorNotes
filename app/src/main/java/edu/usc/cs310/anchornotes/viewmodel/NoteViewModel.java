package edu.usc.cs310.anchornotes.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;
import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.repository.NotesRepository;

public class NoteViewModel extends ViewModel {

    private final NotesRepository repository = NotesRepository.getInstance();
    private final MutableLiveData<List<Note>> notesLiveData = new MutableLiveData<>();

    public NoteViewModel() {
        notesLiveData.setValue(repository.getAllNotes());
    }

    public LiveData<List<Note>> getNotesLiveData() {
        return notesLiveData;
    }

    public void addNote(String title, String body) {
        repository.addNote(new Note(title, body));
        notesLiveData.setValue(repository.getAllNotes());
    }

    public void updateNotes(List<Note> updatedList) {
        notesLiveData.setValue(updatedList);
    }

}
