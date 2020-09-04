package org.hmscommunicator.android.chat;

import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.huawei.camera.camerakit.RequestKey;

import java.util.List;
import java.util.Locale;

import org.hmscommunicator.android.R;
/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BokehFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BokehFragment extends Fragment {
    public CameraKitActivity cameraKitActivity;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public BokehFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BokehFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BokehFragment newInstance(String param1, String param2) {
        BokehFragment fragment = new BokehFragment();
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
        View v = inflater.inflate(R.layout.fragment_bokeh, container, false);
        cameraKitActivity = (CameraKitActivity) getActivity();
        configBokehSeekBar(v);
        return v;
    }

    private void configBokehSeekBar(View v) {
        SeekBar mBokehSeekBar = v.findViewById(R.id.bokehSeekbar);
        final TextView mTextView = v.findViewById(R.id.bokehTips);
        List<CaptureRequest.Key<?>> parameters = cameraKitActivity.mModeCharacteristics.getSupportedParameters();
        // if bokeh function supported
        if ((parameters != null) && (parameters.contains(RequestKey.HW_APERTURE))) {
            List<Float> values = cameraKitActivity.mModeCharacteristics.getParameterRange(RequestKey.HW_APERTURE);
            final Float[] ranges = values.toArray(new Float[values.size()]);
            mBokehSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seek, int progress, boolean isFromUser) {
                    int index = Math.round((1.0f * progress / 100) * (ranges.length - 1));
                    mTextView.setText("Bokeh Level: " + String.format(Locale.ENGLISH, "%.2f", ranges[index]));
                    cameraKitActivity.mMode.setParameter(RequestKey.HW_APERTURE, ranges[index]);
                }
                @Override
                public void onStartTrackingTouch(SeekBar seek) {
                }
                @Override
                public void onStopTrackingTouch(SeekBar seek) {
                }
            });
        } else {
            Log.d(cameraKitActivity.TAG, "configBokehSeekBar: this mode does not support bokeh!");
            mBokehSeekBar.setVisibility(View.GONE);
            mTextView.setVisibility(View.GONE);
        }
    }
}