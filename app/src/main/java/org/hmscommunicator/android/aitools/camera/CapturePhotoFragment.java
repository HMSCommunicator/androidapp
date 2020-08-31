package org.hmscommunicator.android.aitools.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;

import com.huawei.hms.ml.common.base.SmartLog;
import org.hmscommunicator.android.R;
import org.hmscommunicator.android.aitools.camera.CameraConfiguration;
import org.hmscommunicator.android.aitools.camera.LensEngine;
import org.hmscommunicator.android.aitools.camera.LensEnginePreview;
import org.hmscommunicator.android.aitools.util.Constant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CapturePhotoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CapturePhotoFragment extends Fragment {
    private static final String TAG = "CapturePhotoActivity";
    private LensEngine lensEngine = null;
    private LensEnginePreview preview;
    private CameraConfiguration cameraConfiguration = null;
    private int facing = CameraConfiguration.CAMERA_FACING_BACK;
    private String srcLanguage = "Auto";
    private String dstLanguage = "EN";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CapturePhotoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CameraFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CapturePhotoFragment newInstance(String param1, String param2) {
        CapturePhotoFragment fragment = new CapturePhotoFragment();
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
        View v = inflater.inflate(R.layout.fragment_camera, container, false);
        ImageButton takePhotoButton = v.findViewById(R.id.img_takePhoto);
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CapturePhotoFragment.this.toTakePhoto(v);
            }
        });
//        ImageButton backButton = v.findViewById(R.id.capture_back);
//        backButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                Fragment fRemoteTranslate =  new RemoteTranslateFragment();
////                getActivity().getSupportFragmentManager()
////                        .beginTransaction()
////                        .replace(R.id.view1, fRemoteTranslate)
////                        .commit();
//                Navigation.findNavController(v).navigate(R.id.action_capture_photo_dest_to_remoate_trans_dest);
//            }
//        });
        this.preview = v.findViewById(R.id.capture_preview);
        this.cameraConfiguration = new CameraConfiguration();
        this.cameraConfiguration.setCameraFacingBack(this.facing);
        this.srcLanguage = getArguments().getString(Constant.SOURCE_VALUE);
        this.dstLanguage = getArguments().getString(Constant.DEST_VALUE);
        this.createLensEngine();
        this.startLensEngine();
        return v;
    }

    private void createLensEngine() {
        if (this.lensEngine == null) {
            this.lensEngine = new LensEngine(getActivity(), this.cameraConfiguration);
        }
    }

    private void startLensEngine() {
        if (this.lensEngine != null) {
            try {
                this.preview.start(this.lensEngine, false);
            } catch (IOException e) {
                SmartLog.e(CapturePhotoFragment.TAG, "Unable to start lensEngine.", e);
                this.lensEngine.release();
                this.lensEngine = null;
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        this.startLensEngine();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.lensEngine != null) {
            this.lensEngine.release();
        }
        this.facing = CameraConfiguration.CAMERA_FACING_BACK;
        this.cameraConfiguration.setCameraFacingBack(this.facing);
    }

    private void toTakePhoto(View v) {
        lensEngine.takePicture(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                String filePath = null;
                try {
                    filePath = saveBitmapToDisk(bitmap);
                } catch (IOException e) {
                    SmartLog.e(TAG, "Save bitmap failed: " + e.getMessage());
                }
//                Intent intent = new Intent();
//                intent.putExtra(Constant.IMAGE_PATH_VALUE, filePath);
//                onActivityResult(getArguments().getInt("requestcode"), Activity.RESULT_OK, intent);
//                Navigation.findNavController(v).navigate(R.id.action_capture_photo_dest_to_remoate_trans_dest);
                Bundle bundle = new Bundle();
                bundle.putString(Constant.IMAGE_PATH_VALUE, filePath);
                bundle.putString(Constant.SOURCE_VALUE, srcLanguage);
                bundle.putString(Constant.DEST_VALUE, dstLanguage);
                Navigation.findNavController(v).navigate(R.id.action_capture_photo_dest_to_remoate_trans_dest, bundle);


//                Intent intent = new Intent();
//                intent.putExtra(Constant.IMAGE_PATH_VALUE, filePath);
//                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
//
//                FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
//                Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.my_nav_host_fragment);
//                fragmentTransaction.remove(currentFragment);
//                fragmentTransaction.show(getTargetFragment());
//                fragmentTransaction.commit();
            }
        });
    }


    private String saveBitmapToDisk(Bitmap bitmap) throws IOException {
        String storePath = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + "PhotoTranslate";
        File appDir = new File(storePath);
        if (!appDir.exists()) {
            boolean res = appDir.mkdir();
            if (!res) {
                SmartLog.e(TAG, "saveBitmapToDisk failed");
                return "";
            }
        }

        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            fos = null;

            Uri uri = Uri.fromFile(file);
            getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        } catch (FileNotFoundException e) {
            SmartLog.e(TAG, "Save bitmap failed: " + e.getMessage());
        }  catch (IOException e) {
            SmartLog.e(TAG, "Save bitmap failed: " + e.getMessage());
        } finally {
            try {
                if(fos != null) {
                    fos.close();
                }
            }catch (IOException e){
                SmartLog.e(TAG, "Close stream failed: " + e.getMessage());
            }
            fos = null;
        }

        return file.getCanonicalPath();
    }
}