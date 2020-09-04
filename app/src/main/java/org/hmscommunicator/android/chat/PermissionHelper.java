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


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

class PermissionHelper {
    public static final int REQUEST_CODE_ASK_PERMISSIONS = 1;

    private static final String[] PERMISSIONS_ARRAY = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION};

    private static List<String> permissionsList = new ArrayList<>(PERMISSIONS_ARRAY.length);

    private PermissionHelper() {
    }

    public static boolean hasPermission(final Activity activity) {
        for (String permission : PERMISSIONS_ARRAY) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void requestPermission(final Activity activity) {
        for (String permission : PERMISSIONS_ARRAY) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
            }
        }
        ActivityCompat.requestPermissions(activity, permissionsList.toArray(new String[permissionsList.size()]),
            REQUEST_CODE_ASK_PERMISSIONS);
    }
}
