package com.twilio.video.examples.advancedcameracapturer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.twilio.video.CameraCapturer;
import com.twilio.video.CameraParameterUpdater;
import com.twilio.video.LocalMedia;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.VideoView;

/**
 * This example demonstrates advanced use cases of {@link com.twilio.video.CameraCapturer}. Current
 * use cases shown are as follows:
 *
 * <ol>
 *     <li>Setting Custom {@link android.hardware.Camera.Parameters}</li>
 * </ol>
 */
public class AdvancedCameraCapturerActivity extends Activity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private LocalMedia localMedia;
    private VideoView videoView;
    private Button toggleFlashButton;
    private CameraCapturer cameraCapturer;
    private LocalVideoTrack localVideoTrack;
    private boolean flashOn = false;
    private final View.OnClickListener toggleFlashButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleFlash();
        }
    };

    /**
     * An example of a {@link CameraParameterUpdater} that shows how to toggle the flash of a
     * camera if supported by the device.
     */
    private final CameraParameterUpdater flashToggler = new CameraParameterUpdater() {
        @Override
        public void apply(Camera.Parameters parameters) {
            if (parameters.getFlashMode() != null) {
                String flashMode = flashOn ?
                        Camera.Parameters.FLASH_MODE_OFF :
                        Camera.Parameters.FLASH_MODE_TORCH;
                parameters.setFlashMode(flashMode);
                flashOn = !flashOn;
            } else {
                Toast.makeText(AdvancedCameraCapturerActivity.this,
                        R.string.flash_not_supported,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_camera_capturer);

        localMedia = LocalMedia.create(this);
        videoView = (VideoView) findViewById(R.id.video_view);
        toggleFlashButton = (Button) findViewById(R.id.toggle_flash_button);

        if (!checkPermissionForCamera()) {
            requestPermissionForCamera();
        } else {
            addCameraVideo();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            boolean cameraPermissionGranted = true;

            for (int grantResult : grantResults) {
                cameraPermissionGranted &= grantResult == PackageManager.PERMISSION_GRANTED;
            }

            if (cameraPermissionGranted) {
                addCameraVideo();
            } else {
                Toast.makeText(this,
                        R.string.permissions_needed,
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override protected void onDestroy() {
        localVideoTrack.removeRenderer(videoView);
        localMedia.removeVideoTrack(localVideoTrack);
        localMedia.release();
        super.onDestroy();
    }

    private boolean checkPermissionForCamera(){
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        return resultCamera == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionForCamera(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST_CODE);
    }

    private void addCameraVideo() {
        cameraCapturer = new CameraCapturer(this, CameraCapturer.CameraSource.BACK_CAMERA);
        localVideoTrack = localMedia.addVideoTrack(true, cameraCapturer);
        localVideoTrack.addRenderer(videoView);
        toggleFlashButton.setOnClickListener(toggleFlashButtonClickListener);
    }

    private void toggleFlash() {
        // Request an update to camera parameters with flash toggler
        cameraCapturer.updateCameraParameters(flashToggler);
    }
}
