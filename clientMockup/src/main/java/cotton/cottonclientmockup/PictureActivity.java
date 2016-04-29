package cotton.cottonclientmockup;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**a
 *
 *
 * @author Jonathan
 * @author Gunnlaugur
 */
public class PictureActivity extends AppCompatActivity{
    private ImageView view;
    private String CurrentPhotoPath;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        view = (ImageView) findViewById(R.id.imageView);

        Button pictureButton = (Button) findViewById(R.id.imageButton);
        assert pictureButton != null;
        pictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        sendButton = (Button) findViewById(R.id.sendbutton);
        assert sendButton != null;
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), NetworkService.class);
                System.out.println(CurrentPhotoPath);
                intent.putExtra("filename", CurrentPhotoPath);
                intent.putExtra("reciever", new PictureResultReciever(new Handler()));
                startService(intent);
                setToast("Sending to server!");
            }
        });
        sendButton.setClickable(false);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 111) {
                /*Bitmap image = (Bitmap) data.getExtras().get("data");
                view.setImageBitmap(image);
                */File imgFile = new File(CurrentPhotoPath);

                if(imgFile.exists()){
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    view.setImageBitmap(myBitmap);
                    sendButton.setClickable(true);
                }
            }
        } else {
            System.out.println("Something went wrong");
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        CurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;

            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }

            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, 111);
            }
        }
        galleryAddPic();
    }

    private void galleryAddPic() {
        File f = new File(CurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,contentUri);
        sendBroadcast(mediaScanIntent);
    }

    @SuppressLint("ParcelCreator")
    private class PictureResultReciever extends ResultReceiver{
        public PictureResultReciever(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            String resultName = resultData.getString("filename");
            File imgFile = new File(resultName);

            if(imgFile.exists()){
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                view.setImageBitmap(myBitmap);
                sendButton.setClickable(true);
                setToast("Recieved image from server!");
            }

        }
    }

    private void setToast(String s) {
        Toast.makeText(PictureActivity.this, s, Toast.LENGTH_SHORT).show();
    }

}

