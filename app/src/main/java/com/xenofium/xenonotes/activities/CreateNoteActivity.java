package com.xenofium.xenonotes.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.xenofium.xenonotes.R;
import com.xenofium.xenonotes.database.NotesDatabase;
import com.xenofium.xenonotes.entities.Note;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class CreateNoteActivity extends AppCompatActivity {
    private EditText inputNoteTitle, inputNoteSubtitle, inputNoteText;
    private TextView textDateTime, textLink;
    private View viewSubtitleIndicator;
    private ImageView imageNote;
    private LinearLayout layoutLink;

    private String selectedNoteColor;
    private String selectedImagePath;

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    private AlertDialog dialogAddLink;
    private AlertDialog dialogDeleteNote;

    private Note alreadyAvailableNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
        InputMethodManager inputMethodManager = (InputMethodManager)
                this.getSystemService(Activity.INPUT_METHOD_SERVICE);

        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(v -> {
            if (this.getCurrentFocus() != null) {
                inputMethodManager.hideSoftInputFromWindow(
                        this.getCurrentFocus().getWindowToken(), 0);
            }
            onBackPressed();
        });

        ImageView imageSave = findViewById(R.id.imageSave);
        imageSave.setOnClickListener(v -> {
            if (this.getCurrentFocus() != null) {
                inputMethodManager.hideSoftInputFromWindow(
                        this.getCurrentFocus().getWindowToken(), 0);
            }
            saveNote();
        });

        inputNoteTitle = findViewById(R.id.inputNoteTitle);
        inputNoteSubtitle = findViewById(R.id.inputNoteSubtitle);
        inputNoteText = findViewById(R.id.inputNoteText);
        textDateTime = findViewById(R.id.textDateTime);
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator);
        imageNote = findViewById(R.id.imageNote);
        textLink = findViewById(R.id.textLink);
        layoutLink = findViewById(R.id.layoutLink);

        selectedNoteColor = "#333333";
        selectedImagePath = "";

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        } else {
            startDatetimeUpdater(textDateTime);
        }

        findViewById(R.id.imageRemoveLink).setOnClickListener(v -> {
            textLink.setText(null);
            layoutLink.setVisibility(View.GONE);
        });

        if (getIntent().getBooleanExtra("isFromQuickActions", false)) {
            String type = getIntent().getStringExtra("quickActionType");
            if (type != null) {
                if (type.equals("image")) {
                    selectedImagePath = getIntent().getStringExtra("imagePath");
                    imageNote.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath));
                    imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                }
                else if (type.equals("URL")) {
                    textLink.setText(getIntent().getStringExtra("URL"));
                    layoutLink.setVisibility(View.VISIBLE);
                }
            }
        }

        findViewById(R.id.imageRemoveImage).setOnClickListener(v -> {
            imageNote.setImageBitmap(null);
            imageNote.setVisibility(View.GONE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
            selectedImagePath = "";
        });

        initExtended();
        setSubtitleIndicatorColor();
    }

    private void setViewOrUpdateNote () {
        inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        inputNoteText.setText(alreadyAvailableNote.getText());
        textDateTime.setText(alreadyAvailableNote.getDatetime());

        if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            imageNote.setVisibility(View.VISIBLE);

            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            selectedImagePath = alreadyAvailableNote.getImagePath();
        }

        if (alreadyAvailableNote.getBrowserLink() != null && !alreadyAvailableNote.getBrowserLink().trim().isEmpty()) {
            textLink.setText(alreadyAvailableNote.getBrowserLink());
            layoutLink.setVisibility(View.VISIBLE);
        }


    }

    private void startDatetimeUpdater (TextView textView) {
        new datetimeUpdaterThread(textView).start();
    }

    private void saveNote () {
        if (inputNoteTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.errorTitleIsEmpty, Toast.LENGTH_LONG).show();
            return;
        } else if (inputNoteSubtitle.getText().toString().trim().isEmpty()
                && inputNoteText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.errorNoteIsEmpty, Toast.LENGTH_LONG).show();
            return;
        }

        final Note note = new Note();
        note.setTitle(inputNoteTitle.getText().toString());
        note.setSubtitle(inputNoteSubtitle.getText().toString());
        note.setText(inputNoteText.getText().toString());
        note.setDatetime(textDateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        if (layoutLink.getVisibility() == View.VISIBLE) {
            Log.i("DISPLAY _LINK", textLink.getText().toString());
            note.setBrowserLink(textLink.getText().toString());
        }

        if (alreadyAvailableNote != null) {
            note.setId(alreadyAvailableNote.getId());
        }


        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                setResult(RESULT_OK, new Intent());
                finish();
            }
        }

        new SaveNoteTask().execute();
    }

    private String capitalize(String string) {
        return (string == null || string.isEmpty()) ?
                string : string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    private void initExtended() {
        final LinearLayout layoutExtended = findViewById(R.id.layout_extended);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(layoutExtended);

        layoutExtended.findViewById(R.id.textExtended).setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        final ImageView imageDefaultColor = layoutExtended.findViewById(R.id.imageDefaultColor);
        final ImageView imageOriginalColor = layoutExtended.findViewById(R.id.imageOriginalColor);
        final ImageView imageYellowColor = layoutExtended.findViewById(R.id.imageYellowColor);
        final ImageView imageRedColor = layoutExtended.findViewById(R.id.imageRedColor);
        final ImageView imageBlueColor = layoutExtended.findViewById(R.id.imageBlueColor);
        final ImageView imageBlackColor = layoutExtended.findViewById(R.id.imageBlackColor);

        layoutExtended.findViewById(R.id.viewDefaultColor).setOnClickListener(v -> {
            selectedNoteColor = "#333333";
            imageDefaultColor.setImageResource(R.drawable.ic_done);
            imageOriginalColor.setImageResource(0);
            imageYellowColor.setImageResource(0);
            imageRedColor.setImageResource(0);
            imageBlueColor.setImageResource(0);
            imageBlackColor.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutExtended.findViewById(R.id.viewOriginalColor).setOnClickListener(v -> {
            selectedNoteColor = "#00DC00";
            imageDefaultColor.setImageResource(0);
            imageOriginalColor.setImageResource(R.drawable.ic_done);
            imageYellowColor.setImageResource(0);
            imageRedColor.setImageResource(0);
            imageBlueColor.setImageResource(0);
            imageBlackColor.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutExtended.findViewById(R.id.viewYellowColor).setOnClickListener(v -> {
            selectedNoteColor = "#FDBE3B";
            imageDefaultColor.setImageResource(0);
            imageOriginalColor.setImageResource(0);
            imageYellowColor.setImageResource(R.drawable.ic_done);
            imageRedColor.setImageResource(0);
            imageBlueColor.setImageResource(0);
            imageBlackColor.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutExtended.findViewById(R.id.viewRedColor).setOnClickListener(v -> {
            selectedNoteColor = "#FF4842";
            imageDefaultColor.setImageResource(0);
            imageOriginalColor.setImageResource(0);
            imageYellowColor.setImageResource(0);
            imageRedColor.setImageResource(R.drawable.ic_done);
            imageBlueColor.setImageResource(0);
            imageBlackColor.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutExtended.findViewById(R.id.viewBlueColor).setOnClickListener(v -> {
            selectedNoteColor = "#3A52FC";
            imageDefaultColor.setImageResource(0);
            imageOriginalColor.setImageResource(0);
            imageYellowColor.setImageResource(0);
            imageRedColor.setImageResource(0);
            imageBlueColor.setImageResource(R.drawable.ic_done);
            imageBlackColor.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutExtended.findViewById(R.id.viewBlackColor).setOnClickListener(v -> {
            selectedNoteColor = "#000000";
            imageDefaultColor.setImageResource(0);
            imageOriginalColor.setImageResource(0);
            imageYellowColor.setImageResource(0);
            imageRedColor.setImageResource(0);
            imageBlueColor.setImageResource(0);
            imageBlackColor.setImageResource(R.drawable.ic_done);
            setSubtitleIndicatorColor();
        });

        if (alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()) {
            switch (alreadyAvailableNote.getColor()) {
                case "#333333":
                    layoutExtended.findViewById(R.id.viewDefaultColor).performClick();
                    break;
                case "#00DC00":
                    layoutExtended.findViewById(R.id.viewOriginalColor).performClick();
                    break;
                case "#FDBE3B":
                    layoutExtended.findViewById(R.id.viewYellowColor).performClick();
                    break;
                case "#FF4842":
                    layoutExtended.findViewById(R.id.viewRedColor).performClick();
                    break;
                case "#3A52FC":
                    layoutExtended.findViewById(R.id.viewBlueColor).performClick();
                    break;
                case "#000000":
                    layoutExtended.findViewById(R.id.viewBlackColor).performClick();
                    break;
            }
        }

        layoutExtended.findViewById(R.id.layoutAddImage).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (ContextCompat.checkSelfPermission(
                    getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(CreateNoteActivity.this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
            }
            else {
                selectImage();
            }
        });

        layoutExtended.findViewById(R.id.layoutAddLink).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddLinkDialog();
        });

        if (alreadyAvailableNote != null) {
            layoutExtended.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutExtended.findViewById(R.id.layoutDeleteNote).setOnClickListener(v -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteNoteDialog();

            });
        }

    }

    private void showDeleteNoteDialog () {
        if (dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_delete_note,
                    (ViewGroup) findViewById(R.id.layoutDeleteNoteContainer)
            );
            builder.setView(view);
            dialogDeleteNote = builder.create();
            if (dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    @SuppressLint("StaticFieldLeak")
                    class DeleteNoteTask extends AsyncTask<Void, Void, Void> {

                        @Override
                        protected Void doInBackground(Void... voids) {
                            NotesDatabase.getDatabase(getApplicationContext()).noteDao()
                                    .deleteNote(alreadyAvailableNote);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void unused) {
                            super.onPostExecute(unused);
                            Intent intent = new Intent();
                            intent.putExtra("isNoteDeleted", true);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }

                    new DeleteNoteTask().execute();
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> dialogDeleteNote.dismiss());
        }

        dialogDeleteNote.show();
    }

    private void setSubtitleIndicatorColor() {
        GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void selectImage () {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            }
            else {
                Toast.makeText(this, "Persmission Denied!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(bitmap);
                        imageNote.setVisibility(View.VISIBLE);
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);

                        selectedImagePath = getPathFromURI(selectedImageUri);
                    }
                    catch (Exception error) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    private String getPathFromURI (Uri contentURI) {
        String filePath;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            filePath = contentURI.getPath();
        }
        else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    private void showAddLinkDialog () {
        if (dialogAddLink == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_link,
                    findViewById(R.id.layoutAddLinkContainer));
            builder.setView(view);

            dialogAddLink = builder.create();
            if (dialogAddLink.getWindow() != null) {
                dialogAddLink.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputLink = view.findViewById(R.id.inputLink);
            inputLink.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(v -> {
                if (inputLink.getText().toString().trim().isEmpty()) {
                    Toast.makeText(CreateNoteActivity.this, R.string.add_link, Toast.LENGTH_LONG).show();
                }
                else if (!Patterns.WEB_URL.matcher(inputLink.getText().toString()).matches()) {
                    Toast.makeText(CreateNoteActivity.this, R.string.enter_valid_link, Toast.LENGTH_LONG).show();
                }
                else {
                    Log.i("ACCEPT _LINK", inputLink.getText().toString());
                    textLink.setText(inputLink.getText().toString());
                    layoutLink.setVisibility(View.VISIBLE);
                    dialogAddLink.dismiss();
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> dialogAddLink.dismiss());
        }
        dialogAddLink.show();
    }

    class datetimeUpdaterThread extends Thread {
        private final TextView datetimeTextView;

        public datetimeUpdaterThread(TextView textView) {
            this.datetimeTextView = textView;
        }

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                try {

                    runOnUiThread(() -> datetimeTextView.setText(
                            capitalize(new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm",
                                    Locale.getDefault()).format(new Date()))));

                    Thread.sleep(1000);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }
}