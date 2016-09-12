package com.twilio.video.quickstart.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.twilio.common.AccessManager;
import com.twilio.video.quickstart.R;
import com.twilio.video.quickstart.dialog.Dialog;
import com.twilio.video.AudioTrack;
import com.twilio.video.CameraCapturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalMedia;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.Media;
import com.twilio.video.Participant;
import com.twilio.video.Room;
import com.twilio.video.VideoClient;
import com.twilio.video.VideoException;
import com.twilio.video.VideoTrack;
import com.twilio.video.VideoView;

import java.util.Map;

public class VideoActivity extends AppCompatActivity {
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = VideoActivity.class.getName();

    /*
     * You must provide a Twilio AccessToken to connect to the Video service
     */
    private static final String TWILIO_ACCESS_TOKEN = "TWILIO_ACCESS_TOKEN";

    /*
     * Twilio Video Client allows a client to create or connect to a room.
     */
    private VideoClient videoClient;

    /*
     * A Room represents communication between the client and one or more participants.
     */
    private Room room;

    /*
     * A VideoView receives frames from a local or remote video track and renders them
     * to associated view.
     */
    private VideoView primaryVideoView;
    private VideoView thumbnailVideoView;

    /*
     * Android application UI elements
     */
    private TextView videoStatusTextView;
    private AccessManager accessManager;
    private CameraCapturer cameraCapturer;
    private LocalMedia localMedia;
    private LocalAudioTrack localAudioTrack;
    private LocalVideoTrack localVideoTrack;
    private FloatingActionButton connectActionFab;
    private FloatingActionButton switchCameraActionFab;
    private FloatingActionButton localVideoActionFab;
    private FloatingActionButton muteActionFab;
    private android.support.v7.app.AlertDialog alertDialog;
    private AudioManager audioManager;

    private boolean loggingOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        /*
         * Load views from resources
         */
        primaryVideoView = (VideoView) findViewById(R.id.primary_video_view);
        thumbnailVideoView = (VideoView) findViewById(R.id.thumbnail_video_view);
        videoStatusTextView = (TextView) findViewById(R.id.video_status_textview);

        connectActionFab = (FloatingActionButton) findViewById(R.id.connect_action_fab);
        switchCameraActionFab = (FloatingActionButton) findViewById(R.id.switch_camera_action_fab);
        localVideoActionFab = (FloatingActionButton) findViewById(R.id.local_video_action_fab);
        muteActionFab = (FloatingActionButton) findViewById(R.id.mute_action_fab);

        /*
         * Enable changing the volume using the up/down keys during a conversation
         */
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        /*
         * Needed for setting/abandoning audio focus during call
         */
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        /*
         * Check camera and microphone permissions. Needed in Android M.
         */
        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
        } else {
            createLocalMedia();
            /*
             * Create the Twilio Video Client
             */
            createVideoClient();
        }

        /*
         * Set the initial state of the UI
         */
        setConnectAction();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.quickstart_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_log_out:
                /*
                 * All rooms need to be disconnected before tearing down the SDK
                 */
                loggingOut = true;
                if (room != null) {
                    room.disconnect();
                } else {
                    finish();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (primaryVideoView != null) {
            primaryVideoView.release();
            primaryVideoView = null;
        }
        if (thumbnailVideoView != null) {
            thumbnailVideoView.release();
            thumbnailVideoView = null;
        }
        if (localMedia != null) {
            localMedia.removeVideoTrack(localVideoTrack);
            localMedia.removeAudioTrack(localAudioTrack);
            localMedia.release();
            localMedia = null;
        }
        if (accessManager != null) {
            accessManager.dispose();
            accessManager = null;
        }
        if (videoClient != null) {
            videoClient.release();
            videoClient = null;
        }
    }

    private boolean checkPermissionForCameraAndMicrophone(){
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if ((resultCamera == PackageManager.PERMISSION_GRANTED) &&
                (resultMic == PackageManager.PERMISSION_GRANTED)){
            return true;
        } else {
            return false;
        }
    }

    private void requestPermissionForCameraAndMicrophone(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)){
            Toast.makeText(this,
                    R.string.permissions_needed,
                    Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    CAMERA_MIC_PERMISSION_REQUEST_CODE);
        }
    }

    private void createLocalMedia() {
        localMedia = LocalMedia.create(this);
        localAudioTrack = localMedia.addAudioTrack(true);
        cameraCapturer = new CameraCapturer(this,
                CameraCapturer.CameraSource.CAMERA_SOURCE_FRONT_CAMERA, null);
        localVideoTrack = localMedia.addVideoTrack(true, cameraCapturer);
        primaryVideoView.setMirror(true);
        localVideoTrack.addRenderer(primaryVideoView);
    }

    private void createVideoClient() {
        /*
         * First we create VideoClient that allows us to create new rooms or
         * connect to existing ones. The AccessManager manages the lifetime
         * of the access token and notifies the client of token expirations.
         */
        // OPTION 1- Generate an access token from the getting started portal
        // https://www.twilio.com/user/account/video/getting-started
        accessManager = new AccessManager(VideoActivity.this,
                TWILIO_ACCESS_TOKEN,
                accessManagerListener());
        videoClient = new VideoClient(VideoActivity.this, accessManager);
        // OPTION 2- Retrieve an access token from your own web app
        // retrieveAccessTokenfromServer();

    }

    private void connectToRoom(String roomName) {
        setAudioFocus(true);
        ConnectOptions connectOptions = new ConnectOptions.Builder()
                .name(roomName)
                .localMedia(localMedia)
                .build();
        room = videoClient.connect(connectOptions, roomListener());
        setDisconnectAction();
    }

    /*
     * The initial state when there is no active conversation.
     */
    private void setConnectAction() {
        connectActionFab.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_call_white_24px));
        connectActionFab.show();
        connectActionFab.setOnClickListener(connectActionClickListener());
        switchCameraActionFab.show();
        switchCameraActionFab.setOnClickListener(switchCameraClickListener());
        localVideoActionFab.show();
        localVideoActionFab.setOnClickListener(localVideoClickListener());
        muteActionFab.show();
        muteActionFab.setOnClickListener(muteClickListener());
    }

    /*
     * The actions performed during disconnect.
     */
    private void setDisconnectAction() {
        connectActionFab.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_call_end_white_24px));
        connectActionFab.show();
        connectActionFab.setOnClickListener(hangupClickListener());
    }

    /*
     * Creates an connect UI dialog
     */
    private void showConnectDialog() {
        EditText roomEditText = new EditText(this);
        alertDialog = Dialog.createConnectDialog(roomEditText,
                connectClickListener(roomEditText), cancelConnectDialogClickListener(), this);
        alertDialog.show();
    }

    /*
     * Called when participant joins the room
     */
    private void addParticipant(Participant participant) {
        /*
         * This app only displays video for one additional participant per Room
         */
        if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
            Snackbar.make(connectActionFab,
                    "Multiple participants are not currently support in this UI",
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }
        videoStatusTextView.setText("Participant "+participant.getIdentity()+ " joined");
        /*
         * Stop rendering local video track in primary view and move it to thumbnail view
         */
        localVideoTrack.removeRenderer(primaryVideoView);
        thumbnailVideoView.setVisibility(View.VISIBLE);
        localVideoTrack.addRenderer(thumbnailVideoView);
        /*
         * Start listening for participant media events
         */
        participant.getMedia().setListener(createParticipantMediaListener());
    }

    /*
     * Called when participant leaves the room
     */
    private void removeParticipant(Participant participant) {
        videoStatusTextView.setText("Participant "+participant.getIdentity()+ " left.");
        /*
         * Show local video in primary view
         */
        thumbnailVideoView.setVisibility(View.GONE);
        localVideoTrack.removeRenderer(thumbnailVideoView);
        primaryVideoView.setMirror(true);
        localVideoTrack.addRenderer(primaryVideoView);
    }

    /*
     * AccessManager listener
     */
    private AccessManager.Listener accessManagerListener() {
        return new AccessManager.Listener() {
            @Override
            public void onTokenExpired(AccessManager twilioAccessManager) {
                videoStatusTextView.setText("onAccessManagerTokenExpire");

            }

            @Override
            public void onTokenUpdated(AccessManager twilioAccessManager) {
                videoStatusTextView.setText("onTokenUpdated");

            }

            @Override
            public void onError(AccessManager twilioAccessManager, String s) {
                videoStatusTextView.setText("onError");
            }
        };
    }

    /*
     * Room events listener
     */
    private Room.Listener roomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                videoStatusTextView.setText("Connected to " + room.getName());
                setTitle(room.getName());

                for (Map.Entry<String, Participant> entry : room.getParticipants().entrySet()) {
                    addParticipant(entry.getValue());
                    break;
                }
            }

            @Override
            public void onConnectFailure(VideoException e) {
                videoStatusTextView.setText("Failed to connect");
            }

            @Override
            public void onDisconnected(Room room, VideoException e) {
                videoStatusTextView.setText("Disconnected from " + room.getName());
                VideoActivity.this.room = null;
                setAudioFocus(false);
                if (loggingOut) {
                    finish();
                    loggingOut = false;
                } else {
                    setConnectAction();

                    /*
                     * Show local video in primary view
                     */
                    thumbnailVideoView.setVisibility(View.GONE);
                    localVideoTrack.removeRenderer(thumbnailVideoView);
                    primaryVideoView.setMirror(true);
                    localVideoTrack.addRenderer(primaryVideoView);
                }
            }

            @Override
            public void onParticipantConnected(Room room, Participant participant) {
                addParticipant(participant);

            }

            @Override
            public void onParticipantDisconnected(Room room, Participant participant) {
                removeParticipant(participant);
            }
        };
    }

    private Media.Listener createParticipantMediaListener() {
        return new Media.Listener() {

            @Override
            public void onAudioTrackAdded(Media media, AudioTrack audioTrack) {
                videoStatusTextView.setText("onAudioTrackAdded");
            }

            @Override
            public void onAudioTrackRemoved(Media media, AudioTrack audioTrack) {
                videoStatusTextView.setText("onAudioTrackRemoved");
            }

            @Override
            public void onVideoTrackAdded(Media media, VideoTrack videoTrack) {
                videoStatusTextView.setText("onVideoTrackAdded");
                /*
                 * Set primary view as renderer for participant video track
                 */
                primaryVideoView.setMirror(false);
                videoTrack.addRenderer(primaryVideoView);
            }

            @Override
            public void onVideoTrackRemoved(Media media, VideoTrack videoTrack) {
                videoStatusTextView.setText("onVideoTrackRemoved");
                videoTrack.removeRenderer(primaryVideoView);
            }

            @Override
            public void onAudioTrackEnabled(Media media, AudioTrack audioTrack) {

            }

            @Override
            public void onAudioTrackDisabled(Media media, AudioTrack audioTrack) {

            }

            @Override
            public void onVideoTrackEnabled(Media media, VideoTrack videoTrack) {

            }

            @Override
            public void onVideoTrackDisabled(Media media, VideoTrack videoTrack) {

            }
        };
    }

    private DialogInterface.OnClickListener connectClickListener(final EditText roomEditText) {
        return new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                /*
                 * Connect to room
                 */
                connectToRoom(roomEditText.getText().toString());
            }
        };
    }

    private View.OnClickListener hangupClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Disconnect from room
                 */
                if (room != null) {
                    room.disconnect();
                }
                setConnectAction();
            }
        };
    }

    private View.OnClickListener connectActionClickListener() {
        return new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showConnectDialog();
            }
        };
    }

    private DialogInterface.OnClickListener cancelConnectDialogClickListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setConnectAction();
                alertDialog.dismiss();
            }
        };
    }

    private View.OnClickListener switchCameraClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraCapturer != null) {
                    cameraCapturer.switchCamera();
                }
            }
        };
    }

    private View.OnClickListener localVideoClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Enable/disable local video track
                 */
                if (localVideoTrack != null) {
                    boolean enable = !localVideoTrack.isEnabled();
                    if (!localVideoTrack.enable(enable)) {
                        videoStatusTextView.setText("Failed to "+
                                (enable ? "enable" : "disable") + " local video");
                        return;
                    }
                    int icon = 0;
                    if (enable) {
                        icon = R.drawable.ic_videocam_green_24px;
                        switchCameraActionFab.show();
                    } else {
                        icon = R.drawable.ic_videocam_off_red_24px;
                        switchCameraActionFab.hide();
                    }
                    localVideoActionFab.setImageDrawable(
                            ContextCompat.getDrawable(VideoActivity.this, icon));
                }
            }
        };
    }

    private View.OnClickListener muteClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Enable/disable local audio track
                 */
                if (localAudioTrack != null) {
                    boolean enable = !localAudioTrack.isEnabled();
                    if (!localAudioTrack.enable(enable)) {
                        videoStatusTextView.setText("Failed to "+
                                (enable ? "enable" : "disable") + " audio track");
                        return;
                    }
                    int icon = enable ? R.drawable.ic_mic_green_24px :
                            R.drawable.ic_mic_off_red_24px;
                    muteActionFab.setImageDrawable(ContextCompat.getDrawable(
                            VideoActivity.this, icon));
                }
            }
        };
    }

    private void retrieveAccessTokenfromServer() {
        Ion.with(this)
                .load("http://localhost:8000/token.php")
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e == null) {

                            String accessToken = result.get("token").getAsString();

                            accessManager = new AccessManager(VideoActivity.this,
                                    accessToken,
                                    accessManagerListener());
                            videoClient = new VideoClient(VideoActivity.this, accessManager);
                        } else {
                            Toast.makeText(VideoActivity.this,
                                    R.string.error_retrieving_access_token, Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
    }

    private int savedAudioMode = AudioManager.MODE_INVALID;
    private void setAudioFocus(boolean setFocus) {
        if (audioManager != null) {
            if (setFocus) {
                savedAudioMode = audioManager.getMode();
                // Request audio focus before making any device switch.
                audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

                // Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
                // required to be in this mode when playout and/or recording starts for
                // best possible VoIP performance.
                // Some devices have difficulties with speaker mode if this is not set.
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            } else {
                audioManager.setMode(savedAudioMode);
                audioManager.abandonAudioFocus(null);
            }
        }
    }
}
