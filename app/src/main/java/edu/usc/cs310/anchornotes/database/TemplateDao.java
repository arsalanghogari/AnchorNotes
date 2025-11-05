package edu.usc.cs310.anchornotes.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import edu.usc.cs310.anchornotes.model.Template;

@Dao
public interface TemplateDao {

    @Insert
    long insert(Template template);

    @Update
    void update(Template template);

    @Delete
    void delete(Template template);

    @Query("SELECT * FROM templates ORDER BY name ASC")
    LiveData<List<Template>> getAllTemplates();

    @Query("SELECT * FROM templates WHERE id = :id LIMIT 1")
    Template getById(int id);

    @Query("SELECT COUNT(*) FROM templates")
    int countTemplates();
}

