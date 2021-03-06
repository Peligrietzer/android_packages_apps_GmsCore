/*
 * Copyright 2013-2015 µg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.google.android.auth.IAuthManagerService;
import com.google.android.gms.auth.AccountChangeEventsRequest;
import com.google.android.gms.auth.AccountChangeEventsResponse;

import org.microg.gms.common.PackageUtils;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static android.accounts.AccountManager.KEY_ANDROID_PACKAGE_NAME;
import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static org.microg.gms.auth.AskPermissionActivity.EXTRA_CONSENT_DATA;

public class AuthManagerServiceImpl extends IAuthManagerService.Stub {
    private static final String TAG = "GmsAuthManagerSvc";

    public static final String KEY_AUTHORITY = "authority";
    public static final String KEY_CALLBACK_INTENT = "callback_intent";
    public static final String KEY_CALLER_UID = "callerUid";
    public static final String KEY_CLIENT_PACKAGE_NAME = "clientPackageName";
    public static final String KEY_HANDLE_NOTIFICATION = "handle_notification";
    public static final String KEY_REQUEST_ACTIONS = "request_visible_actions";
    public static final String KEY_REQUEST_VISIBLE_ACTIVITIES = "request_visible_actions";
    public static final String KEY_SUPPRESS_PROGRESS_SCREEN = "suppressProgressScreen";
    public static final String KEY_SYNC_EXTRAS = "sync_extras";

    public static final String KEY_AUTH_TOKEN = "authtoken";
    public static final String KEY_ERROR = "Error";
    public static final String KEY_USER_RECOVERY_INTENT = "userRecoveryIntent";

    private final Context context;

    public AuthManagerServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public Bundle getToken(String accountName, String scope, Bundle extras) throws RemoteException {
        String packageName = extras.getString(KEY_ANDROID_PACKAGE_NAME, extras.getString(KEY_CLIENT_PACKAGE_NAME, null));
        int callerUid = extras.getInt(KEY_CALLER_UID, 0);
        PackageUtils.checkPackageUid(context, packageName, callerUid, getCallingUid());
        boolean notify = extras.getBoolean(KEY_HANDLE_NOTIFICATION, false);

        Log.d(TAG, "getToken: account:" + accountName + " scope:" + scope + " extras:" + extras + ", notify: " + notify);
        AuthManager authManager = new AuthManager(context, accountName, packageName, scope);
        try {
            AuthResponse res = authManager.requestAuth(false);
            if (res.auth != null) {
                Log.d(TAG, "getToken: " + res.auth);
                Bundle result = new Bundle();
                result.putString(KEY_AUTH_TOKEN, res.auth);
                result.putString(KEY_ERROR, "OK");
                return result;
            } else {
                Bundle result = new Bundle();
                result.putString(KEY_ERROR, "Unknown");
                Intent i = new Intent(context, AskPermissionActivity.class);
                i.putExtras(extras);
                i.putExtra(KEY_ANDROID_PACKAGE_NAME, packageName);
                i.putExtra(KEY_ACCOUNT_TYPE, authManager.getAccountType());
                i.putExtra(KEY_ACCOUNT_NAME, accountName);
                i.putExtra(KEY_AUTHTOKEN, scope);
                if (res.consentDataBase64 != null)
                    i.putExtra(EXTRA_CONSENT_DATA, Base64.decode(res.consentDataBase64, Base64.DEFAULT));
                if (notify) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                } else {
                    result.putParcelable(KEY_USER_RECOVERY_INTENT, i);
                }
                return result;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public AccountChangeEventsResponse getChangeEvents(AccountChangeEventsRequest request) {
        return new AccountChangeEventsResponse();
    }

    @Override
    public Bundle clearToken(String token, Bundle extras) throws RemoteException {
        return null;
    }
}
