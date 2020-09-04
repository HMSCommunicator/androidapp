/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hmscommunicator.android.chat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.huawei.camera.camerakit.ActionDataCallback;
import com.huawei.camera.camerakit.ActionStateCallback;
import com.huawei.camera.camerakit.CameraKit;
import com.huawei.camera.camerakit.Mode;
import com.huawei.camera.camerakit.ModeCharacteristics;
import com.huawei.camera.camerakit.ModeConfig;
import com.huawei.camera.camerakit.ModeStateCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.hmscommunicator.android.R;

/**
 * Demo Activity
 */
public class CameraKitActivity extends AppCompatActivity {

    private FragmentManager fManager;
    public static final String TAG = CameraKit.class.getSimpleName();
	
	private static final long PREVIEW_SURFACE_READY_TIMEOUT = 5000L;

    protected final ConditionVariable mPreviewSurfaceChangedDone = new ConditionVariable();

    private static final int DEFAULT_RETURN_VALUE = -1;

    public static final String PIVOT = " ";

    /**
     * View for preview
     */
    protected AutoFitTextureView mTextureView;

    /**
     * Button for capture
     */
    private Button mButtonCaptureImage;

    private Button mButtonStopPicture;

    private Button mButtonSuperNightMode;

    private Button mButtonBokehMode;

    private Button mButtonPortraitMode;

    private Button mButtonVideoMode;

    private Button mButtonSwitchCamera;

    private Button mButtonGallary;

    private Button mButtonNormalMode;

    private Button mButtonHDRMode;

    private ImageButton mButtonBackToChat;


    private OrientationEventListener mOrientationListener;

    /**
     * Preview size
     */
    protected Size mPreviewSize;

    /**
     * Capture size
     */
    private Size mCaptureSize;

    /**
     * Capture jpeg file
     */
    private File mFile;

    /**
     * CameraKit instance
     */
    protected CameraKit mCameraKit;

    /**
     * Current mode type
     */
    protected  @Mode.Type int mCurrentModeType = Mode.Type.NORMAL_MODE;

    /**
     * Current mode object
     */
    public Mode mMode;

    /**
     * Mode characteristics
     */
    public ModeCharacteristics mModeCharacteristics;

    /**
     * Mode config builder
     */
    public ModeConfig.Builder modeConfigBuilder;

    /**
     * Work thread for time consumed task
     */
    public HandlerThread mCameraKitThread;

    /**
     * Handler correspond to mCameraKitThread
     */
    protected Handler mCameraKitHandler;

    /**
     * Lock for camera device
     */
    protected Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                    mCameraKitHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            createMode();
                        }
                    });
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                    mPreviewSurfaceChangedDone.open();
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture texture) {
                }
            };

    private final ActionDataCallback actionDataCallback = new ActionDataCallback() {
        @Override
        public void onImageAvailable(Mode mode, @Type int type, Image image) {
            Log.d(TAG, "onImageAvailable: save img");
            switch (type) {
                case Type.TAKE_PICTURE: {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    FileOutputStream output = null;
                    try {
                        output = new FileOutputStream(mFile);
                        output.write(bytes);
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFile)));
                    } catch (IOException e) {
                        Log.e(TAG, "IOException when write in run");
                    } finally {
                        image.close();
                        if (output != null) {
                            try {
                                output.close();
                            } catch (IOException e) {
                                Log.e(TAG, "IOException when close in run");
                            }
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    private final ActionStateCallback actionStateCallback = new ActionStateCallback() {
        @Override
        public void onPreview(Mode mode, int state, PreviewResult result) {
            if (state == PreviewResult.State.PREVIEW_STARTED) {
                Log.i(TAG, "onPreview Started");
                runOnUiThread(() -> {
                    configZoomSeekBar();
                });
                runOnUiThread(() -> {
                    mButtonCaptureImage.setEnabled(true);
                });
            }
        }

        @Override
        public void onTakePicture(Mode mode, int state, TakePictureResult result) {
            switch (state) {
                case TakePictureResult.State.CAPTURE_STARTED:
                    Log.d(TAG, "onState: STATE_CAPTURE_STARTED");
                    break;
                case TakePictureResult.State.CAPTURE_EXPOSURE_BEGIN:
                    // exposure begin
                    processExposureBegein(result);
                    break;
                case TakePictureResult.State.CAPTURE_EXPOSURE_END:
                    // exposure end
                    processExposureEnd();
                    break;
                case TakePictureResult.State.CAPTURE_COMPLETED:
                    Log.d(TAG, "onState: STATE_CAPTURE_COMPLETED");
                    showToast("take picture success! file=" + mFile);
                    break;
                default:
                    break;
            }
        }
    };

    private void processExposureEnd() {
        runOnUiThread(() -> {
            Toast.makeText(this, "exposure end, capturing", Toast.LENGTH_SHORT).show();
            mButtonStopPicture.setVisibility(View.INVISIBLE);
        });
    }

    private void processExposureBegein(ActionStateCallback.TakePictureResult result) {
        if (result == null) {
            return;
        }

        if (mCurrentModeType == Mode.Type.SUPER_NIGHT_MODE) {
            // Get the exposure time of this shooting. After that time, the exposure will be completed. Then you can get the picture through actiondatacallback.onimageavailable.
            int exposureTime = result.getExposureTime();
            if (exposureTime != DEFAULT_RETURN_VALUE) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "exposureTime " + exposureTime, Toast.LENGTH_SHORT).show();
                    // after the exposure starts, it is allowed to finish the exposure by stoppicture before the end of the exposure countdown.
                    mButtonStopPicture.setVisibility(View.VISIBLE);
                });
            }
        }
    }

    private final ModeStateCallback mModeStateCallback = new ModeStateCallback() {
        @Override
        public void onCreated(Mode mode) {
            Log.d(TAG, "mModeStateCallback onModeOpened: ");
            mCameraOpenCloseLock.release();
            mMode = mode;
            mModeCharacteristics = mode.getModeCharacteristics();
            modeConfigBuilder = mMode.getModeConfigBuilder();
            configMode();
        }

        @Override
        public void onCreateFailed(String cameraId, int modeType, int errorCode) {
            Log.d(TAG,
                    "mModeStateCallback onCreateFailed with errorCode: " + errorCode + " and with cameraId: " + cameraId);
            mCameraOpenCloseLock.release();
        }

        @Override
        public void onConfigured(Mode mode) {
            Log.d(TAG, "mModeStateCallback onModeActivated : ");
            mMode.startPreview();

        }

        @Override
        public void onConfigureFailed(Mode mode, int errorCode) {
            Log.d(TAG, "mModeStateCallback onConfigureFailed with cameraId: " + mode.getCameraId());
            mCameraOpenCloseLock.release();
        }

        @Override
        public void onFatalError(Mode mode, int errorCode) {
            Log.d(TAG, "mModeStateCallback onFatalError with errorCode: " + errorCode + " and with cameraId: "
                    + mode.getCameraId());
            mCameraOpenCloseLock.release();
            finish();
        }

        @Override
        public void onReleased(Mode mode) {
            Log.d(TAG, "mModeStateCallback onModeReleased: ");
            mCameraOpenCloseLock.release();
        }
    };


    private void createMode() {
        Log.i(TAG, "createMode begin");
        mCameraKit = CameraKit.getInstance(getApplicationContext());
        if (mCameraKit == null) {
            Log.e(TAG, "This device does not support CameraKit！");
            showToast("CameraKit not exist or version not compatible");
            return;
        }
        // Query camera id list
        String[] cameraLists = mCameraKit.getCameraIdList();
        if ((cameraLists != null) && (cameraLists.length > 0)) {
            Log.i(TAG, "Try to use camera with id " + cameraLists[0]);
            // Query supported modes of this device
            //Log.e("camera id", "number of camera is  =====================" + cameraLists.length);
            int[] modes = mCameraKit.getSupportedModes(cameraLists[0]);
            if (!Arrays.stream(modes).anyMatch((i) -> i == mCurrentModeType)) {
                Log.w(TAG, "Current mode is not supported in this device!");
                return;
            }
            try {
                if (!mCameraOpenCloseLock.tryAcquire(2000, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }
                mCameraKit.createMode(cameraLists[0], mCurrentModeType, mModeStateCallback, mCameraKitHandler);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            }
        }
        Log.i(TAG, "createMode end");
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void configMode() {
        Log.i(TAG, "configMode begin");
        // Query supported preview size
        List<Size> previewSizes = mModeCharacteristics.getSupportedPreviewSizes(SurfaceTexture.class);
        // Query supported capture size
        List<Size> captureSizes = mModeCharacteristics.getSupportedCaptureSizes(ImageFormat.JPEG);
        Log.d(TAG, "configMode: captureSizes = " + captureSizes.size() + ";previewSizes=" + previewSizes.size());
        // Use the first one or default 4000x3000
        mCaptureSize = captureSizes.stream().findFirst().orElse(new Size(4000, 3000));
        // Use the same ratio with preview
        Size tmpPreviewSize = previewSizes.stream().filter((size) -> Math.abs((1.0f * size.getHeight() / size.getWidth()) - (1.0f * mCaptureSize.getHeight() / mCaptureSize.getWidth())) < 0.01).findFirst().get();
        Log.i(TAG, "configMode: mCaptureSize = " + mCaptureSize + ";mPreviewSize=" + mPreviewSize);
		// Update view
        runOnUiThread(() -> {
            mTextureView.setAspectRatio(tmpPreviewSize.getHeight(), tmpPreviewSize.getWidth());
        });
        waitTextureViewSizeUpdate(tmpPreviewSize);
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        if (texture == null) {
            Log.e(TAG, "configMode: texture=null!");
            return;
        }
        // Set buffer size of view
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        // Get surface of texture
        Surface surface = new Surface(texture);
        // Add preview and capture parameters to config builder
        modeConfigBuilder.addPreviewSurface(surface).addCaptureImage(mCaptureSize, ImageFormat.JPEG);
        // Set callback for config builder
        modeConfigBuilder.setDataCallback(actionDataCallback, mCameraKitHandler);
        modeConfigBuilder.setStateCallback(actionStateCallback, mCameraKitHandler);
        // Configure mode
        mMode.configure();
        Log.i(TAG, "configMode end");
    }

    private void waitTextureViewSizeUpdate(Size targetPreviewSize) {
        if (mPreviewSize == null) {
            mPreviewSize = targetPreviewSize;
            mPreviewSurfaceChangedDone.close();
            mPreviewSurfaceChangedDone.block(PREVIEW_SURFACE_READY_TIMEOUT);
        } else {
            if (targetPreviewSize.getHeight() * mPreviewSize.getWidth()
                    - targetPreviewSize.getWidth() * mPreviewSize.getHeight() == 0) {
                mPreviewSize = targetPreviewSize;
            } else {
                mPreviewSize = targetPreviewSize;
                mPreviewSurfaceChangedDone.close();
                mPreviewSurfaceChangedDone.block(PREVIEW_SURFACE_READY_TIMEOUT);
            }
        }
    }

    private void captureImage() {
        Log.i(TAG, "captureImage begin");
        if (mMode != null) {
            //mMode.setImageRotation(180);
            String storePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()+ "/Camera";
            File appDir = new File(storePath);
            if (!appDir.exists()) {
                boolean res = appDir.mkdir();
                if (!res) {
                    Log.e(TAG, "save photo to disk failed");
                    return;
                }
            }
            // Default jpeg file path
            mFile = new File(appDir, System.currentTimeMillis() + "pic.jpg");
            // Take picture
            mMode.takePicture();
        }
        Log.i(TAG, "captureImage end");
    }

    private void stopPicture() {
        /** In the super night view mode, call takepicture to enter the long exposure stage.
		You can call stoppicture to end the exposure in advance after receive TakePictureResult.State.CAPTURE_EXPOSURE_BEGIN, and get the photo  */
        if (mMode != null) {
            mButtonStopPicture.setVisibility(View.INVISIBLE);
            mMode.stopPicture();
        }
    }

    private void showToast(final String text) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mButtonCaptureImage = findViewById(R.id.capture_image);
        mButtonCaptureImage.setOnClickListener((v) -> captureImage());
        mButtonStopPicture = findViewById(R.id.stopPicture);
        mButtonStopPicture.setOnClickListener((v) -> stopPicture());
        mButtonStopPicture.setVisibility(View.INVISIBLE);
        mTextureView = findViewById(R.id.texture);
        mButtonSuperNightMode = findViewById(R.id.supernight);
        mButtonSuperNightMode.setOnClickListener((v) -> openSuperNight());
        mButtonBokehMode = findViewById(R.id.bokeh);
        mButtonBokehMode.setOnClickListener((v) -> openBokehMode());
        mButtonPortraitMode = findViewById(R.id.portrait);
        mButtonPortraitMode.setOnClickListener((v) -> openPortraitMode());
        mButtonSwitchCamera = findViewById(R.id.switchCamera);
        mButtonSwitchCamera.setOnClickListener((v) -> switchCamera());
        mButtonGallary = findViewById(R.id.gallary);
        mButtonNormalMode = findViewById(R.id.normal);
        mButtonNormalMode.setOnClickListener((v) -> openNormal());
        mButtonHDRMode = findViewById(R.id.hdr);
        mButtonHDRMode.setOnClickListener((v) -> openHDR());
        mButtonVideoMode = findViewById(R.id.video);
        mButtonVideoMode.setOnClickListener((v) -> openVideo());
//        mButtonBackToChat = findViewById(R.id.back_to_chat);
//        mButtonBackToChat.setOnClickListener((v) -> backToChat());


        mOrientationListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                String[] cameraLists = mCameraKit.getCameraIdList();
                //Log.d(TAG, "onOrientationChanged: " + orientation);
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return; // 手机平放时，检测不到有效的角度
                }
                // 只检测是否有四个角度的改变
                if (mMode != null) {
                    if (orientation > 350 || orientation < 10) {
                        // 0度：手机默认竖屏状态（home键在正下方)
                        if (mMode.getCameraId() == cameraLists[0]) {
                            mMode.setImageRotation(90);
                        }else {
                            mMode.setImageRotation(270);
                        }
                        mButtonCaptureImage.setRotation(0);
                        mButtonGallary.setRotation(0);
                        mButtonSwitchCamera.setRotation(0);
                    } else if (orientation > 80 && orientation < 100) {
                        // 90度：手机顺时针旋转90度横屏（home建在左侧）
                        mMode.setImageRotation(180);
                        mButtonCaptureImage.setRotation(270);
                        mButtonGallary.setRotation(270);
                        mButtonSwitchCamera.setRotation(270);
                    } else if (orientation > 170 && orientation < 190) {
                        // 180度：手机顺时针旋转180度竖屏（home键在上方）
                        if (mMode.getCameraId() == cameraLists[0]) {
                            mMode.setImageRotation(270);
                        }else {
                            mMode.setImageRotation(90);
                        }
                        mButtonCaptureImage.setRotation(180);
                        mButtonGallary.setRotation(180);
                        mButtonSwitchCamera.setRotation(180);
                    } else if (orientation > 260 && orientation < 280) {
                        // 270度：手机顺时针旋转270度横屏，（home键在右侧）
                        mMode.setImageRotation(360);
                        mButtonCaptureImage.setRotation(90);
                        mButtonGallary.setRotation(90);
                        mButtonSwitchCamera.setRotation(90);
                    }
                }
            }
        };
        mOrientationListener.enable();
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        if (!PermissionHelper.hasPermission(this)) {
            PermissionHelper.requestPermission(this);
            return;
        } else {
            if (!initCameraKit()) {
                showAlertWarning(getString(R.string.warning_str));
                return;
            }
        }
        startBackgroundThread();
        if (mTextureView != null) {
            if (mTextureView.isAvailable()) {
                mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
                mCameraKitHandler.post(() -> createMode());
            } else {
                mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            }
        }
    }

    protected void showAlertWarning(String msg) {
        new AlertDialog.Builder(this).setMessage(msg)
                .setTitle("warning:")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause: ");
        if (mMode != null) {
            mCameraKitHandler.post(() -> {
                try {
                    mCameraOpenCloseLock.acquire();
                    mMode.release();
                    mMode = null;
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
                } finally {
                    Log.d(TAG, "closeMode:");
                    mCameraOpenCloseLock.release();
                }
            });
        }
        super.onPause();
    }

    protected boolean initCameraKit() {
        mCameraKit = CameraKit.getInstance(getApplicationContext());
        if (mCameraKit == null) {
            Log.e(TAG, "initCamerakit: this devices not support camerakit or not installed!");
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        mOrientationListener.disable();
        super.onDestroy();
        stopBackgroundThread();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: ");
        if (!PermissionHelper.hasPermission(this)) {
            Toast.makeText(this, "This application needs camera permission.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    protected void startBackgroundThread() {
        Log.d(TAG, "startBackgroundThread");
        if (mCameraKitThread == null) {
            mCameraKitThread = new HandlerThread("CameraBackground");
            mCameraKitThread.start();
            mCameraKitHandler = new Handler(mCameraKitThread.getLooper());
//            if(mCameraKitHandler == null) {
//                Log.d("mcamerakithandler", "is null..................");
//            }
            Log.d(TAG, "startBackgroundTThread: mCameraKitThread.getThreadId()=" + mCameraKitThread.getThreadId());
        }
    }

    protected void stopBackgroundThread() {
        Log.d(TAG, "stopBackgroundThread");
        if (mCameraKitThread != null) {
            mCameraKitThread.quitSafely();
            try {
                mCameraKitThread.join();
                mCameraKitThread = null;
                mCameraKitHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException in stopBackgroundThread " + e.getMessage());
            }
        }
    }


    public void gallary(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/* video/*");
        this.startActivityForResult(intent, 1);
    }

    public void openSuperNight() {
        mCameraKit.changeMode(mMode, Mode.Type.SUPER_NIGHT_MODE, mModeStateCallback);
        mCurrentModeType = Mode.Type.SUPER_NIGHT_MODE;
        mButtonSwitchCamera.setVisibility(View.INVISIBLE);
        fManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fManager.beginTransaction();
        SuperNightFragment superNightFragment =  new SuperNightFragment();
        fragmentTransaction.replace(R.id.layoutview1, superNightFragment);
        fragmentTransaction.show(superNightFragment);
        fragmentTransaction.commit();
    }

    public void openHDR() {
        mCameraKit.changeMode(mMode, Mode.Type.HDR_MODE, mModeStateCallback);
        mCurrentModeType = Mode.Type.HDR_MODE;
        mButtonSwitchCamera.setVisibility(View.INVISIBLE);
        fManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fManager.beginTransaction();
        HDRFragment hdrFragment =  new HDRFragment();
        fragmentTransaction.replace(R.id.layoutview1, hdrFragment);
        fragmentTransaction.show(hdrFragment);
        fragmentTransaction.commit();
    }

    public void openBokehMode() {
        mCameraKit.changeMode(mMode, Mode.Type.BOKEH_MODE, mModeStateCallback);
        mCurrentModeType = Mode.Type.BOKEH_MODE;
        mButtonSwitchCamera.setVisibility(View.INVISIBLE);
        fManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fManager.beginTransaction();
        BokehFragment bokehFragment =  new BokehFragment();
        fragmentTransaction.replace(R.id.layoutview1, bokehFragment);
        fragmentTransaction.show(bokehFragment);
        fragmentTransaction.commit();
    }

    public void openPortraitMode() {
        mCameraKit.changeMode(mMode, Mode.Type.PORTRAIT_MODE, mModeStateCallback);
        mCurrentModeType = Mode.Type.PORTRAIT_MODE;
        mButtonSwitchCamera.setVisibility(View.VISIBLE);
        fManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fManager.beginTransaction();
        PortraitFragment portraitFragment =  new PortraitFragment();
        fragmentTransaction.replace(R.id.layoutview1, portraitFragment);
        fragmentTransaction.show(portraitFragment);
        fragmentTransaction.commit();
    }

    public void openNormal() {
        mCameraKit.changeMode(mMode, Mode.Type.NORMAL_MODE, mModeStateCallback);
        mCurrentModeType = Mode.Type.NORMAL_MODE;
        mButtonSwitchCamera.setVisibility(View.VISIBLE);
        fManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fManager.beginTransaction();
        NormalFragment normalFragment =  new NormalFragment();
        fragmentTransaction.replace(R.id.layoutview1, normalFragment);
        fragmentTransaction.show(normalFragment);
        fragmentTransaction.commit();
    }

    public void openVideo() {
//        mButtonSwitchCamera.setVisibility(View.VISIBLE);
        mButtonSwitchCamera.setVisibility(View.VISIBLE);
        mCurrentModeType = Mode.Type.VIDEO_MODE;
        fManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fManager.beginTransaction();
        VideoFragment videoFragment =  new VideoFragment();
        fragmentTransaction.replace(R.id.layoutview1, videoFragment);
        fragmentTransaction.show(videoFragment);
        fragmentTransaction.commit();
        Log.e("camerakit", "going to open video mode ...............");
    }

    public void switchCamera() {
        //mMode.release();
        if (mCameraKit == null) {
            initCameraKit();
        }
        String[] cameraLists = mCameraKit.getCameraIdList();
        if (cameraLists.length >= 2) {
            if (mMode.getCameraId() == cameraLists[0]) {
                try {
                    if (!mCameraOpenCloseLock.tryAcquire(2000, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException("Time out waiting to lock camera opening.");
                    }
                    mButtonBokehMode.setClickable(false);
                    mButtonSuperNightMode.setClickable(false);
                    mButtonHDRMode.setClickable(false);
                    mMode.release();
                    mCameraKit.createMode(cameraLists[1], mCurrentModeType, mModeStateCallback, mCameraKitHandler);
                    if (mCurrentModeType == Mode.Type.PORTRAIT_MODE) {

                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
                }
            }else {
                try {
                    if (!mCameraOpenCloseLock.tryAcquire(2000, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException("Time out waiting to lock camera opening.");
                    }
                    mButtonBokehMode.setClickable(true);
                    mButtonSuperNightMode.setClickable(true);
                    mButtonHDRMode.setClickable(true);
                    mMode.release();
                    mCameraKit.createMode(cameraLists[0], mCurrentModeType, mModeStateCallback, mCameraKitHandler);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
                }
            }
        }
    }

    private void configZoomSeekBar() {
        SeekBar mBokehSeekBar = findViewById(R.id.zoomSeekbar);
        TextView mTextView = findViewById(R.id.zoomTips);
        // if bokeh function supported
        //List<Float> values = mModeCharacteristics.getSupportedZoom();
        float[] ranges = mModeCharacteristics.getSupportedZoom();
        mBokehSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seek, int progress, boolean isFromUser) {
                double zoomLevel = round((1.0f * progress / 100) * (ranges[1] - ranges[0]), 1);
                mTextView.setText("Zoom Level: " + String.format(Locale.ENGLISH, "%.2f", zoomLevel));
                mMode.setZoom((float) zoomLevel);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seek) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seek) {
            }
        });
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

//    private void backToChat() {
//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
//    }

//    private void initZoomSpinner() {
//        initRotatedSpinner(R.id.zoomSpinner,
//                floatToList(mModeCharacteristics.getSupportedZoom(),
//                        R.string.zoom),
//                new SpinnerOperation() {
//                    @Override
//                    public void doOperation(String text) {
//                        try {
//                            mMode.setZoom(Float.valueOf(text.split(PIVOT)[1]));
//                        } catch (NumberFormatException e) {
//                            Log.e(TAG, "NumberFormatException text: " + text);
//                        }
//                    }
//                });
//    }
//
//    private List<String> floatToList(float[] values, int id) {
//        List<String> lists = new ArrayList<>(0);
//        if ((values == null) || (values.length == 0)) {
//            Log.d(TAG, "getLongList, values is null");
//            return lists;
//        }
//        for (float mode : values) {
//            lists.add(getString(id) + PIVOT + mode);
//        }
//        return lists;
//    }
//
//    private void initRotatedSpinner(int resId, List<String> list, final SpinnerOperation operation) {
//        final Spinner spinner = findViewById(resId);
//        spinner.setVisibility(View.VISIBLE);
//        //spinner.setRotation(90);
//        if (list.size() == 0) {
//            spinner.setVisibility(View.GONE);
//            return;
//        }
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item, R.id.itemText, list);
//        spinner.setAdapter(adapter);
//        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                String text = spinner.getItemAtPosition(position).toString();
//                mCameraKitHandler.post(() -> {
//                    if (mMode != null) {
//                        operation.doOperation(text);
//                    }
//                });
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//            }
//        });
//    }





}