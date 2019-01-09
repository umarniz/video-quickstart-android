package com.twilio.video.examples.customrenderer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import android.content.Context;
import android.widget.TextView;
import android.app.Activity;

import com.twilio.video.I420Frame;
import com.twilio.video.VideoRenderer;

import tensorflow.demo.Classifier;
import tensorflow.demo.TFLiteImageClassifier;
import tensorflow.demo.env.ImageUtils;

import org.webrtc.RendererCommon;
import org.webrtc.YuvConverter;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.lang.Float;

import static android.graphics.ImageFormat.NV21;

/**
 * SnapshotVideoRenderer demonstrates how to implement a custom {@link VideoRenderer}. Caches the
 * last frame rendered and will update the provided image view any time {@link #takeSnapshot()} is
 * invoked.
 */
public class SnapshotVideoRenderer implements VideoRenderer {
    private final ImageView imageView;
    private final TextView textView;
    private final AtomicBoolean snapshotRequsted = new AtomicBoolean(false);
    private final Handler handler = new Handler(Looper.getMainLooper());

    //TODO: added for TensorFlow Lite
    private static final int INPUT_SIZE = 224;
    private static final String MODEL_FILE = "mobilenet_quant_v1_224.tflite";
    private static final String LABEL_FILE = "labels_mobilenet_quant_v1_224.txt";
    private Classifier classifier;
    private long lastProcessingTimeMs;
    private Activity myActivity;
    private static final boolean SAVE_BITMAP = false;
    private Boolean isProcessing = false;
    private Bitmap croppedBitmap = null;


    public SnapshotVideoRenderer(ImageView imageView, Activity activity, TextView textview) {
        this.imageView = imageView;
        this.myActivity = activity;
        this.textView = textview;
        if (croppedBitmap == null)
            croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
        if (classifier == null)
            classifier = TFLiteImageClassifier.create(myActivity.getAssets(), MODEL_FILE, LABEL_FILE, INPUT_SIZE);
    }

    @Override
    public void renderFrame(final I420Frame i420Frame) {
        // Capture bitmap and post to main thread
        final Bitmap bitmap = i420Frame.yuvPlanes == null ?
                captureBitmapFromTexture(i420Frame) :
                captureBitmapFromYuvFrame(i420Frame);

        handler.post(() -> {
            //pass bitmap to TFLite model
            processAndRecognize(bitmap);
            // Frames must be released after rendering to free the native memory
            i420Frame.release();
        });

    }

    /**
     * Request a snapshot on the rendering thread.
     */
    public void takeSnapshot() {
        snapshotRequsted.set(true);
    }

    private Bitmap captureBitmapFromTexture(I420Frame i420Frame) {
        int width = i420Frame.rotatedWidth();
        int height = i420Frame.rotatedHeight();
        int outputFrameSize = width * height * 3 / 2;
        ByteBuffer outputFrameBuffer = ByteBuffer.allocateDirect(outputFrameSize);
        final float frameAspectRatio = (float) i420Frame.rotatedWidth() /
                (float) i420Frame.rotatedHeight();
        final float[] rotatedSamplingMatrix =
                RendererCommon.rotateTextureMatrix(i420Frame.samplingMatrix,
                        i420Frame.rotationDegree);
        final float[] layoutMatrix = RendererCommon.getLayoutMatrix(false,
                frameAspectRatio,
                (float) width / height);
        final float[] texMatrix = RendererCommon.multiplyMatrices(rotatedSamplingMatrix,
                layoutMatrix);
        /*
         * YuvConverter must be instantiated on a thread that has an active EGL context. We know
         * that renderFrame is called from the correct render thread therefore
         * we defer instantiation of the converter until frame arrives.
         */
        YuvConverter yuvConverter = new YuvConverter();
        yuvConverter.convert(outputFrameBuffer,
                width,
                height,
                width,
                i420Frame.textureId,
                texMatrix);

        // Now we need to unpack the YUV data into planes
        byte[] data = outputFrameBuffer.array();
        int offset = outputFrameBuffer.arrayOffset();
        int stride = width;
        ByteBuffer[] yuvPlanes = new ByteBuffer[] {
                ByteBuffer.allocateDirect(width * height),
                ByteBuffer.allocateDirect(width * height / 4),
                ByteBuffer.allocateDirect(width * height / 4)
        };
        int[] yuvStrides = new int[] {
                width,
                (width + 1) / 2,
                (width + 1) / 2
        };

        // Write Y
        yuvPlanes[0].put(data, offset, width * height);

        // Write U
        for (int r = height ; r < height * 3 / 2; ++r) {
            yuvPlanes[1].put(data, offset + r * stride, stride / 2);
        }

        // Write V
        for (int r = height ; r < height * 3 / 2 ; ++r) {
            yuvPlanes[2].put(data, offset + r * stride + stride / 2, stride / 2);
        }

        // Convert the YuvImage
        YuvImage yuvImage = i420ToYuvImage(yuvPlanes, yuvStrides, width, height);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Rect rect = new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight());

        // Compress YuvImage to jpeg
        yuvImage.compressToJpeg(rect, 100, stream);

        // Convert jpeg to Bitmap
        byte[] imageBytes = stream.toByteArray();

        // Release YUV Converter
        yuvConverter.release();

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private Bitmap captureBitmapFromYuvFrame(I420Frame i420Frame) {
        YuvImage yuvImage = i420ToYuvImage(i420Frame.yuvPlanes,
                i420Frame.yuvStrides,
                i420Frame.width,
                i420Frame.height);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Rect rect = new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight());

        // Compress YuvImage to jpeg
        yuvImage.compressToJpeg(rect, 100, stream);

        // Convert jpeg to Bitmap
        byte[] imageBytes = stream.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Matrix matrix = new Matrix();

        // Apply any needed rotation
        matrix.postRotate(i420Frame.rotationDegree);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                true);

        return bitmap;
    }

    private YuvImage i420ToYuvImage(ByteBuffer[] yuvPlanes,
                                    int[] yuvStrides,
                                    int width,
                                    int height) {
        if (yuvStrides[0] != width) {
            return fastI420ToYuvImage(yuvPlanes, yuvStrides, width, height);
        }
        if (yuvStrides[1] != width / 2) {
            return fastI420ToYuvImage(yuvPlanes, yuvStrides, width, height);
        }
        if (yuvStrides[2] != width / 2) {
            return fastI420ToYuvImage(yuvPlanes, yuvStrides, width, height);
        }

        byte[] bytes = new byte[yuvStrides[0] * height +
                yuvStrides[1] * height / 2 +
                yuvStrides[2] * height / 2];
        ByteBuffer tmp = ByteBuffer.wrap(bytes, 0, width * height);
        copyPlane(yuvPlanes[0], tmp);

        byte[] tmpBytes = new byte[width / 2 * height / 2];
        tmp = ByteBuffer.wrap(tmpBytes, 0, width / 2 * height / 2);

        copyPlane(yuvPlanes[2], tmp);
        for (int row = 0 ; row < height / 2 ; row++) {
            for (int col = 0 ; col < width / 2 ; col++) {
                bytes[width * height + row * width + col * 2]
                        = tmpBytes[row * width / 2 + col];
            }
        }
        copyPlane(yuvPlanes[1], tmp);
        for (int row = 0 ; row < height / 2 ; row++) {
            for (int col = 0 ; col < width / 2 ; col++) {
                bytes[width * height + row * width + col * 2 + 1] =
                        tmpBytes[row * width / 2 + col];
            }
        }
        return new YuvImage(bytes, NV21, width, height, null);
    }

    private YuvImage fastI420ToYuvImage(ByteBuffer[] yuvPlanes,
                                        int[] yuvStrides,
                                        int width,
                                        int height) {
        byte[] bytes = new byte[width * height * 3 / 2];
        int i = 0;
        for (int row = 0 ; row < height ; row++) {
            for (int col = 0 ; col < width ; col++) {
                bytes[i++] = yuvPlanes[0].get(col + row * yuvStrides[0]);
            }
        }
        for (int row = 0 ; row < height / 2 ; row++) {
            for (int col = 0 ; col < width / 2; col++) {
                bytes[i++] = yuvPlanes[2].get(col + row * yuvStrides[2]);
                bytes[i++] = yuvPlanes[1].get(col + row * yuvStrides[1]);
            }
        }
        return new YuvImage(bytes, NV21, width, height, null);
    }

    private void copyPlane(ByteBuffer src, ByteBuffer dst) {
        src.position(0).limit(src.capacity());
        dst.put(src);
        dst.position(0).limit(dst.capacity());
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    private void processAndRecognize(Bitmap srcBitmap) {
//        L.w(getClass(), "PID: " + Thread.currentThread().getId());
        if (isProcessing) {
            return;
        }

        isProcessing = true;

        Bitmap dstBitmap;
        if (srcBitmap.getWidth() >= srcBitmap.getHeight()) {
            dstBitmap = Bitmap.createBitmap(srcBitmap, srcBitmap.getWidth()/2 - srcBitmap.getHeight()/2, 0,
                    srcBitmap.getHeight(), srcBitmap.getHeight()
            );

        } else {
            dstBitmap = Bitmap.createBitmap(srcBitmap, 0, srcBitmap.getHeight()/2 - srcBitmap.getWidth()/2,
                    srcBitmap.getWidth(), srcBitmap.getWidth()
            );
        }

        Matrix frameToCropTransform = ImageUtils.getTransformationMatrix(dstBitmap.getWidth(), dstBitmap.getHeight(),
                INPUT_SIZE, INPUT_SIZE, 0, true);

        Matrix cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(dstBitmap, frameToCropTransform, null);

        //TODO: enable this for analyzing the bitmaps, but it may degrade the performance
        if (SAVE_BITMAP) {
            ImageUtils.saveBitmap(srcBitmap, "remote_raw.png");
            ImageUtils.saveBitmap(dstBitmap, "remote_rawCrop.png");
            ImageUtils.saveBitmap(croppedBitmap, "remote_crop.png");
        }

        runInBackground(() -> {
            //pass bitmap to TFLite model
            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            Float confidence = 0.0f;
            String object = "";
            final StringBuilder builder = new StringBuilder();
            for (Classifier.Recognition result : results) {
                if (result.getConfidence() > confidence) {
                    confidence = result.getConfidence();
                    object = result.getTitle();
                }
            }
            builder.append(object).append(", Confidence:").append(confidence);
            myActivity.runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   SnapshotVideoRenderer.this.textView.setText(builder);
               }
            });

            isProcessing = false;
        });

        // recycle bitmaps
        dstBitmap.recycle();
    }
}
