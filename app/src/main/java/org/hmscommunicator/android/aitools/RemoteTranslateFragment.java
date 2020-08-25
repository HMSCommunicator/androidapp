package org.hmscommunicator.android.aitools;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.common.internal.client.SmartLog;
import com.huawei.hms.mlsdk.text.MLRemoteTextSetting;
import com.huawei.hms.mlsdk.text.MLText;
import com.huawei.hms.mlsdk.text.MLTextAnalyzer;
import com.huawei.hms.mlsdk.translate.MLTranslatorFactory;
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslateSetting;
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslator;

import org.hmscommunicator.android.MainActivity;
import org.hmscommunicator.android.aitools.camera.CapturePhotoFragment;
import org.hmscommunicator.android.aitools.util.BitmapUtils;
import org.hmscommunicator.android.aitools.util.Constant;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hmscommunicator.android.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RemoteTranslateFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RemoteTranslateFragment extends Fragment {
    private static String TAG = "RemoteTranslateActivity";
    private RelativeLayout relativeLayoutLoadPhoto;
    private RelativeLayout relativeLayoutTakePhoto;
    private RelativeLayout relativeLayoutTranslate;
    private ImageView preview;
    private TextView textView;
    private Uri imageUri;
    private String path;
    private Bitmap originBitmap;
    private Integer maxWidthOfImage;
    private Integer maxHeightOfImage;
    boolean isLandScape;
    private int REQUEST_CHOOSE_ORIGINPIC = 2001;
    private int REQUEST_TAKE_PHOTO = 2000;
    private static final String KEY_IMAGE_URI = "KEY_IMAGE_URI";
    private static final String KEY_IMAGE_MAX_WIDTH =
            "KEY_IMAGE_MAX_WIDTH";
    private static final String KEY_IMAGE_MAX_HEIGHT =
            "KEY_IMAGE_MAX_HEIGHT";
    private String sourceText = "";

    private String srcLanguage = "Auto";
    private String dstLanguage = "EN";

    private MLTextAnalyzer textAnalyzer;
    private MLRemoteTranslator translator;

    private String result;

    private Activity mActivity;

//    // TODO: Rename parameter arguments, choose names that match
//    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
//
//    // TODO: Rename and change types of parameters
//    private String mParam1;
//    private String mParam2;

    public RemoteTranslateFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RemoteTranslateFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RemoteTranslateFragment newInstance(String param1, String param2) {
        RemoteTranslateFragment fragment = new RemoteTranslateFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            this.srcLanguage = getArguments().getString(Constant.SOURCE_VALUE);
            this.dstLanguage = getArguments().getString(Constant.DEST_VALUE);
        } catch (RuntimeException e) {
            SmartLog.e(RemoteTranslateFragment.TAG, "Get args value failed:" + e.getMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_remote_translate, container, false);
//        v.findViewById(R.id.getback).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
//                Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.view1);
//                fragmentTransaction.remove(currentFragment);
//                fragmentTransaction.show(getTargetFragment()).commit();
//            }
//        });
        this.isLandScape =
                (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        this.initView(v);
        this.initAction();
        try {
            if (getArguments().getString(Constant.IMAGE_PATH_VALUE) != null) {
                Intent intent = new Intent();
                intent.putExtra(Constant.IMAGE_PATH_VALUE, getArguments().getString(Constant.IMAGE_PATH_VALUE));
                onActivityResult(RemoteTranslateFragment.this.REQUEST_TAKE_PHOTO, Activity.RESULT_OK, intent);
            }
        } catch (RuntimeException e) {
            SmartLog.e(RemoteTranslateFragment.TAG, "Get args value failed:" + e.getMessage());
        }
        return v;
    }


    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity){
            mActivity =(Activity) context;
        }
    }

    private void initView(View v) {
        this.relativeLayoutLoadPhoto = v.findViewById(R.id.relativate_chooseImg);
        this.relativeLayoutTakePhoto = v.findViewById(R.id.relativate_camera);
        this.relativeLayoutTranslate = v.findViewById(R.id.relativate_translate);
        this.preview = v.findViewById(R.id.previewPane);
        this.textView = v.findViewById(R.id.translate_result);
    }

    private void initAction() {
        this.relativeLayoutLoadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoteTranslateFragment.this.selectLocalImage(RemoteTranslateFragment.this.REQUEST_CHOOSE_ORIGINPIC);
            }
        });

        // Outline the edge.
        this.relativeLayoutTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (RemoteTranslateFragment.this.imageUri == null
                        && RemoteTranslateFragment.this.path == null) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.please_select_picture, Toast.LENGTH_SHORT).show();
                } else {
                    RemoteTranslateFragment.this.createRemoteTextAnalyzer();
                    Toast.makeText(getActivity().getApplicationContext(), R.string.translate_start, Toast.LENGTH_SHORT).show();
                }
            }
        });

        this.relativeLayoutTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoteTranslateFragment.this.takePhoto(RemoteTranslateFragment.this.REQUEST_TAKE_PHOTO, v);
            }
        });
    }

    private void takePhoto(int requestCode, View v) {
//        Bundle args = new Bundle();
//        args.putString(Constant.SOURCE_VALUE, this.srcLanguage);
//        args.putString(Constant.DEST_VALUE, this.dstLanguage);

//        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
//        Fragment existingFragment = new CapturePhotoFragment();
//        Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.my_nav_host_fragment);
//        fragmentTransaction.add(R.id.my_nav_host_fragment, currentFragment);
//        fragmentTransaction.add(R.id.my_nav_host_fragment, existingFragment);
//        existingFragment.setTargetFragment(currentFragment, requestCode);
//        fragmentTransaction.hide(currentFragment);
//        fragmentTransaction.show(existingFragment);
//        fragmentTransaction.commit();

//        Intent intent = new Intent(getActivity(), CapturePhotoActivity.class);
//        this.startActivityForResult(intent, requestCode);

        Bundle args = new Bundle();
        args.putInt("requestcode", requestCode);
        args.putString(Constant.SOURCE_VALUE, this.srcLanguage);
        args.putString(Constant.DEST_VALUE, this.dstLanguage);
        Navigation.findNavController(v).navigate(R.id.action_remoate_trans_dest_to_capture_photo_dest, args);
    }

    private void selectLocalImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        this.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == this.REQUEST_CHOOSE_ORIGINPIC)
                && (resultCode == Activity.RESULT_OK)) {
            // In this case, imageUri is returned by the chooser, save it.
            this.imageUri = data.getData();
            try {
                this.loadOriginImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ((requestCode == this.REQUEST_TAKE_PHOTO)
                && (resultCode == Activity.RESULT_OK)
                && data != null) {
            this.path = data.getStringExtra(Constant.IMAGE_PATH_VALUE);
            this.loadCameraImage();
        }
    }

    private void loadCameraImage() {
        FileInputStream fis = null;

        try {
            if (path == null) {
                return;
            }
            fis = new FileInputStream(path);
            this.originBitmap = BitmapFactory.decodeStream(fis);
            this.originBitmap = this.originBitmap.copy(Bitmap.Config.ARGB_4444, true);
            this.preview.setImageBitmap(this.originBitmap);
        } catch (IOException e) {
            Log.e(TAG, "file not found");
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException error) {
                    SmartLog.e(TAG, "Load camera image failed: " + error.getMessage());
                }
            }
        }
    }

    private void createRemoteTranslator() {
        MLRemoteTranslateSetting.Factory factory = new MLRemoteTranslateSetting
                .Factory()
                // Set the target language code. The ISO 639-1 standard is used.
                .setTargetLangCode(this.dstLanguage);
        if (!this.srcLanguage.equals("AUTO")) {
            // Set the source language code. The ISO 639-1 standard is used.
            factory.setSourceLangCode(this.srcLanguage);
        }
        this.translator = MLTranslatorFactory.getInstance().getRemoteTranslator(factory.create());
        final Task<String> task = translator.asyncTranslate(this.sourceText);
        task.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String text) {
                if (text != null) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogBuilder.setMessage("Do you want to keep original text?");
                    alertDialogBuilder.setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    RemoteTranslateFragment.this.remoteDisplaySuccess(text);
                                    Toast.makeText(getActivity(),"You clicked yes button",Toast.LENGTH_LONG).show();
                                }
                            });

                    alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            RemoteTranslateFragment.this.remoteDisplaySuccessWithoutSourceText(text);
                        }
                    });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                } else {
                    RemoteTranslateFragment.this.displayFailure();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                RemoteTranslateFragment.this.displayFailure();
            }
        });
    }

    private void createRemoteTextAnalyzer() {
        MLRemoteTextSetting setting = (new MLRemoteTextSetting.Factory())
                .setTextDensityScene(MLRemoteTextSetting.OCR_LOOSE_SCENE)
                .create();
        this.textAnalyzer = MLAnalyzerFactory.getInstance().getRemoteTextAnalyzer(setting);
        if (this.isChosen(this.originBitmap)) {
            MLFrame mlFrame = new MLFrame.Creator().setBitmap(this.originBitmap).create();
            Task<MLText> task = this.textAnalyzer.asyncAnalyseFrame(mlFrame);
            task.addOnSuccessListener(new OnSuccessListener<MLText>() {
                @Override
                public void onSuccess(MLText mlText) {
                    // Transacting logic for segment success.
                    if (mlText != null) {
                        try {
                            RemoteTranslateFragment.this.remoteDetectSuccess(mlText);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        RemoteTranslateFragment.this.displayFailure();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    // Transacting logic for segment failure.
                    RemoteTranslateFragment.this.displayFailure();
                    return;
                }
            });
        } else {
            Toast.makeText(getActivity().getApplicationContext(), R.string.please_select_picture, Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private void remoteDetectSuccess(MLText mlTexts) throws InterruptedException {
        this.sourceText = "";
        List<MLText.Block> blocks = mlTexts.getBlocks();
        List<MLText.TextLine> lines = new ArrayList<>();
        for (MLText.Block block : blocks) {
            for (MLText.TextLine line : block.getContents()) {
                if (line.getStringValue() != null) {
                    lines.add(line);
                }
            }
        }
        Collections.sort(lines, new SortComparator());
        for (int i = 0; i < lines.size(); i++) {
            this.sourceText = this.sourceText + lines.get(i).getStringValue().trim() + "\n";
        }

        this.createRemoteTranslator();
    }

    private static class SortComparator implements Comparator<MLText.TextLine> {
        @Override
        public int compare(MLText.TextLine o1, MLText.TextLine o2) {
            Point[] point1 = o1.getVertexes();
            Point[] point2 = o2.getVertexes();
            return point1[0].y - point2[0].y;
        }
    }

    private void remoteDisplaySuccess(String test) {
        String[] sourceLines = sourceText.split("\n");
        String[] drtLines = test.split("\n");
        if (textView.getContext().toString() != null) {
            textView.setText("");
        }
        for (int i = 0; i < sourceLines.length && i < drtLines.length; i++) {
            this.textView.append(sourceLines[i] + "-> " + drtLines[i] + "\n");
        }
        Toast.makeText(getActivity().getApplicationContext(), R.string.translate_success, Toast.LENGTH_SHORT).show();
    }

    private void remoteDisplaySuccessWithoutSourceText(String test) {
        String[] sourceLines = sourceText.split("\n");
        String[] drtLines = test.split("\n");
        if (textView.getContext().toString() != null) {
            textView.setText("");
        }
        for (int i = 0; i < sourceLines.length && i < drtLines.length; i++) {
            this.textView.append(drtLines[i] + "\n");
        }
        Toast.makeText(getActivity().getApplicationContext(), R.string.translate_success, Toast.LENGTH_SHORT).show();
    }

    private void displayFailure() {
        Toast.makeText(getActivity().getApplicationContext(), "Fail", Toast.LENGTH_SHORT).show();
    }

    private boolean isChosen(Bitmap bitmap) {
        if (bitmap == null) {
            return false;
        } else {
            return true;
        }
    }

    private void loadOriginImage() throws IOException {
        if (this.imageUri == null) {
            return;
        }
        Pair<Integer, Integer> targetedSize = this.getTargetSize();
        int targetWidth = targetedSize.first;
        int maxHeight = targetedSize.second;
        if (mActivity == null) {
            Log.e("nll", "it is  null activity");
        }
        //this.originBitmap = BitmapUtils.loadFromPath(mActivity, this.imageUri, targetWidth, maxHeight);

        // Determine how much to scale down the image.
        //SmartLog.i(RemoteTranslateFragment.TAG, "resized image size width:" + this.originBitmap.getWidth() + ",height: " + this.originBitmap.getHeight());
        //this.preview.setImageBitmap(this.originBitmap);

        //this.originBitmap = MediaStore.Images.Media.getBitmap(mActivity.getContentResolver(), imageUri);
        this.preview.setImageURI(imageUri);
        //Bitmap bp = ((BitmapDrawable) preview.getDrawable()).getBitmap();
        this.originBitmap = ((BitmapDrawable) preview.getDrawable()).getBitmap();
    }

    // Returns max width of image.
    private Integer getMaxWidthOfImage() {
        if (this.maxWidthOfImage == null) {
            if (this.isLandScape) {
                this.maxWidthOfImage = ((View) this.preview.getParent()).getHeight();
            } else {
                this.maxWidthOfImage = ((View) this.preview.getParent()).getWidth();
            }
        }
        return this.maxWidthOfImage;
    }

    // Returns max height of image.
    private Integer getMaxHeightOfImage() {
        if (this.maxHeightOfImage == null) {
            if (this.isLandScape) {
                this.maxHeightOfImage = ((View) this.preview.getParent()).getWidth();
            } else {
                this.maxHeightOfImage = ((View) this.preview.getParent()).getHeight();
            }
        }
        return this.maxHeightOfImage;
    }

    // Gets the targeted size(width / height).
    private Pair<Integer, Integer> getTargetSize() {
        Integer targetWidth;
        Integer targetHeight;
        Integer maxWidth = this.getMaxWidthOfImage();
        Integer maxHeight = this.getMaxHeightOfImage();
        targetWidth = this.isLandScape ? maxHeight : maxWidth;
        targetHeight = this.isLandScape ? maxWidth : maxHeight;
        SmartLog.i(RemoteTranslateFragment.TAG, "height:" + targetHeight + ",width:" + targetWidth);
        return new Pair<>(targetWidth, targetHeight);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.textAnalyzer != null) {
            try {
                this.textAnalyzer.close();
            } catch (IOException e) {
                SmartLog.e(RemoteTranslateFragment.TAG, "Stop analyzer failed: " + e.getMessage());
            }
        }
        if (this.translator != null) {
            this.translator.stop();
        }
        this.imageUri = null;
        this.path = null;
        this.srcLanguage = "Auto";
        this.dstLanguage = "EN";
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(RemoteTranslateFragment.KEY_IMAGE_URI, this.imageUri);
        if (this.maxWidthOfImage != null) {
            outState.putInt(RemoteTranslateFragment.KEY_IMAGE_MAX_WIDTH, this.maxWidthOfImage);
        }
        if (this.maxHeightOfImage != null) {
            outState.putInt(RemoteTranslateFragment.KEY_IMAGE_MAX_HEIGHT, this.maxHeightOfImage);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

}