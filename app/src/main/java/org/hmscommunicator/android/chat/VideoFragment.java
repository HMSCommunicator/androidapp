package org.hmscommunicator.android.chat;


import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.huawei.camera.camerakit.Metadata;
import com.huawei.camera.camerakit.Mode;
import com.huawei.camera.camerakit.ModeStateCallback;
import com.huawei.camera.camerakit.RequestKey;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;
import org.hmscommunicator.android.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VideoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private static final String TAG = VideoFragment.class.getSimpleName();

    private static final int OPEN_CAEMERA_TIME_OUT = 2500;

    private static final long PREVIEW_SURFACE_READY_TIMEOUT = 5000L;

    private static final String PIVOT = " ";

    private static final int VIDEO_ENCODING_BIT_RATE = 10000000;

    private static final int VIDEO_FRAME_RATE = 30;

    private Surface mVideoSurface;

    private CameraKitHelper.RecordState mRecordState;

    private Semaphore mStartStopRecordLock = new Semaphore(1);

    private int mSensorOrientation;

    private MediaRecorder mMediaRecorder;

    private String mVideoFile = "";

    private Size mRecordSize;

    private Button mButtonVideo;

    private Button mButtonRecordPause;

    private OrientationEventListener mOrientationListener;


    private boolean mIsFirstRecord = true;

    CameraKitActivity cameraKitActivity;

    private final View.OnClickListener mVideoClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View vi) {
            cameraKitActivity.mCameraKitHandler.post(() -> {
                if (mRecordState == CameraKitHelper.RecordState.IDLE) {
                    startRecord();
                } else if (mRecordState == CameraKitHelper.RecordState.RECORDING) {
                    stopRecord();
                    getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(mVideoFile))));
                } else {
                    Log.d(TAG, "No new command issued!");
                }
            });
        }
    };

    private final View.OnClickListener mVideoPauseAndResumeListener = new View.OnClickListener() {
        @Override
        public void onClick(View vi) {
            Log.d(TAG, "onClick: video");
            cameraKitActivity.mCameraKitHandler.post(() -> {
                if (mRecordState == CameraKitHelper.RecordState.RECORDING) {
                    pauseRecord();
                } else if (mRecordState == CameraKitHelper.RecordState.PAUSED) {
                    resumeRecord();
                } else {
                    Log.d(TAG, "not in idele or RECORDING state");
                }
            });
        }
    };

    private final ModeStateCallback mModeStateCallback = new ModeStateCallback() {
        @Override
        public void onCreated(Mode mode) {
            cameraKitActivity.mCameraOpenCloseLock.release();
            cameraKitActivity.mMode = mode;
            cameraKitActivity.modeConfigBuilder = cameraKitActivity.mMode.getModeConfigBuilder();
            activeVideoModePreview();
        }

        @Override
        public void onCreateFailed(String cameraId, int modeType, int errorCode) {
            Log.d(TAG,
                    "mModeStateCallback onCreateFailed with errorCode: " + errorCode + " and with cameraId: " + cameraId);
            cameraKitActivity.mCameraOpenCloseLock.release();
        }

        @Override
        public void onConfigured(Mode mode) {
            cameraKitActivity.mMode.startPreview();
            getActivity().runOnUiThread(() -> {
                if ((cameraKitActivity.mTextureView == null) || (cameraKitActivity.mPreviewSize == null)) {
                    return;
                }
                CameraKitHelper.configureTransform(getActivity(), cameraKitActivity.mTextureView, cameraKitActivity.mPreviewSize,
                        new Size(cameraKitActivity.mTextureView.getWidth(), cameraKitActivity.mTextureView.getHeight()));
                mButtonVideo.setVisibility(View.VISIBLE);
                mButtonVideo.setEnabled(true);
            });

        }


        @Override
        public void onConfigureFailed(Mode mode, int errorCode) {
            Log.d(TAG, "mModeStateCallback onConfigureFailed with cameraId: " + mode.getCameraId());
            cameraKitActivity.mCameraOpenCloseLock.release();
        }

        @Override
        public void onReleased(Mode mode) {
            Log.d(TAG, "mModeStateCallback onModeReleased: ");
            cameraKitActivity.mCameraOpenCloseLock.release();
        }

        @Override
        public void onFatalError(Mode mode, int errorCode) {
            Log.d(TAG, "mModeStateCallback onFatalError with errorCode: " + errorCode + " and with cameraId: "
                    + mode.getCameraId());
            cameraKitActivity.mCameraOpenCloseLock.release();
            getActivity().finish();
        }
    };

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                    Log.d(TAG, "onSurfaceTextureAvailable: " + new Size(width, height));
                    cameraKitActivity.mCameraKitHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            startCamerakit();
                        }
                    });
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                    Log.d(TAG, "onSurfaceTextureSizeChanged: " + new Size(width, height) + " PreviewSize:" + cameraKitActivity.mPreviewSize);
                    if ((cameraKitActivity.mTextureView == null) || (cameraKitActivity.mPreviewSize == null)) {
                        return;
                    }
                    CameraKitHelper.configureTransform(getActivity(), cameraKitActivity.mTextureView, cameraKitActivity.mPreviewSize,
                            new Size(width, height));
                    cameraKitActivity.mPreviewSurfaceChangedDone.open();
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture texture) {

                }
            };

    public VideoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment VideoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static VideoFragment newInstance(String param1, String param2) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_video, container, false);
        cameraKitActivity = (CameraKitActivity) getActivity();
        getActivity().runOnUiThread(() -> {
            mButtonVideo = v.findViewById(R.id.video);
            mButtonVideo.setOnClickListener(mVideoClickListener);
            mButtonRecordPause = v.findViewById(R.id.recordPause);
            mButtonRecordPause.setOnClickListener(mVideoPauseAndResumeListener);
            initAiMovieSpinner(v);
        });

//        mOrientationListener = new OrientationEventListener(this) {
//            @Override
//            public void onOrientationChanged(int orientation) {
//                String[] cameraLists = cameraKitSuperNightCaptureActivity.mCameraKit.getCameraIdList();
//                //Log.d(TAG, "onOrientationChanged: " + orientation);
//                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
//                    return; // 手机平放时，检测不到有效的角度
//                }
//                // 只检测是否有四个角度的改变
//                if (cameraKitSuperNightCaptureActivity.mMode != null) {
//                    if (orientation > 350 || orientation < 10) {
//                        // 0度：手机默认竖屏状态（home键在正下方)
//                        if (cameraKitSuperNightCaptureActivity.mMode.getCameraId() == cameraLists[0]) {
//                            cameraKitSuperNightCaptureActivity.mMode.setImageRotation(90);
//                        }else {
//                            mMode.setImageRotation(180);
//                        }
//                    } else if (orientation > 80 && orientation < 100) {
//                        // 90度：手机顺时针旋转90度横屏（home建在左侧）
//                        mMode.setImageRotation(180);
//                        mButtonCaptureImage.setRotation(270);
//                        mButtonGallary.setRotation(270);
//                        mButtonSwitchCamera.setRotation(270);
//                    } else if (orientation > 170 && orientation < 190) {
//                        // 180度：手机顺时针旋转180度竖屏（home键在上方）
//                        if (mMode.getCameraId() == cameraLists[0]) {
//                            mMode.setImageRotation(270);
//                        }else {
//                            mMode.setImageRotation(90);
//                        }
//                        mButtonCaptureImage.setRotation(180);
//                        mButtonGallary.setRotation(180);
//                        mButtonSwitchCamera.setRotation(180);
//                    } else if (orientation > 260 && orientation < 280) {
//                        // 270度：手机顺时针旋转270度横屏，（home键在右侧）
//                        mMode.setImageRotation(360);
//                        mButtonCaptureImage.setRotation(90);
//                        mButtonGallary.setRotation(90);
//                        mButtonSwitchCamera.setRotation(90);
//                    }
//                }
//            }
//        };
//        mOrientationListener.enable();

        return v;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!PermissionHelper.hasPermission(getActivity())) {
            PermissionHelper.requestPermission(getActivity());
            return;
        } else {
            if (!cameraKitActivity.initCameraKit()) {
                cameraKitActivity.showAlertWarning(getString(R.string.warning_str));
                return;
            }
        }
        cameraKitActivity.startBackgroundThread();
        CameraKitHelper.initStoreDir(getActivity().getApplicationContext());
        CameraKitHelper.checkImageDirectoryExists();
        if (cameraKitActivity.mTextureView != null) {
            Log.d(TAG, "onResume: setSurfaceTextureListener: ");
            cameraKitActivity.mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            //cameraKitSuperNightCaptureActivity.initCameraKit();
            if (cameraKitActivity.mTextureView.isAvailable()) {
                Log.d(TAG, "onResume startCamerakit");
                cameraKitActivity.mCameraKitHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        startCamerakit();
                    }
                });
            }
        }
    }

    @Override
    public void onPause() {
        if (cameraKitActivity.mMode != null) {
            cameraKitActivity.mCameraKitHandler.post(new Runnable() {
                @Override
                public void run() {
//                    try {
//                        cameraKitSuperNightCaptureActivity.mCameraOpenCloseLock.acquire();
//                        cameraKitSuperNightCaptureActivity.mMode.release();
//                        cameraKitSuperNightCaptureActivity.mMode = null;
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
//                    } finally {
//                        Log.d(TAG, "closeMode:------ ");
//                        cameraKitSuperNightCaptureActivity.mCameraOpenCloseLock.release();
//                    }
                    releaseMediaRecorder();
                }
            });
            if (mIsFirstRecord) {
                clearInvalidFile();
            }
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        //cameraKitSuperNightCaptureActivity.stopBackgroundThread();
    }

    private void activeVideoModePreview() {
        List<Size> previewSizes = cameraKitActivity.mModeCharacteristics.getSupportedPreviewSizes(SurfaceTexture.class);
        List<Size> recordSizes = getSupportedVideoSizes();
        mRecordSize = Collections.max(recordSizes, new CameraKitHelper.CompareSizesByArea());
        Size previewSize = CameraKitHelper.getOptimalVideoPreviewSize(getActivity(), mRecordSize, previewSizes);
        if (previewSize == null) {
            Log.d(TAG, "activeVideoModePreview: preview size is null");
            return;
        }
        getActivity().runOnUiThread(() -> {
            cameraKitActivity.mTextureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
        });
        if ((cameraKitActivity.mPreviewSize == null) || ((previewSize.getHeight() != cameraKitActivity.mPreviewSize.getHeight())
                || (previewSize.getWidth() != cameraKitActivity.mPreviewSize.getWidth()))) {
            Log.e(TAG, "activeVideoModePreview: mPreviewSurfaceChangedDone start:" + previewSize);
            cameraKitActivity.mPreviewSize = previewSize;
            cameraKitActivity.mPreviewSurfaceChangedDone.block(PREVIEW_SURFACE_READY_TIMEOUT);
        } else {
            cameraKitActivity.mPreviewSize = previewSize;
        }
        SurfaceTexture texture = cameraKitActivity.mTextureView.getSurfaceTexture();
        if (texture == null) {
            Log.e(TAG, "activeVideoModePreview: texture=null!");
            return;
        }
        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = new Surface(texture);
        cameraKitActivity.modeConfigBuilder.addPreviewSurface(surface);
        mVideoSurface = MediaCodec.createPersistentInputSurface();
        setUpMediaRecorder(mRecordSize, mVideoSurface);
        mRecordState = CameraKitHelper.RecordState.IDLE;
        mIsFirstRecord = true;
        cameraKitActivity.modeConfigBuilder.addVideoSurface(mVideoSurface);
        cameraKitActivity.mMode.configure();
    }

    private List<Size> getSupportedVideoSizes() {
        Map<Integer, List<Size>> videoSizes = cameraKitActivity.mModeCharacteristics.getSupportedVideoSizes(MediaRecorder.class);
        if ((videoSizes != null) && videoSizes.containsKey(Metadata.FpsRange.HW_FPS_30)) {
            return videoSizes.get(Metadata.FpsRange.HW_FPS_30);
        }
        return new ArrayList<>(0);
    }

    private void setUpMediaRecorder(Size size, Surface surface) {
        mMediaRecorder.reset();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mVideoFile = CameraKitHelper.getVideoName();
        mMediaRecorder.setOutputFile(mVideoFile);
        mMediaRecorder.setVideoEncodingBitRate(VIDEO_ENCODING_BIT_RATE);
        mMediaRecorder.setVideoFrameRate(VIDEO_FRAME_RATE);
        mMediaRecorder.setVideoSize(size.getWidth(), size.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = cameraKitActivity.getWindowManager().getDefaultDisplay().getRotation();
        mMediaRecorder.setOrientationHint(CameraKitHelper.getOrientation(mSensorOrientation, rotation));
        mMediaRecorder.setInputSurface(surface);
        try {
            mMediaRecorder.prepare();
            Log.d(TAG, "mMediaRecorder prepare done!");
        } catch (IOException e) {
            Log.e(TAG, "mMediaRecorder prepare ioe exception " + e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "mMediaRecorder prepare state error");
        }
    }

    private void initAiMovieSpinner(View v) {
        Byte[] ranges = new Byte[0];
        List<CaptureRequest.Key<?>> parameters = cameraKitActivity.mModeCharacteristics.getSupportedParameters();
        if ((parameters != null) && (parameters.contains(RequestKey.HW_AI_MOVIE))) {
            List<Byte> lists = cameraKitActivity.mModeCharacteristics.getParameterRange(RequestKey.HW_AI_MOVIE);
            ranges = new Byte[lists.size()];
            lists.toArray(ranges);
        }
        initSpinner(v, R.id.aiMovieSpinner, byteToList(ranges, R.string.aiMovie), new SpinnerOperation() {
            @Override
            public void doOperation(String text) {
                try {
                    cameraKitActivity.mMode.setParameter(RequestKey.HW_AI_MOVIE, Byte.valueOf(text.split(PIVOT)[1]));
                } catch (PatternSyntaxException e) {
                    Log.e(TAG, "NumberFormatException text: " + text);
                }
            }
        });
    }

    private void initSpinner(View v, int resId, List<String> list, final SpinnerOperation operation) {
        final Spinner spinner = v.findViewById(resId);
        spinner.setVisibility(View.VISIBLE);
        if (list.size() == 0) {
            spinner.setVisibility(View.GONE);
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.item, R.id.itemText, list);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String text = spinner.getItemAtPosition(position).toString();
                cameraKitActivity.mCameraKitHandler.post(() -> {
                    operation.doOperation(text);
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            Log.v(TAG, "Releasing media recorder.");
            try {
                mMediaRecorder.reset();
            } catch (IllegalStateException e) {
                Log.e(TAG, "media recorder maybe has been released! msg=" + e.getMessage());
            }
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    /**
     * start recording
     */
    private void startRecord() {
        try {
            acquiremStartStopRecordLock();
            mRecordState = CameraKitHelper.RecordState.PRE_PROCESS;
            if (!mIsFirstRecord) {
                setUpMediaRecorder(mRecordSize, mVideoSurface);
            }
            mIsFirstRecord = false;
            cameraKitActivity.mMode.startRecording();
            mMediaRecorder.start();
            getActivity().runOnUiThread(() -> {
                mButtonRecordPause.setVisibility(View.VISIBLE);
                mButtonVideo.setText(R.string.stoprecord);
                mButtonVideo.setEnabled(true);
            });
            mRecordState = CameraKitHelper.RecordState.RECORDING;
            Log.d(TAG, "Recording starts!");
        } catch (InterruptedException e) {
            Log.e(TAG, "acquiremStartStopRecordLock failed");
        } catch (IllegalStateException e) {
            Log.e(TAG, "mMediaRecorder prepare not well!");
            clearInvalidFile();
            mRecordState = CameraKitHelper.RecordState.IDLE;
        } finally {
            releasemStartStopRecordLock();
        }
    }

    private void stopRecord() {
        try {
            acquiremStartStopRecordLock();
            cameraKitActivity.mMode.stopRecording();
            mMediaRecorder.stop();
            Log.i(TAG, "captureImage begin");
            if (cameraKitActivity.mMode != null) {
                cameraKitActivity.mMode.setImageRotation(90);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "acquiremStartStopRecordLock failed");
        } catch (IllegalStateException e) {
            Log.e(TAG, "mMediaRecorder stop state error");
        } catch (RuntimeException stopException) {
            Log.e(TAG, "going to clean up the invalid output file");
            clearInvalidFile();
        } finally {
            mRecordState = CameraKitHelper.RecordState.IDLE;
            getActivity().runOnUiThread(() -> {
                mButtonRecordPause.setVisibility(View.INVISIBLE);
                mButtonVideo.setText(R.string.record);
            });
            releasemStartStopRecordLock();
        }
    }

    private void pauseRecord() {
        if (mRecordState == CameraKitHelper.RecordState.RECORDING) {
            Toast.makeText(getActivity(), "pauseRecord", Toast.LENGTH_LONG).show();
            try {
                acquiremStartStopRecordLock();
                cameraKitActivity.mMode.pauseRecording();
                mMediaRecorder.pause();
                mRecordState = CameraKitHelper.RecordState.PAUSED;
                getActivity().runOnUiThread(() -> {
                    mButtonRecordPause.setText(R.string.resume);
                    mButtonVideo.setEnabled(false);
                });
            } catch (InterruptedException e) {
                Log.e(TAG, "interrupted while trying to acquire start stop lock when pauseRecord" + e.getCause());
            } catch (IllegalStateException e) {
                Log.e(TAG, "mMediaRecorder pause state error");
            } finally {
                releasemStartStopRecordLock();
            }
        }
    }

    private void resumeRecord() {
        Toast.makeText(getActivity(), "resumeRecord", Toast.LENGTH_LONG).show();
        if (mRecordState == CameraKitHelper.RecordState.PAUSED) {
            Log.d(TAG, "[schedule] resume recording");
            try {
                acquiremStartStopRecordLock();
                cameraKitActivity.mMode.resumeRecording();
                mMediaRecorder.resume();
                mRecordState = CameraKitHelper.RecordState.RECORDING;
                getActivity().runOnUiThread(() -> {
                    mButtonRecordPause.setText(R.string.pause);
                    mButtonVideo.setEnabled(true);
                });
            } catch (InterruptedException e) {
                Log.e(TAG, "interrupted while trying to acquire start stop lock when resumeRecord " + e.getCause());
            } catch (IllegalStateException e) {
                Log.e(TAG, "mMediaRecorder resume state error");
            } finally {
                releasemStartStopRecordLock();
            }
        }
    }

    private void acquiremStartStopRecordLock() throws InterruptedException {
        if (mStartStopRecordLock != null) {
            mStartStopRecordLock.acquire();
        } else {
            Log.d(TAG, "acquiremStartStopRecordLock, mStartStopRecordLock refer null");
        }
    }

    private void releasemStartStopRecordLock() {
        if (mStartStopRecordLock != null) {
            if (mStartStopRecordLock.availablePermits() < 1) {
                mStartStopRecordLock.release();
            }
        } else {
            Log.d(TAG, "release lock, but it is null");
        }
    }

    private void clearInvalidFile() {
        if (!mVideoFile.isEmpty()) {
            File vidFile = new File(mVideoFile);
            if (vidFile.exists()) {
                vidFile.delete();
                mVideoFile = "";
                Log.d(TAG, "invalid video file deleted!");
            }
        }
    }

    private List<String> byteToList(Byte[] values, int id) {
        List<String> lists = new ArrayList<>(0);
        if ((values == null) || (values.length == 0)) {
            Log.d(TAG, "getIntList, values is null");
            return lists;
        }
        for (byte mode : values) {
            lists.add(getString(id) + PIVOT + mode);
        }
        return lists;
    }

    private void startCamerakit() {
        String[] cameraLists = cameraKitActivity.mCameraKit.getCameraIdList();
        String currCamera = cameraLists[0];
        if ((cameraLists != null) && (cameraLists.length > 0)) {
            Log.d(TAG, "openCamera: cameraId=" + cameraLists[0]);
            try {
                //String currCamera = cameraKitSuperNightCaptureActivity.mMode.getCameraId();
                if (!cameraKitActivity.mCameraOpenCloseLock.tryAcquire(OPEN_CAEMERA_TIME_OUT, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }
                if (cameraKitActivity.mMode != null) {
                    cameraKitActivity.mMode.release();
                    currCamera = cameraKitActivity.mMode.getCameraId();
                }
                cameraKitActivity.mCameraKit.createMode(currCamera, cameraKitActivity.mCurrentModeType, this.mModeStateCallback, cameraKitActivity.mCameraKitHandler);

                //cameraKitSuperNightCaptureActivity.mCameraKit.changeMode(cameraKitSuperNightCaptureActivity.mMode, Mode.Type.VIDEO_MODE, this.mModeStateCallback);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            }
        }
        mMediaRecorder = new MediaRecorder();
        cameraKitActivity.mModeCharacteristics = cameraKitActivity.mCameraKit.getModeCharacteristics(cameraLists[0], cameraKitActivity.mCurrentModeType);
        mSensorOrientation = cameraKitActivity.mModeCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }
}