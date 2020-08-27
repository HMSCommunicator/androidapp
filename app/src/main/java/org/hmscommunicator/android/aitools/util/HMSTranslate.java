package org.hmscommunicator.android.aitools.util;

import android.content.Context;
import android.widget.Toast;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.translate.MLTranslatorFactory;
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslateSetting;
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslator;

public class HMSTranslate {
    private static String TAG = "HMSTranslate";
    private MLRemoteTranslator translator;
    private String dstLanguage;
    private String srcLanguage;
    private String sourceText;
    private Context context;
    private String result;

    public HMSTranslate(String dstLanguage, String srcLanguage, String sourceText, Context context) {
        this.dstLanguage = dstLanguage;
        this.srcLanguage = srcLanguage;
        this.sourceText = sourceText;
        this.context = context;
        //createRemoteTranslator();
    }
    public synchronized void createRemoteTranslator() {
        MLRemoteTranslateSetting.Factory factory = new MLRemoteTranslateSetting
                .Factory()
                // Set the target language code. The ISO 639-1 standard is used.
                .setTargetLangCode(dstLanguage);
        if (!srcLanguage.equals("AUTO")) {
            // Set the source language code. The ISO 639-1 standard is used.
            factory.setSourceLangCode(srcLanguage);
        }
        this.translator = MLTranslatorFactory.getInstance().getRemoteTranslator(factory.create());
        final Task<String> task = translator.asyncTranslate(sourceText);
        task.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String text) {
                if (text != null) {
                    //Log.d(TAG, text);
                    result = text;
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

//        if (translator!= null) {
//            translator.stop();
//        }
    }

    public synchronized String getResult() {
        return result;
    }
    private void displayFailure() {
        Toast.makeText(context.getApplicationContext(), "Fail", Toast.LENGTH_SHORT).show();
    }
}
