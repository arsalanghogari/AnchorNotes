package edu.usc.cs310.anchornotes.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.usc.cs310.anchornotes.model.Note;
import edu.usc.cs310.anchornotes.model.NoteTag;
import edu.usc.cs310.anchornotes.model.Tag;
import edu.usc.cs310.anchornotes.model.Template;

@Database(entities = { Note.class, Tag.class, NoteTag.class, Template.class }, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public static final ExecutorService databaseWriteExecutor =
            Executors.newSingleThreadExecutor();

    public abstract NoteDao noteDao();
    public abstract TagDao tagDao(); // Add this line
    public abstract TemplateDao templateDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "anchor_notes_db"
                            ).fallbackToDestructiveMigration() // This will clear database on version change
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}