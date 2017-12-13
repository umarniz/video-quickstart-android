package com.twilio.video.quickstart.kotlin

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.MenuItem
import com.twilio.video.AudioCodec
import com.twilio.video.VideoCodec
import org.webrtc.MediaCodecVideoDecoder
import org.webrtc.MediaCodecVideoEncoder
import java.util.ArrayList

class SettingsActivity : AppCompatActivity() {
    companion object {
        val PREF_AUDIO_CODEC = "audio_codec"
        val PREF_AUDIO_CODEC_DEFAULT = "OPUS"
        val PREF_VIDEO_CODEC = "video_codec"
        val PREF_VIDEO_CODEC_DEFAULT = "VP8"
        val PREF_SENDER_MAX_AUDIO_BITRATE = "sender_max_audio_bitrate"
        val PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT = "0"
        val PREF_SENDER_MAX_VIDEO_BITRATE = "sender_max_video_bitrate"
        val PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT = "0"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val settingsFragment = SettingsFragment.newInstance()
        supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, settingsFragment)
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private val sharedPreferences by lazy {
            PreferenceManager.getDefaultSharedPreferences(activity)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.settings)
            setHasOptionsMenu(true)
            setupCodecListPreference(AudioCodec::class.java,
                    PREF_AUDIO_CODEC,
                    PREF_AUDIO_CODEC_DEFAULT,
                    findPreference(PREF_AUDIO_CODEC) as ListPreference)
            setupCodecListPreference(VideoCodec::class.java,
                    PREF_VIDEO_CODEC,
                    PREF_VIDEO_CODEC_DEFAULT,
                    findPreference(PREF_VIDEO_CODEC) as ListPreference)
            setupSenderBandwidthPreferences(PREF_SENDER_MAX_AUDIO_BITRATE,
                    PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT,
                    findPreference(PREF_SENDER_MAX_AUDIO_BITRATE) as EditTextPreference)
            setupSenderBandwidthPreferences(PREF_SENDER_MAX_VIDEO_BITRATE,
                    PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT,
                    findPreference(PREF_SENDER_MAX_VIDEO_BITRATE) as EditTextPreference)
        }


        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            return when (item.itemId) {
                android.R.id.home -> {
                    startActivity(Intent(activity, SettingsActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }

        private fun <T : Enum<T>> setupCodecListPreference(enumClass: Class<T>,
                                                           key: String,
                                                           defaultValue: String,
                                                           listPreference: ListPreference) {
            // Create codec entries
            val codecEntries = enumClass.enumConstants.mapTo(ArrayList()) { it.toString() }

            // Remove H264 if not supported
            if (!MediaCodecVideoDecoder.isH264HwSupported() ||
                    !MediaCodecVideoEncoder.isH264HwSupported()) {
                codecEntries.remove(VideoCodec.H264.name)
            }

            // Bind value
            val prefValue = sharedPreferences.getString(key, defaultValue)
            val codecStrings = codecEntries.toTypedArray()

            listPreference.apply {
                entries = codecStrings
                entryValues = codecStrings
                value = prefValue
                summary = prefValue
                onPreferenceChangeListener =
                        Preference.OnPreferenceChangeListener { preference, newValue ->
                            preference.summary = newValue.toString()
                            true
                        }
            }

        }

        private fun setupSenderBandwidthPreferences(key: String,
                                                    defaultValue: String,
                                                    editTextPreference: EditTextPreference) {
            val value = sharedPreferences.getString(key, defaultValue)

            // Set layout with input type number for edit text
            editTextPreference.apply {
                dialogLayoutResource = R.layout.preference_dialog_number_edittext
                summary = value
                onPreferenceChangeListener =
                        Preference.OnPreferenceChangeListener { preference, newValue ->
                            preference.summary = newValue.toString()
                            true
                        }
            }

        }

        companion object {
            fun newInstance(): SettingsFragment {
                return SettingsFragment()
            }
        }
    }
}