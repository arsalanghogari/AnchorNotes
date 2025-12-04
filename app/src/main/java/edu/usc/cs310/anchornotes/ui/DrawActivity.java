package edu.usc.cs310.anchornotes.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.usc.cs310.anchornotes.R;

public class DrawActivity extends AppCompatActivity {

    public static final String EXTRA_DRAWING_URI = "edu.usc.cs310.anchornotes.EXTRA_DRAWING_URI";

    private DrawingView drawingView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        drawingView = findViewById(R.id.drawingView);
        Button clearButton = findViewById(R.id.clearButton);
        Button saveButton = findViewById(R.id.saveButton);

        clearButton.setOnClickListener(v -> drawingView.clear());

        saveButton.setOnClickListener(v -> {
            Uri uri = saveDrawingToFile();
            if (uri != null) {
                Intent data = new Intent();
                data.putExtra(EXTRA_DRAWING_URI, uri.toString());
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }

    private Uri saveDrawingToFile() {
        Bitmap bitmap = drawingView.getBitmap();
        if (bitmap == null) return null;

        FileOutputStream out = null;
        try {
            // Reuse same "photos" directory style as EditorActivity
            File dir = getExternalFilesDir("photos");
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            File targetDir = (dir != null) ? dir : getFilesDir();
            File file = new File(targetDir, "drawing_" + System.currentTimeMillis() + ".png");

            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();

            return Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (out != null) {
                try { out.close(); } catch (IOException ignored) {}
            }
        }
    }
}
