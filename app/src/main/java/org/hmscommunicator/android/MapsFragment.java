package org.hmscommunicator.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.SupportMapFragment;
import com.huawei.hms.maps.model.LatLng;

import org.hmscommunicator.android.R;

public class MapsFragment extends Fragment {

    private static final String TAG = "MapsFragment";
    private HuaweiMap huaweiMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

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
        public void onMapReady(HuaweiMap map) {
            huaweiMap = map;
            final Activity activity;
            activity = getActivity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.i(TAG, "sdk >= 23 M");
                if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    String[] strings =
                            {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                    ActivityCompat.requestPermissions(activity, strings, 1);
                }
            }
            LatLng currLocation = new LatLng(37.717376,-121.864727);  //set default location to San Francisco before current location detection
            huaweiMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currLocation, 3));
            Log.d(TAG, "onMapReady: ");
            huaweiMap.setMyLocationEnabled(true);

            huaweiMap.setOnMapLoadedCallback(new HuaweiMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    setCurrentLocationOnMap();
                }
            });

            huaweiMap.setOnMapLongClickListener(new HuaweiMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    Toast.makeText(activity.getApplicationContext(), "onMapLongClick:" + latLng.toString(), Toast.LENGTH_SHORT).show();
                }
            });

            huaweiMap.setOnMyLocationButtonClickListener(new HuaweiMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    Toast.makeText(activity.getApplicationContext(), "MyLocation button clicked", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
    };

    /**
     * Obtain the last known location and set on the map
     */
    private void setCurrentLocationOnMap() {
        try {
            // create fusedLocationProviderClient
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
            mFusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location == null) {
                        Log.i(TAG, "getLastLocation onSuccess location is null");
                        return;
                    }
                    Log.i(TAG,
                            "getLastLocation onSuccess location[Longitude,Latitude]:" + location.getLongitude() + ","
                                    + location.getLatitude());
                    LatLng latLngCurrentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    huaweiMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngCurrentLocation, 15), 1000, null);
                    huaweiMap.addMarker(new MarkerOptions().position(latLngCurrentLocation).title("walking"));

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "getLastLocation onFailure:" + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "getLastLocation exception:" + e.getMessage());
        }

    }

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