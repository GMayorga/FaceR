package com.doxua.www.facer;

import android.os.FileObserver;
import android.os.Looper;

import android.os.Handler;


public class GalleryObserver extends FileObserver {

    String galleryPath;


    public GalleryObserver(String path) {
        super(path, FileObserver.MODIFY);
        galleryPath = path;
    }

    @Override
    public void onEvent(int event, String path) {
        if(path != null){
//            Delay needed because APP runs faster than Glass can send photo to Gallery
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

              MainActivity.getInstance().lastPhotoInGallery();
            }
        },1000);

        }
    }


}
