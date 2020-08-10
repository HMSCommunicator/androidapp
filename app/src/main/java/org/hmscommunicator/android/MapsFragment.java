package org.hmscommunicator.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.SupportMapFragment;
import com.huawei.hms.maps.model.LatLng;

public class MapsFragment extends Fragment {

    private static final String TAG = "MapsFragment";

    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(HuaweiMap huaweiMap) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.i(TAG, "sdk >= 23 M");
                Activity activity;
                activity = getActivity();
                if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    String[] strings =
                            {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                    ActivityCompat.requestPermissions(activity, strings, 1);
                }
            }
//            LatLng sydney = new LatLng(-34, 151);
//            huaweiMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//            huaweiMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
            Log.d(TAG, "onMapReady: ");
//            hMap = map;
            huaweiMap.setMyLocationEnabled(true);
            huaweiMap.getUiSettings().setMyLocationButtonEnabled(true);

            huaweiMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.804897,-122.477195), 10));
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }
}