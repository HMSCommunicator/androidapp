package org.hmscommunicator.android.chat;


import android.os.Bundle;
import android.os.ConditionVariable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

import com.huawei.camera.camerakit.CameraKit;
import com.huawei.camera.camerakit.Metadata;

import java.util.ArrayList;
import java.util.List;
import org.hmscommunicator.android.R;
/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PortraitFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PortraitFragment extends Fragment {

    private static final String TAG = CameraKit.class.getSimpleName();

    private static final long PREVIEW_SURFACE_READY_TIMEOUT = 5000L;

    private final ConditionVariable mPreviewSurfaceChangedDone = new ConditionVariable();

    private static final String PIVOT = " ";

    public CameraKitActivity cameraKitActivity;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public PortraitFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PortraitFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PortraitFragment newInstance(String param1, String param2) {
        PortraitFragment fragment = new PortraitFragment();
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
        View v =  inflater.inflate(R.layout.fragment_portrait, container, false);
        cameraKitActivity = (CameraKitActivity) getActivity();
        initBodySpinner(v);
        initSkinColorSpinner(v);
        initSlenderSpinner(v);
        initSmoothSpinner(v);
        return v;
    }

    private void initSlenderSpinner(View v) {
        initSpinner(v, R.id.slenderSpinner,
                intToList(cameraKitActivity.mModeCharacteristics.getSupportedBeauty(Metadata.BeautyType.HW_BEAUTY_FACE_SLENDER),
                        R.string.slender),
                new SpinnerOperation() {
                    @Override
                    public void doOperation(String text) {
                        try {
                            cameraKitActivity.mMode.setBeauty(Metadata.BeautyType.HW_BEAUTY_FACE_SLENDER,
                                    Integer.valueOf(text.split(PIVOT)[1]));
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "NumberFormatException initSlenderSpinner text: " + text);
                        }
                    }
                });
        Log.e("spinner", "initial finished here");
    }

    private void initSkinColorSpinner(View v) {
        initSpinner(v, R.id.skinColorSpinner,
                intToList(cameraKitActivity.mModeCharacteristics.getSupportedBeauty(Metadata.BeautyType.HW_BEAUTY_SKIN_COLOR),
                        R.string.skinColor),
                new SpinnerOperation() {
                    @Override
                    public void doOperation(String text) {
                        try {
                            cameraKitActivity.mMode.setBeauty(Metadata.BeautyType.HW_BEAUTY_SKIN_COLOR,
                                    Integer.valueOf(text.split(PIVOT)[1]));
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "initSkinColorSpinner text:" + text);
                        }
                    }
                });
    }

    private void initSmoothSpinner(View v) {
        initSpinner(v, R.id.smoothSpinner,
                intToList(cameraKitActivity.mModeCharacteristics.getSupportedBeauty(Metadata.BeautyType.HW_BEAUTY_SKIN_SMOOTH),
                        R.string.smooth),
                new SpinnerOperation() {
                    @Override
                    public void doOperation(String text) {
                        try {
                            cameraKitActivity.mMode.setBeauty(Metadata.BeautyType.HW_BEAUTY_SKIN_SMOOTH,
                                    Integer.valueOf(text.split(PIVOT)[1]));
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "NumberFormatException text: " + text);
                        }
                    }
                });
    }

    private void initBodySpinner(View v) {
        initSpinner(v, R.id.bodySpinner,
                intToList(cameraKitActivity.mModeCharacteristics.getSupportedBeauty(Metadata.BeautyType.HW_BEAUTY_BODY_SHAPING),
                        R.string.body),
                new SpinnerOperation() {
                    @Override
                    public void doOperation(String text) {
                        try {
                            cameraKitActivity.mMode.setBeauty(Metadata.BeautyType.HW_BEAUTY_BODY_SHAPING,
                                    Integer.valueOf(text.split(PIVOT)[1]));
                        } catch (NumberFormatException e) {
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
                    if (cameraKitActivity.mMode != null) {
                        operation.doOperation(text);
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private List<String> intToList(int[] values, int id) {
        List<String> lists = new ArrayList<>(0);
        if ((values == null) || (values.length == 0)) {
            Log.d(TAG, "getIntList1, values is null");
            return lists;
        }
        for (int mode : values) {
            lists.add(getString(id) + PIVOT + mode);
        }
        return lists;
    }
}