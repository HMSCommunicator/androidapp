package org.hmscommunicator.android.aitools;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlplugin.asr.MLAsrCaptureActivity;
import com.huawei.hms.mlplugin.asr.MLAsrCaptureConstants;
import com.huawei.hms.mlsdk.translate.MLTranslatorFactory;
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslateSetting;
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslator;
import com.huawei.hms.mlsdk.tts.MLTtsAudioFragment;
import com.huawei.hms.mlsdk.tts.MLTtsCallback;
import com.huawei.hms.mlsdk.tts.MLTtsConfig;
import com.huawei.hms.mlsdk.tts.MLTtsConstants;
import com.huawei.hms.mlsdk.tts.MLTtsEngine;
import com.huawei.hms.mlsdk.tts.MLTtsError;
import com.huawei.hms.mlsdk.tts.MLTtsWarn;

//import com.huawei.mlkit.example.R;
import org.hmscommunicator.android.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AsrAnalyseFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AsrAnalyseFragment extends Fragment {
    private static final String TAG = AsrAnalyseFragment.class.getSimpleName();
    private static final int HANDLE_CODE = 0;
    private static final String HANDLE_KEY = "text";
    private static final int AUDIO_PERMISSION_CODE = 1;
    private static final int ML_ASR_CAPTURE_CODE = 2;
    private TextView mTextView;
    private MLRemoteTranslateSetting settings;
    private TextView langView;
    private String sourceLangCode;
    private String targetLangCode;
    private String sourceText;
    private String result;

    MLTtsEngine mlTtsEngine;
    MLTtsConfig mlConfigs;

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case HANDLE_CODE:
                    String text = message.getData().getString(HANDLE_KEY);
                    mTextView.setText(text + "\n");
                    Log.e(TAG, text);
                    break;
                default:
                    break;
            }
            return false;
        }
    });

//    // TODO: Rename parameter arguments, choose names that match
//    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
//
//    // TODO: Rename and change types of parameters
//    private String mParam1;
//    private String mParam2;

    public AsrAnalyseFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AsrAnalyseFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AsrAnalyseFragment newInstance(String param1, String param2) {
        AsrAnalyseFragment fragment = new AsrAnalyseFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "requesting permission");
            this.requestCameraPermission();
        }else{
            Log.d(TAG, "permission already granted");
        }
        sourceLangCode = "en";
        targetLangCode = "zh";
        // Method 1: Use the default parameter settings to create a TTS engine.
        // In the default settings, the source language is Chinese, the Chinese female voice is used,
        // the voice speed is 1.0 (1x), and the volume is 1.0 (1x).
        // MLTtsConfig mlConfigs = new MLTtsConfig();
        // Method 2: Use customized parameter settings to create a TTS engine.
        mlConfigs = new MLTtsConfig()
                // Set the text converted from speech to English.
                // MLTtsConstants.TTS_EN_US: converts text to English.
                // MLTtsConstants.TTS_ZH_HANS: converts text to Chinese.
                .setLanguage(MLTtsConstants.TTS_ZH_HANS)
                // Set the English timbre.
                // MLTtsConstants.TTS_SPEAKER_FEMALE_ZH: Chinese female voice.
                // MLTtsConstants.TTS_SPEAKER_MALE_ZH: Chinese male voice.
                .setPerson(MLTtsConstants.TTS_SPEAKER_FEMALE_ZH)
                // Set the speech speed. Range: 0.2–1.8. 1.0 indicates 1x speed.
                .setSpeed(1.0f)
                // Set the volume. Range: 0.2–1.8. 1.0 indicates 1x volume.
                .setVolume(1.0f);
        mlTtsEngine = new MLTtsEngine(mlConfigs);
        // Pass the TTS callback to the TTS engine.
        mlTtsEngine.setTtsCallback(callback);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_asr_analyse, container, false);
        this.mTextView = v.findViewById(R.id.bigTitleView);
        v.findViewById(R.id.voice_input).setOnClickListener(this::onClick);
        v.findViewById(R.id.tts).setOnClickListener(this::onClick);
//        v.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
//                Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.view1);
//                fragmentTransaction.remove(currentFragment);
//                fragmentTransaction.show(getTargetFragment()).commit();
//
//            }
//        });
        langView = v.findViewById(R.id.textView2);
        return v;
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};
        if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(getActivity(), permissions, AsrAnalyseFragment.AUDIO_PERMISSION_CODE);
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != AsrAnalyseFragment.AUDIO_PERMISSION_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
    }

    private void displayResult(String str) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putString(HANDLE_KEY, str);
        msg.setData(data);
        msg.what = HANDLE_CODE;
        handler.sendMessage(msg);
    }



    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.voice_input:
                asr(v);
                break;
            case R.id.tts:
                tts(v);
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String text = "";
        if (null == data) {
            displayResult("Intent data is null.");
        }
        // ML_ASR_CAPTURE_CODE: request code between the current activity and speech pickup UI activity.
        if (requestCode == ML_ASR_CAPTURE_CODE) {
            switch (resultCode) {
                // MLAsrCaptureConstants.ASR_SUCCESS: Recognition is successful.
                case MLAsrCaptureConstants.ASR_SUCCESS:
                    if (data != null) {
                        Bundle bundle = data.getExtras();
                        // Obtain the text information recognized from speech.
                        if (bundle != null && bundle.containsKey(MLAsrCaptureConstants.ASR_RESULT)) {
                            text = bundle.getString(MLAsrCaptureConstants.ASR_RESULT);
                        }
                        if (text == null || "".equals(text)) {
                            text = "Result is null.";
                        }
                        // Process the recognized text information.
                        //displayResult(text);
                        sourceText = text;
                        settings = new MLRemoteTranslateSetting.Factory()
                                .setSourceLangCode(sourceLangCode)
                                .setTargetLangCode(targetLangCode)
                                .create();
                        MLRemoteTranslator mlRemoteTranslator = MLTranslatorFactory.getInstance().getRemoteTranslator(settings);
                        // Translate text.
                        // sourceText: text to be translated.
                        final Task<String> task = mlRemoteTranslator.asyncTranslate(text);
                        task.addOnSuccessListener(new OnSuccessListener<String>() {
                            public void onSuccess(String text) {
                                // Processing logic for success.
                                result = text;
                                displayResult(sourceText + "\n" + text);
                                mlTtsEngine = new MLTtsEngine(mlConfigs);
                                mlTtsEngine.setTtsCallback(callback);
                                String id = mlTtsEngine.speak(result, MLTtsEngine.QUEUE_APPEND);
                                //mTextView.setText(text);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            public void onFailure(Exception e) {
                                // Processing logic for failure.
                                displayResult("Oops, problem happened. " + e.toString());
                            }
                        });
                        // Release resources.
                        mlRemoteTranslator.stop();
                    }
                    break;
                // MLAsrCaptureConstants.ASR_FAILURE: Recognition fails.
                case MLAsrCaptureConstants.ASR_FAILURE:
                    if (data != null) {
                        Bundle bundle = data.getExtras();
                        // Check whether a result code is contained.
                        if (bundle != null && bundle.containsKey(MLAsrCaptureConstants.ASR_ERROR_CODE)) {
                            text = text + bundle.getInt(MLAsrCaptureConstants.ASR_ERROR_CODE);
                        }
                        // Check whether error information is contained.
                        if (bundle != null && bundle.containsKey(MLAsrCaptureConstants.ASR_ERROR_MESSAGE)) {
                            String errorMsg = bundle.getString(MLAsrCaptureConstants.ASR_ERROR_MESSAGE);
                            if (errorMsg != null && !"".equals(errorMsg)) {
                                text = "[" + text + "]" + errorMsg;
                            }
                        }
                    }
                    displayResult("ASR failed. " + text);
                default:
                    displayResult("Failure.");
                    break;
            }
        }
    }

    public void asr(View view) {
        // If you want to use ASR, you need to apply for an agconnect-services.json file in the developer
        // alliance(https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/ml-add-agc),
        // replacing the sample-agconnect-services.json in the project.
        // Use Intent for recognition settings.
        if (sourceLangCode == "en") {
            Intent intent = new Intent(getActivity(), MLAsrCaptureActivity.class)
                    // Set the language that can be recognized to English. If this parameter is not set,
                    // English is recognized by default. Example: "zh": Chinese or "en-US": English
                    .putExtra(MLAsrCaptureConstants.LANGUAGE, "en-US")
                    // Set whether to display text on the speech pickup UI. MLAsrCaptureConstants.FEATURE_ALLINONE: no;
                    // MLAsrCaptureConstants.FEATURE_WORDFLUX: yes.
                    .putExtra(MLAsrCaptureConstants.FEATURE, MLAsrCaptureConstants.FEATURE_WORDFLUX);
            // ML_ASR_CAPTURE_CODE: request code between the current activity and speech pickup UI activity.
            // You can use this code to obtain the processing result of the speech pickup UI.
            startActivityForResult(intent, ML_ASR_CAPTURE_CODE);
        }else if (sourceLangCode == "zh"){
            Intent intent = new Intent(getActivity(), MLAsrCaptureActivity.class)
                    // Set the language that can be recognized to English. If this parameter is not set,
                    // English is recognized by default. Example: "zh": Chinese or "en-US": English
                    .putExtra(MLAsrCaptureConstants.LANGUAGE, "zh")
                    // Set whether to display text on the speech pickup UI. MLAsrCaptureConstants.FEATURE_ALLINONE: no;
                    // MLAsrCaptureConstants.FEATURE_WORDFLUX: yes.
                    .putExtra(MLAsrCaptureConstants.FEATURE, MLAsrCaptureConstants.FEATURE_WORDFLUX);
            // ML_ASR_CAPTURE_CODE: request code between the current activity and speech pickup UI activity.
            // You can use this code to obtain the processing result of the speech pickup UI.
            startActivityForResult(intent, ML_ASR_CAPTURE_CODE);
        }
    }

    public void tts(View view) {
        if (mTextView.getContext() == null) {
            Toast.makeText(getActivity().getApplicationContext(), "Please say something before play it!", Toast.LENGTH_SHORT).show();
        }else {
            mlTtsEngine = new MLTtsEngine(mlConfigs);
            mlTtsEngine.setTtsCallback(callback);
            String id = mlTtsEngine.speak(result, MLTtsEngine.QUEUE_APPEND);
            //displayResult("TaskID: " + id + " submit.");
        }
    }

    /**
     * TTS callback function.. If you want to use TTS,
     * you need to apply for an agconnect-services.json file in the developer
     * alliance(https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/ml-add-agc),
     * replacing the sample-agconnect-services.json in the project.
     */
    MLTtsCallback callback = new MLTtsCallback() {
        @Override
        public void onError(String taskId, MLTtsError err) {
            // Processing logic for TTS failure.
            String str = "TaskID: " + taskId + ", error:" + err;
            displayResult("Error!! " + str);
        }

        @Override
        public void onWarn(String taskId, MLTtsWarn warn) {
            // Alarm handling without affecting service logic.
            String str = "TaskID: " + taskId + ", warn:" + warn;
            displayResult("Warning!! " + str);
        }

        @Override
        public void onRangeStart(String taskId, int start, int end) {
            // Process the mapping between the currently played segment and text.
            String str = "TaskID: " + taskId + ", onRangeStart [" + start + "，" + end + "]";
            //displayResult(str);
        }

        @Override
        public void onAudioAvailable(String s, MLTtsAudioFragment mlTtsAudioFragment, int i, Pair<Integer, Integer> pair, Bundle bundle) {
            //  Audio stream callback API, which is used to return the synthesized audio data to the app.
            //  taskId: ID of an audio synthesis task corresponding to the audio.
            // audioFragment: audio data.
            // offset: offset of the audio segment to be transmitted in the queue. One audio synthesis task corresponds to an audio synthesis queue.
            //  range: text area where the audio segment to be transmitted is located; range.first (included): start position; range.second (excluded): end position.
        }

        @Override
        // Callback method of a TTS event. eventName: event name. The events are as follows:
        // MLTtsConstants.EVENT_PLAY_RESUME: playback resumption.
        // MLTtsConstants.EVENT_PLAY_PAUSE: playback pause.
        // MLTtsConstants.EVENT_PLAY_STOP: playback stop.
        public void onEvent(String taskId, int eventId, Bundle bundle) {
            String str = "TaskID: " + taskId + ", eventName:" + eventId;
            // Callback method of an audio synthesis event. eventId: event name.
            switch (eventId) {
                case MLTtsConstants.EVENT_PLAY_START:
                    //  Called when playback starts.
                    break;
                case MLTtsConstants.EVENT_PLAY_STOP:
                    // Called when playback stops.
                    boolean isInterrupted = bundle.getBoolean(MLTtsConstants.EVENT_PLAY_STOP_INTERRUPTED);
                    str += " " + isInterrupted;
                    break;
                case MLTtsConstants.EVENT_PLAY_RESUME:
                    //  Called when playback resumes.
                    break;
                case MLTtsConstants.EVENT_PLAY_PAUSE:
                    // Called when playback pauses.
                    break;

                /*//Pay attention to the following callback events when you focus on only synthesized audio data but do not use the internal player for playback:
                case MLTtsConstants.EVENT_SYNTHESIS_START:
                    //  Called when TTS starts.
                    break;
                case MLTtsConstants.EVENT_SYNTHESIS_END:
                    // Called when TTS ends.
                    break;
                case MLTtsConstants.EVENT_SYNTHESIS_COMPLETE:
                    // TTS is complete. All synthesized audio streams are passed to the app.
                    boolean isInterrupted = bundle.getBoolean(MLTtsConstants.EVENT_SYNTHESIS_INTERRUPTED);
                    break;*/
                default:
                    break;
            }

            //displayResult(str);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mlTtsEngine != null) {
            this.mlTtsEngine.shutdown();
        }
    }
}