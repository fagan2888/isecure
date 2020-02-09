package com.infy.stg.isecure;

import android.graphics.Bitmap;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FaceRecognizer {

    public static Bitmap cropFace(FaceDetector detector, Bitmap bitmap) {
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> arr = detector.detect(frame);
        List<Face> faces = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            Face face = arr.get(i);
            faces.add(face);
        }
        Face face = faces.stream().max(new Comparator<Face>() {
            @Override
            public int compare(Face face, Face t1) {
                return Float.compare(face.getWidth() * face.getHeight(), t1.getWidth() * t1.getHeight());
            }
        }).orElse(null);

        if (face == null || face.getWidth() * face.getHeight() <= 0.15 * bitmap.getWidth() * bitmap.getHeight())
            return null;

        Bitmap crop = Bitmap.createBitmap(bitmap, (int) face.getPosition().x, (int) face.getPosition().y, (int) face.getWidth() - (int) face.getPosition().x, (int) face.getHeight() - (int) face.getPosition().y);
        return crop;
    }
}
