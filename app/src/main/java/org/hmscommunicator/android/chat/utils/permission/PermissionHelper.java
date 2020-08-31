/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hmscommunicator.android.chat.utils.permission;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

/**
 * Permission helper.
 *
 * @since 2020-01-13
 */
public class PermissionHelper {
    private Activity mActivity;
    private org.hmscommunicator.android.chat.utils.permission.PermissionInterface mPermissionInterface;

    public PermissionHelper(@NonNull Activity activity,
                            @NonNull org.hmscommunicator.android.chat.utils.permission.PermissionInterface permissionInterface) {
        mActivity = activity;
        mPermissionInterface = permissionInterface;
    }

    public void requestPermissions() {
        String[] deniedPermissions =
                org.hmscommunicator.android.chat.utils.permission.PermissionUtil.getDeniedPermissions(
                        mActivity, mPermissionInterface.getPermissions());
        if (deniedPermissions != null && deniedPermissions.length > 0) {
            org.hmscommunicator.android.chat.utils.permission.PermissionUtil.requestPermissions(
                    mActivity, deniedPermissions, mPermissionInterface.getPermissionsRequestCode());
        } else {
            mPermissionInterface.requestPermissionsSuccess();
        }
    }

    public boolean requestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == mPermissionInterface.getPermissionsRequestCode()) {
            boolean isAllGranted = true;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    isAllGranted = false;
                    break;
                }
            }
            if (isAllGranted) {
                mPermissionInterface.requestPermissionsSuccess();
            } else {
                mPermissionInterface.requestPermissionsFail();
            }
            return true;
        }
        return false;
    }
}
