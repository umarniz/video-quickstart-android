package com.twilio.conversations.quickstart.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.twilio.common.AccessManager;
import com.twilio.conversations.quickstart.dialog.Dialog;
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
import com.twilio.conversations.quickstart.R;

import java.util.Map;

public class VideoActivity extends AppCompatActivity {
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = VideoActivity.class.getName();

    /*
     * You must provide a Twilio AccessToken to connect to the Conversations service
     */
    //private static final String TWILIO_ACCESS_TOKEN = "TWILIO_ACCESS_TOKEN";
    private static final String TWILIO_ACCESS_TOKEN = "eyJhbGciOiAiSFMyNTYiLCAidHlwIjogIkpXVCIsICJjdHkiOiAidHdpbGlvLWZwYTt2PTEifQ.eyJpc3MiOiAiU0s2NzRiMTg4NjlmMTFmYWNjNjY1YTY1ZmQ0ZGRmMmY0ZiIsICJncmFudHMiOiB7InJ0YyI6IHsiY29uZmlndXJhdGlvbl9wcm9maWxlX3NpZCI6ICJWUzNmNzVlMGYxNGU3YzhiMjA5MzhmYzUwOTJlODJmMjNhIn0sICJpZGVudGl0eSI6ICJqb2phIn0sICJqdGkiOiAiU0s2NzRiMTg4NjlmMTFmYWNjNjY1YTY1ZmQ0ZGRmMmY0Zi0xNDczNDU0Nzg5IiwgInN1YiI6ICJBQzk2Y2NjOTA0NzUzYjMzNjRmMjQyMTFlOGQ5NzQ2YTkzIiwgImV4cCI6IDE0NzM0NjQ3ODh9.GBv-7ViFGYR-CyU0h35jIDr3ySqvK-gTVOCy_47zab4";

    /*
     * Twilio Conversations Client allows a client to create or participate in a conversation.
     */
    private VideoClient videoClient;

    /*
     * A Conversation represents communication between the client and one or more participants.
     */
    private Room room;

    /*
     * An OutgoingInvite represents an invitation to start or join a conversation with one or
     * more participants
     */
    //private OutgoingInvite outgoingInvite;

    /*
     * A VideoViewRenderer receives frames from a local or remote video track and renders
     * the frames to a provided view
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
    private FloatingActionButton joinRoomActionFab;
    private FloatingActionButton switchCameraActionFab;
    private FloatingActionButton localVideoActionFab;
    private FloatingActionButton muteActionFab;
    private FloatingActionButton speakerActionFab;
    private android.support.v7.app.AlertDialog alertDialog;
    private AudioManager audioManager;

    private boolean muteMicrophone;
    private boolean pauseVideo;

    private boolean wasPreviewing;
    private boolean wasLive;

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

        joinRoomActionFab = (FloatingActionButton) findViewById(R.id.call_action_fab);
        switchCameraActionFab = (FloatingActionButton) findViewById(R.id.switch_camera_action_fab);
        localVideoActionFab = (FloatingActionButton) findViewById(R.id.local_video_action_fab);
        muteActionFab = (FloatingActionButton) findViewById(R.id.mute_action_fab);
        speakerActionFab = (FloatingActionButton) findViewById(R.id.speaker_action_fab);

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
             * Initialize the Twilio Conversations SDK
             */
            createVideoClient();
        }

        /*
         * Set the initial state of the UI
         */
        setCallAction();

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
         * Now that the SDK is initialized we create a ConversationsClient and
         * register for incoming calls. The TwilioAccessManager manages the lifetime
         * of the access token and notifies the client of token expirations.
         */
        // OPTION 1- Generate an access token from the getting started portal https://www.twilio.com/user/account/video/getting-started
        accessManager = new AccessManager(VideoActivity.this,
                TWILIO_ACCESS_TOKEN,
                accessManagerListener());
        videoClient = new VideoClient(VideoActivity.this, accessManager);
        // OPTION 2- Retrieve an access token from your own web app
//      retrieveAccessTokenfromServer();

    }

    private void connectToRoom(String roomName) {
        //setAudioFocus(true);
        ConnectOptions connectOptions = new ConnectOptions.Builder()
                .name(roomName)
                .localMedia(localMedia)
                .build();
        room = videoClient.connect(connectOptions, createRoomListener());

    }

    /*
     * The initial state when there is no active conversation.
     */
    private void setCallAction() {
        joinRoomActionFab.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_call_white_24px));
        joinRoomActionFab.show();
        joinRoomActionFab.setOnClickListener(joinRoomActionClickListener());
        switchCameraActionFab.show();
        switchCameraActionFab.setOnClickListener(switchCameraClickListener());
        localVideoActionFab.show();
        localVideoActionFab.setOnClickListener(localVideoClickListener());
        muteActionFab.show();
        muteActionFab.setOnClickListener(muteClickListener());
        speakerActionFab.hide();
    }

    /*
     * Creates an outgoing conversation UI dialog
     */
    private void showCreateRoomDialog() {
        EditText roomEditText = new EditText(this);
        alertDialog = Dialog.createCreateRoomsDialog(roomEditText,
                createRoomClickListener(roomEditText), cancelRoomDialogClickListener(), this);
        alertDialog.show();
    }



    private void addParticipant(Participant participant) {
        // TODO support multiple participants
        if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Do not support multiple participants yet", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        videoStatusTextView.setText("Participant "+participant.getIdentity()+ " joined");
        localVideoTrack.removeRenderer(primaryVideoView);
        thumbnailVideoView.setVisibility(View.VISIBLE);
        localVideoTrack.addRenderer(thumbnailVideoView);
        participant.getMedia().setListener(createParticipantMediaListener());
    }

    private void removeParticipant(Participant participant) {
        videoStatusTextView.setText("Participant "+participant.getIdentity()+ " left.");
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
    private Room.Listener createRoomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                videoStatusTextView.setText("Connected to " + room.getName());

                for (Map.Entry<String, Participant> entry : room.getParticipants().entrySet()) {
                    addParticipant(entry.getValue());
                    // TODO just grabbing first participant...need to support multiple participants
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
                setCallAction();
                // TODO: set local participant media to primary view
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

    private DialogInterface.OnClickListener createRoomClickListener(final EditText roomEditText) {
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

    private View.OnClickListener joinRoomActionClickListener() {
        return new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showCreateRoomDialog();
            }
        };
    }

    private DialogInterface.OnClickListener cancelRoomDialogClickListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setCallAction();
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

            }
        };
    }

    private View.OnClickListener muteClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        };
    }
}
