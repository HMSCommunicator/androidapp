package org.hmscommunicator.android.aitools;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.ml.common.base.SmartLog;
import com.huawei.hms.mlsdk.common.MLApplication;
import com.huawei.hms.mlsdk.translate.MLTranslatorFactory;
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslateSetting;
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslator;

import org.hmscommunicator.android.aitools.PhotoTranslatorViewModel;
import org.hmscommunicator.android.aitools.util.Constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.hmscommunicator.android.R;

public class LangSelectFragment extends Fragment {
    private static final String TAG = "MainActivity";
    private static final String[] SOURCE_LANGUAGE_CODE = new String[]{"Auto", "ZH", "EN", "FR", "ES", "AR", "TH", "TR", "DE", "IT", "JA", "PT", "RU"};
    private static final String[] DEST_LANGUAGE_CODE = new String[]{"ZH", "EN", "FR", "ES", "AR", "TH", "TR", "DE", "IT", "JA", "PT", "RU"};
    private static final List<String> SP_SOURCE_LIST = new ArrayList<>(Arrays.asList("自动检测", "中文", "英文", "法语", "西班牙语", "阿拉伯语", "泰语", "土耳其语", "德语", "意大利语", "日语", "葡萄牙语", "俄语"));
    private static final List<String> SP_SOURCE_LIST_EN = new ArrayList<>(Arrays.asList("Auto", "Chinese", "English", "French", "Spanish", "Arabic", "Thai", "Turkish", "German", "Italian", "Japanese", "Portuguese", "Russian"));
    private static final List<String> SP_DEST_LIST = new ArrayList<>(Arrays.asList("中文", "英文", "法语", "西班牙语", "阿拉伯语", "泰语", "土耳其语", "德语", "意大利语", "日语", "葡萄牙语", "俄语"));
    private static final List<String> SP_DEST_LIST_EN = new ArrayList<>(Arrays.asList("Chinese", "English", "French", "Spanish", "Arabic", "Thai", "Turkish", "German", "Italian", "Japanese", "Portuguese", "Russian"));
    private static final List<String> CODE_LIST = new ArrayList<>(Arrays.asList("ar", "de", "en", "es", "fr", "it", "ja", "pt", "ru", "th", "tr", "zh", "ro"));
    private static final List<String> LANGUAGE_LIST= new ArrayList<>(Arrays.asList("Arabic", "German", "English", "Spanish", "French", "Italian",
            "Japanese", "Portuguese", "Russian", "Thai", "Turkish", "Chinese", "Romanian"));

    private static final int PERMISSION_REQUESTS = 1;

    private Spinner spSourceType;
    private Spinner spDestType;
    private ImageButton btrSwitchLang;
    private String srcLanguage = "Auto";
    private String dstLanguage = "EN";
    public static final String EN = "en";
    private ArrayAdapter<String> spSourceAdapter;
    private ArrayAdapter<String> spDestAdapter;
    private EditText input_;

    private MLRemoteTranslator translator;
    private String sourceText;
    private TextView output_;

    private PhotoTranslatorViewModel mViewModel;

    public static LangSelectFragment newInstance() {
        return new LangSelectFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_language_select, container, false);
        this.output_ = v.findViewById(R.id.outputView);
        this.input_ = v.findViewById(R.id.inputView);
        input_.setOnKeyListener(new View.OnKeyListener()
        {
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            sourceText = input_.getText().toString();
                            createRemoteTranslator();
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
        this.spSourceType = v.findViewById(R.id.spSourceType);
        this.spDestType = v.findViewById(R.id.spDestType);
        this.btrSwitchLang = v.findViewById(R.id.buttonSwitchLang);
        v.findViewById(R.id.select_photo).setOnClickListener(this::onClick);
        v.findViewById(R.id.asr).setOnClickListener(this::onClick);
        btrSwitchLang.setOnClickListener(this::onClick);
        this.createSpinner();
        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(PhotoTranslatorViewModel.class);
        // TODO: Use the ViewModel
    }

    private void createSpinner() {
        if (this.isEngLanguage()) {
            this.spSourceAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, LangSelectFragment.SP_SOURCE_LIST_EN);
            this.spDestAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, LangSelectFragment.SP_DEST_LIST_EN);
        } else {
            this.spSourceAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, LangSelectFragment.SP_SOURCE_LIST);
            this.spDestAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, LangSelectFragment.SP_DEST_LIST);
        }

        this.spSourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (this.spSourceType != null) {
            this.spSourceType.setAdapter(this.spSourceAdapter);
        }

        this.spDestAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (this.spDestType != null) {
            this.spDestType.setAdapter(this.spDestAdapter);
        }

        if (spSourceType != null) {
            this.spSourceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    LangSelectFragment.this.srcLanguage = LangSelectFragment.SOURCE_LANGUAGE_CODE[position];
                    SmartLog.i(LangSelectFragment.TAG, "srcLanguage: " + LangSelectFragment.this.srcLanguage);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        if (spDestType != null) {
            this.spDestType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    LangSelectFragment.this.dstLanguage = LangSelectFragment.DEST_LANGUAGE_CODE[position];
                    SmartLog.i(LangSelectFragment.TAG, "dstLanguage: " + LangSelectFragment.this.dstLanguage);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    public boolean isEngLanguage() {
        Locale locale = Locale.getDefault();
        if (locale != null) {
            String strLan = locale.getLanguage();
            return strLan != null && LangSelectFragment.EN.equals(strLan);
        }
        return false;
    }


    private void updateSourceLanguage(String code) {
        int count = this.spSourceAdapter.getCount();
        if (this.spSourceType == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            if (this.getLanguageName(code).equalsIgnoreCase(this.spSourceAdapter.getItem(i))) {
                this.spSourceType.setSelection(i, true);
                return;
            }
        }
        this.spSourceType.setSelection(0, true);
    }

    private void updateDestLanguage(String code) {
        if (code.equalsIgnoreCase(LangSelectFragment.SOURCE_LANGUAGE_CODE[0]) || code.equalsIgnoreCase(LangSelectFragment.SP_SOURCE_LIST.get(0))) {
            this.dstLanguage = LangSelectFragment.DEST_LANGUAGE_CODE[0];
            return;
        }
        if (this.spDestType == null) {
            return;
        }
        int count = this.spDestAdapter.getCount();
        for (int i = 0; i < count; i++) {
            if (this.getLanguageName(code).equalsIgnoreCase(this.spDestAdapter.getItem(i))) {
                this.spDestType.setSelection(i, true);
                return;
            }
        }
        this.spDestType.setSelection(0, true);
    }

    private String getLanguageName(String code) {
        int index = 0;
        for (int i = 0; i < LangSelectFragment.SOURCE_LANGUAGE_CODE.length; i++) {
            if (code.equalsIgnoreCase(LangSelectFragment.SOURCE_LANGUAGE_CODE[i])) {
                index = i;
                break;
            }
        }
        return this.spSourceAdapter.getItem(index);
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(getActivity(), permission)) {
                allNeededPermissions.add(permission);
            }
        }
        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    getActivity(), allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(getActivity(), permission)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            SmartLog.i(TAG, "Permission granted: " + permission);
            return true;
        }
        SmartLog.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    getActivity().getPackageManager()
                            .getPackageInfo(getActivity().getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return new String[0];
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != PERMISSION_REQUESTS) {
            return;
        }
        boolean isNeedShowDiag = false;
        for (int i = 0; i < permissions.length; i++) {
            if ((permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE) && grantResults[i] != PackageManager.PERMISSION_GRANTED)
                    || (permissions[i].equals(Manifest.permission.CAMERA) && grantResults[i] != PackageManager.PERMISSION_GRANTED)) {
                isNeedShowDiag = true;
            }
        }
        if (isNeedShowDiag && !ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CALL_PHONE)) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.camera_permission_rationale))
                    .setPositiveButton(getString(R.string.settings), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                            startActivityForResult(intent, 200);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getActivity().finish();
                        }
                    }).create();
            dialog.show();
        }
    }

    private void createRemoteTranslator() {
        MLApplication.getInstance().setApiKey("CgB6e3x9B3TFxMBwzp5Fw9sBMiFvLeGxNWHQvlLNvlmqNhx7IOVxSrxsFHNMJkzjVuc15rSVqoz8Dq0MgsmXtvxV");
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
                    remoteDisplaySuccessWithoutSourceText(text);
                } else {
                    displayFailure();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                displayFailure();
            }
        });
    }

    private void remoteDisplaySuccessWithoutSourceText(String test) {
        String[] sourceLines = sourceText.split("\n");
        String[] drtLines = test.split("\n");
        if (output_.getContext().toString() != null) {
            output_.setText("");
        }
        for (int i = 0; i < sourceLines.length && i < drtLines.length; i++) {
            this.output_.append(drtLines[i] + "\n");
        }
        Toast.makeText(getActivity().getApplicationContext(), R.string.translate_success, Toast.LENGTH_SHORT).show();
    }

    private void displayFailure() {
        Toast.makeText(getActivity().getApplicationContext(), "Fail", Toast.LENGTH_SHORT).show();
    }

    public void translatePhoto(View view) {
//        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        Bundle args = new Bundle();
        args.putString(Constant.SOURCE_VALUE, srcLanguage);
        args.putString(Constant.DEST_VALUE, dstLanguage);
        Navigation.findNavController(view).navigate(R.id.action_select_lang_dest_to_remoteTranslateFragment, args);
//        Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.view1);
//        Fragment fRemoteTranslate =  new RemoteTranslateFragment();
//        fRemoteTranslate.setArguments(args);
//        fRemoteTranslate.setTargetFragment(this, 1);
//        fragmentTransaction.add(R.id.view1, fRemoteTranslate);
//        fragmentTransaction.hide(currentFragment);
//        fragmentTransaction.show(fRemoteTranslate);
//        fragmentTransaction.commit();

    }

    public void asrTranslator(View view) {
        if (this.srcLanguage != "EN" && this.srcLanguage != "ZH") {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setMessage("Sorry, we only support English and Chinese as source language now. Please reset your choice. Thank you!");
            alertDialogBuilder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }else {
//            FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
//            Fragment asrAnalyseFragment =  new AsrAnalyseFragment();
//            asrAnalyseFragment.setTargetFragment(this, 1);
//            fragmentTransaction.add(R.id.view1, asrAnalyseFragment);
//            Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.view1);
//            fragmentTransaction.hide(currentFragment);
//            fragmentTransaction.show(asrAnalyseFragment);
//            fragmentTransaction.commit();
            Bundle args = new Bundle();
            args.putString(Constant.SOURCE_VALUE, srcLanguage);
            args.putString(Constant.DEST_VALUE, dstLanguage);
            Navigation.findNavController(view).navigate(R.id.action_select_lang_dest_to_asrAnalyseFragment, args);
        }
    }

    public void doLanguageSwitch(View view) {
        String str = this.srcLanguage;
        this.srcLanguage = this.dstLanguage;
        this.dstLanguage = str;
        this.updateSourceLanguage(this.srcLanguage);
        this.updateDestLanguage(this.dstLanguage);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSwitchLang:
                doLanguageSwitch(v);
                break;
            case R.id.select_photo:
                translatePhoto(v);
                break;
            case R.id.asr:
                asrTranslator(v);
                break;
            default:
                break;

        }
    }


}