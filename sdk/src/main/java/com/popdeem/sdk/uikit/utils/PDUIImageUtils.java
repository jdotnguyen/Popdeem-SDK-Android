/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Popdeem
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.popdeem.sdk.uikit.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by mikenolan on 23/02/16.
 */
public class PDUIImageUtils {

    public static final int PD_TAKE_PHOTO_REQUEST_CODE = 999;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String CROPPED_FILE_PREFIX = "cropped_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";


    public static int getOrientation(String photoPath) {
        ExifInterface ei;
        try {
            ei = new ExifInterface(photoPath);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return -1;
        }
    }

    public static Bitmap rotateBitmap(String path, int orientation, BitmapFactory.Options bmOptions) {
        Matrix matrix = new Matrix();
        matrix.setRotate(orientation);

        Bitmap currentBitmap = BitmapFactory.decodeFile(path, bmOptions);
        Bitmap bitmap = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.getWidth(), currentBitmap.getHeight(), matrix, true);
        currentBitmap.recycle();

        return bitmap;
    }

    public static void deletePhotoFile(String path) {
        if (path == null) {
            Log.d(PDUIImageUtils.class.getSimpleName(), "path is null");
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            Log.d(PDUIImageUtils.class.getSimpleName(), "image does exists");
            return;
        }

        Log.d(PDUIImageUtils.class.getSimpleName(), "image exists");
        if (file.delete()) {
            Log.d(PDUIImageUtils.class.getSimpleName(), "image deleted");
        }
    }

    public static File createImageFile(boolean coppedImage) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = coppedImage ? CROPPED_FILE_PREFIX : "" + JPEG_FILE_PREFIX + timeStamp + "_";
//        File folder = new File(context.getFilesDir() + "/popdeem/");
        File albumF = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/popdeem/");
        if (!albumF.exists()) {
            albumF.mkdirs();
        }
        Log.d(PDUIImageUtils.class.getSimpleName(), "photo directory: " + albumF.getAbsolutePath());
        return File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
    }


    public static Bitmap getResizedBitmap(String path, int targetHeight, int targetWidth, int orientation) {
        /* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetWidth > 0) || (targetHeight > 0)) {
            scaleFactor = Math.min(photoW / targetWidth, photoH / targetHeight);
        }

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

		/* Decode the JPEG file into a Bitmap */
        Bitmap bitmap;
        boolean hasOrientation = orientation != -1;
        if (hasOrientation) {
            bitmap = rotateBitmap(path, orientation, bmOptions);
        } else {
            bitmap = BitmapFactory.decodeFile(path, bmOptions);
        }

        return bitmap;
    }

}