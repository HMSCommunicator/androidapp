package org.hmscommunicator.android.chat;


import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

import com.huawei.camera.camerakit.RequestKey;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import org.hmscommunicator.android.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SuperNightFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SuperNightFragment extends Fragment {
    public CameraKitActivity cameraKitActivity;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SuperNightFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SuperNightFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SuperNightFragment newInstance(String param1, String param2) {
        SuperNightFragment fragment = new SuperNightFragment();
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
        View v = inflater.inflate(R.layout.fragment_super_night, container, false);
        cameraKitActivity = (CameraKitActivity) getActivity();
        //mMode = cameraKitSuperNightCaptureActivity.mMode
//        cameraKitSuperNightCaptureActivity.runOnUiThread(() -> {
//            initManualIsoSpinner(v);
//            initManualExposureSpinner(v);
//        });
        initManualIsoSpinner(v);
        initManualExposureSpinner(v);
            //configBokehSeekBar();
        return v;
    }

    private void initManualIsoSpinner(View v) {
        Long[] ranges = new Long[0];
        List<CaptureRequest.Key<?>> parameters = cameraKitActivity.mModeCharacteristics.getSupportedParameters();
        if ((parameters != null) && (parameters.contains(RequestKey.HW_SUPER_NIGHT_ISO))) {
            List<Long> lists = cameraKitActivity.mModeCharacteristics.getParameterRange(RequestKey.HW_SUPER_NIGHT_ISO);
            ranges = new Long[lists.size()];
            lists.toArray(ranges);
        }
        initSpinner(v, R.id.manualIso, longToList(ranges, R.string.manualIso), new SpinnerOperation() {
            @Override
            public void doOperation(String text) {
                try {
                    cameraKitActivity.mMode.setParameter(RequestKey.HW_SUPER_NIGHT_ISO, Long.parseLong(text.split(cameraKitActivity.PIVOT)[1]));
                } catch (PatternSyntaxException | NumberFormatException e) {
                    Log.e(cameraKitActivity.TAG, "patternSyntaxException NumberFormatException text: " + text);
                }
            }
        });
    }

    private void initManualExposureSpinner(View v) {
        Long[] ranges = new Long[0];
        List<CaptureRequest.Key<?>> parameters = cameraKitActivity.mModeCharacteristics.getSupportedParameters();
        if ((parameters != null) && (parameters.contains(RequestKey.HW_SUPER_NIGHT_EXPOSURE))) {
            List<Long> lists = cameraKitActivity.mModeCharacteristics.getParameterRange(RequestKey.HW_SUPER_NIGHT_EXPOSURE);
            ranges = new Long[lists.size()];
            lists.toArray(ranges);
        }
        initSpinner(v, R.id.manualExposure, longToList(ranges, R.string.manualExposure), new SpinnerOperation() {
            @Override
            public void doOperation(String text) {
                try {
                    cameraKitActivity.mMode.setParameter(RequestKey.HW_SUPER_NIGHT_EXPOSURE, Long.parseLong(text.split(cameraKitActivity.PIVOT)[1]));
                } catch (PatternSyntaxException | NumberFormatException e) {
                    Log.e(cameraKitActivity.TAG, "patternSyntaxException NumberFormatException text: " + text);
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

    private List<String> longToList(Long[] values, int id) {
        List<String> lists = new ArrayList<>(0);
        if ((values == null) || (values.length == 0)) {
            Log.d(cameraKitActivity.TAG, "getLongList, values is null");
            return lists;
        }
        for (long mode : values) {
            lists.add(getString(id) + cameraKitActivity.PIVOT + mode);
        }
        return lists;
    }

}