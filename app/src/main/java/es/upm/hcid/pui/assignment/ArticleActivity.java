package es.upm.hcid.pui.assignment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileNotFoundException;
import java.io.InputStream;

import es.upm.hcid.pui.assignment.exceptions.ServerCommunicationError;

public class ArticleActivity extends AppCompatActivity {

    private static final String PARAM_ARTICLE = "article";
    private static final int OPEN_IMAGE = 1;
    Article SelectedArticle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.efm_logo_full_white);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        int id = intent.getIntExtra(PARAM_ARTICLE, -1);

        DownloadArticleThread d = new DownloadArticleThread(this, id);

        Thread th = new Thread(d);
        th.start();
    }

    protected void initialize(Article article) throws ServerCommunicationError {

        SelectedArticle = article;

        TextView article_title = findViewById(R.id.titleText);
       // TextView article_subtitle = findViewById(R.id.subtitle_textView);
        ImageView article_image = findViewById(R.id.imageView);
        TextView article_category = findViewById(R.id.categoryText);
        TextView article_abstract = findViewById(R.id.abstractText);
        TextView article_body = findViewById(R.id.bodyText);
        TextView userId = findViewById(R.id.userIdText);
       // TextView modification_date = findViewById(R.id.modification_textView);

        article_title.setText(article.getTitleText());
        article_abstract.setText(Html.fromHtml(article.getAbstractText(), Html.FROM_HTML_MODE_COMPACT));
        article_body.setText(Html.fromHtml(article.getBodyText(), Html.FROM_HTML_MODE_COMPACT));
        article_category.setText(article.getCategory());


        /*String modificationDate = "Modification date ";
        if (article.getModificationDate()) {
            modificationDate += article.getModificationDate();
        }
        modification_date.setText(modificationDate);*/

        String userIdString = "User ID: ";
        if (article.getIdUser() > 0) {
            userIdString += article.getIdUser();
        }
        userId.setText(userIdString);

        if (article.getImage() != null && article.getImage().getImage() != null) {
            article_image.setImageBitmap(Utils.base64StringToImg(article.getImage().getImage()));
        } else {
            article_image.setImageResource(R.drawable.newspaper);
        }

        if (MainActivity.loggedIn) {

            Button btn_image_edit = findViewById(R.id.btn_imageEdit);
            btn_image_edit.setVisibility(View.VISIBLE);
            btn_image_edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, OPEN_IMAGE);
                }
            });

            Button btn_article_save = findViewById(R.id.btn_saveArticle);
            btn_article_save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Thread thread = new Thread(() -> {
                        try {
                            MainActivity.modelManager.save(SelectedArticle);
                            finish();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    thread.start();

                }
            });

            Button btn_article_delete = findViewById(R.id.btn_deleteArticle);
            btn_article_delete.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case OPEN_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    InputStream stream = null;
                    try {
                        stream = getContentResolver().openInputStream(data.getData());
                        Bitmap bmp = Utils.createScaledImage(BitmapFactory.decodeStream(stream), 500, 500);
                        SelectedArticle.addImage(Utils.imgToBase64String(bmp), "");

                        ImageView viewer = findViewById(R.id.imageView);
                        viewer.setImageBitmap(bmp);

                        Button btn_article_save = findViewById(R.id.btn_saveArticle);
                        btn_article_save.setVisibility(View.VISIBLE);
                    } catch (FileNotFoundException | ServerCommunicationError e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "The action was cancelled", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    public void deleteArticle(View view) {
        new Thread(() -> {
            try {
                MainActivity.modelManager.delete(SelectedArticle);
                runOnUiThread(this::finish);
            } catch (ServerCommunicationError serverCommunicationError) {
                serverCommunicationError.printStackTrace();
            }
        }).start();
    }
}