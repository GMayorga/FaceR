package com.doxua.www.facer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import static org.opencv.core.Core.LINE_8;
import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.EigenFaceRecognizer;

//For Photos:
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;



public class MainActivity extends AppCompatActivity {


    //For Photos
    public static int NOTIFICATION_ID = 1;
    Bitmap bitmapSelectGallery =null;
    Bitmap bitmapAutoGallery;
//    Bitmap finalBitmapPic;

    GalleryObserver directoryFileObserver;
    private static MainActivity instance;

    //For Photos ^

    private static final int ACCEPT_LEVEL = 1000;
    private static final int PICK_IMAGE = 100;
    private static final int IMG_SIZE = 160;

    // Views.
    private ImageView imageView;
    private TextView tv;

    // Face Detection.
    private CascadeClassifier faceDetector;
    private int absoluteFaceSize = 0;

    // Face Recognition.
    private FaceRecognizer faceRecognizer = EigenFaceRecognizer.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the image view and text view.
        imageView = (ImageView) findViewById(R.id.imageView);
        tv = (TextView) findViewById(R.id.predict_faces);

        //This is required to open the gallery to select a photo:
        Button pickImageButton = (Button) findViewById(R.id.btnGallery);
        pickImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });


        instance = this;

        directoryFileObserver = new GalleryObserver("/storage/emulated/0/MyGlass/");
        directoryFileObserver.startWatching();

        lastPhotoInGallery();



        findViewById(R.id.btTrain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, TrainFaces.class));
            }
        });
    }




        private void openGallery() {
            Intent gallery =
                    new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(gallery, PICK_IMAGE);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            //This code is to display the selected image from Gallery and convert to Bitmap
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
                Uri imageUri = data.getData();

                //This is required to make a bitmap out of URI. Notifications can only display bitmap
                try {
                    bitmapSelectGallery = MediaStore.Images.Media.getBitmap(
                            this.getContentResolver(), imageUri);

                } catch (Exception e) {

                }
                imageView.setImageBitmap(bitmapSelectGallery);

                //This is required in order to make notification appear automatically:
//            notifications();

                if (bitmapSelectGallery !=null) {
                    detectDisplayAndRecognize(bitmapSelectGallery);
                }
            }
        }


        public static MainActivity getInstance() {
            return instance;
        }

        public void lastPhotoInGallery () {
            // Find the last picture

            String[] projection = new String[]{
                    MediaStore.Images.ImageColumns._ID,
                    MediaStore.Images.ImageColumns.DATA,
                    MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.DATE_TAKEN,
                    MediaStore.Images.ImageColumns.MIME_TYPE
            };
            final Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                    null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

            // Put it in the image view


            if (cursor.moveToFirst()) {
                final ImageView imageView = (ImageView) findViewById(R.id.imageView);
                String imageLocation  = cursor.getString(1);
                File imageFile = new File(imageLocation);

                if (imageFile.exists()) {
                    bitmapAutoGallery = BitmapFactory.decodeFile(imageLocation);

                    if (bitmapAutoGallery != null) {
                        imageView.setImageBitmap(bitmapAutoGallery);
//
//                    //This is required in order to make notification appear automatically
//                    //However, a delay is required because if it appears to soon on phone, it will not appear on Glass
////                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
////                        @Override
////                        public void run() {
////                            notifications();
////                        }
////                    }, 2000);

                        detectDisplayAndRecognize(bitmapAutoGallery);

                    }
                }
            }

        }


//    public void notifications(){
//        //This code is required to send notifications to the phone and Google Glass
//        //Google Glass automatically will display phone notifications as part of its design
//
//        //This is used to open the new screen when the notification is clicked on the phone:
//        Intent detailsIntent = new Intent(RegFaces.this, DetailsActivity.class);
//        detailsIntent.putExtra("EXTRA_DETAILS_ID", 42);
//        PendingIntent detailsPendingIntent = PendingIntent.getActivity(
//                RegFaces.this,
//                0,
//                detailsIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT
//        );
//
//        //Need to increase notification id by 1 in order to have multiple notifications displayed, otherwise notifications
//        //will overwrite previous notification
//        NOTIFICATION_ID++;
//
//        //To determine what needs to be displayed
//        if (bitmapSelectGallery !=null){
//
//            //bitmapSelectGallery is for images selected from Gallery on phone
//            //Need to resize bitmaps otherwise app will crash and/or not display photo correctly
//            finalBitmapPic = Bitmap.createScaledBitmap(bitmapSelectGallery, 500, 800, false);
//        }
//        else{
//            //bitmapAutoGallery is for the image that auto loads on app since it is latest image in Gallery
//            //Need to resize bitmaps otherwise app will crash and/or not display photo correctly
//            finalBitmapPic = Bitmap.createScaledBitmap(bitmapAutoGallery, 500, 800, false);
//        }
//
//        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(RegFaces.this)
//
//                //LargeIcon needs to be updated to pull from app
//                //setContentTitle needs to be updated to info about match
//                .setSmallIcon(android.R.drawable.ic_dialog_info)
//                .setLargeIcon(finalBitmapPic)
//                .setContentTitle("Database Text Once It's Built")
//                .setAutoCancel(true)
//                .setContentIntent(detailsPendingIntent)
//                .addAction(android.R.drawable.ic_menu_compass, "Details", detailsPendingIntent);
//
//        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
//
//    }
        /**
         * Face Detection.
         * Face Recognition.
         * Display the detection result and recognition result.
         * @param bitmap
         */
        void detectDisplayAndRecognize(Bitmap bitmap) {

            // Create a new gray Mat.
            Mat greyMat = new Mat();
            // JavaCV frame converters.
            AndroidFrameConverter converterToBitmap = new AndroidFrameConverter();
            OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();

            // -------------------------------------------------------------------
            //                    Convert to mat for processing
            // -------------------------------------------------------------------
            // Convert to Bitmap.
            Frame frame = converterToBitmap.convert(bitmap);
            // Convert to Mat.
            Mat colorMat = converterToMat.convert(frame);


            // Convert to Gray scale.
            cvtColor(colorMat, greyMat, CV_BGR2GRAY);
            // Vector of rectangles where each rectangle contains the detected object.
            RectVector faces = new RectVector();


            // -----------------------------------------------------------------------------------------
            //                                  FACE DETECTION
            // -----------------------------------------------------------------------------------------
            // Load the CascadeClassifier class to detect objects.
            faceDetector = TrainFaces.loadClassifierCascade(MainActivity.this, R.raw.frontalface);
            // Detect the face.
            faceDetector.detectMultiScale(greyMat, faces, 1.25f, 3, 1,
                    new Size(absoluteFaceSize, absoluteFaceSize),
                    new Size(4 * absoluteFaceSize, 4 * absoluteFaceSize));


            // Count number of faces and display in text view.
            int numFaces = (int) faces.size();

            // -----------------------------------------------------------------------------------------
            //                                      DISPLAY
            // -----------------------------------------------------------------------------------------

            if (numFaces > 0) {
                // Multiple face detection.
                for (int i = 0; i < numFaces; i++) {

                    int x = faces.get(i).x();
                    int y = faces.get(i).y();
                    int w = faces.get(i).width();
                    int h = faces.get(i).height();

                    rectangle(colorMat, new Point(x, y), new Point(x + w, y + h), Scalar.GREEN, 2, LINE_8, 0);

                    // -------------------------------------------------------------------
                    //              Convert back to bitmap for displaying
                    // -------------------------------------------------------------------
                    // Convert processed Mat back to a Frame
                    frame = converterToMat.convert(colorMat);
                    // Copy the data to a Bitmap for display or something
                    Bitmap bm = converterToBitmap.convert(frame);

                    // Display the picked image.
                    imageView.setImageBitmap(bm);
                }
            } else {
                imageView.setImageBitmap(bitmap);
                tv.setText("No Face Detected.");

            }
            // -----------------------------------------------------------------------------------------
            //                                  FACE RECOGNITION
            // -----------------------------------------------------------------------------------------

            if (numFaces > 0) {

                recognize(faces.get(0), greyMat, tv);

            }
        }


        /**
         * Predict whether the choosing image is matching or not.
         * IMPORTANT.
         * @param dadosFace
         * @param greyMat
         */
        void recognize(Rect dadosFace, Mat greyMat, TextView tv) {

            // Find the root path.
            String root = Environment.getExternalStorageDirectory().toString();

            // Find the correct root path where our trained face model is stored.
            String personName = "Tom Cruise";
            String photosFolderPath = root + "/saved_images/tom_cruise";
            File photosFolder = new File(photosFolderPath);
            File f = new File(photosFolder, TrainFaces.EIGEN_FACES_CLASSIFIER);

            // Loads a persisted model and state from a given XML or YAML file.
            faceRecognizer.read(f.getAbsolutePath());

            Mat detectedFace = new Mat(greyMat, dadosFace);
            resize(detectedFace, detectedFace, new Size(IMG_SIZE, IMG_SIZE));

            IntPointer label = new IntPointer(1);
            DoublePointer reliability = new DoublePointer(1);
            faceRecognizer.predict(detectedFace, label, reliability);

            // Display on the text view what we found.
            int prediction = label.get(0);
            int acceptanceLevel = (int) reliability.get(0);

            // If a face is not found but we have its model.
            // Read the next model to find the matching.
            if (prediction <= -1 || acceptanceLevel >= ACCEPT_LEVEL) {

                // Find the correct root path where our trained face model is stored.
                personName = "Katie Holmes";
                photosFolderPath = root + "/saved_images/katie_holmes";
                photosFolder = new File(photosFolderPath);
                f = new File(photosFolder, TrainFaces.EIGEN_FACES_CLASSIFIER);

                // Loads a persisted model and state from a given XML or YAML file.
                faceRecognizer.read(f.getAbsolutePath());

                detectedFace = new Mat(greyMat, dadosFace);
                resize(detectedFace, detectedFace, new Size(IMG_SIZE, IMG_SIZE));

                label = new IntPointer(1);
                reliability = new DoublePointer(1);
                faceRecognizer.predict(detectedFace, label, reliability);

                // Display on the text view what we found.
                prediction = label.get(0);
                acceptanceLevel = (int) reliability.get(0);

            }


            // Display the prediction.
            if (prediction <= -1 || acceptanceLevel >= ACCEPT_LEVEL) {
                // Display on text view, not matching or unknown person.
                tv.setText("\tUnknown." + "\nAcceptance Level Too High: " +acceptanceLevel);

            } else {
                // Display the information for the matching image.
                tv.setText("A match is found: " + personName + "\nAcceptance Level: " + acceptanceLevel);        }

        }

    }

