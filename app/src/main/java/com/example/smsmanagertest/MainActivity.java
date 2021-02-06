package com.example.smsmanagertest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView mTvSend;
    private EditText mEtMessage;
    public static final int KEY_SEND_SMS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvSend = findViewById(R.id.tvSend);
        mEtMessage = findViewById(R.id.etMessage);
        mTvSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
            }
        });
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            //系统对话框
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, KEY_SEND_SMS);
        } else {
            sendSM(mEtMessage.getText().toString(), "哈哈哈哈哈哈");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean isPermissions = false;
        switch (requestCode) {
            case KEY_SEND_SMS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSM(mEtMessage.getText().toString(), "哈哈哈哈哈哈");
                    isPermissions = true;
                } else {
                    isPermissions = false;
                }
                break;
            default:
                break;
        }
        List<String> list = new ArrayList<>();
        for (int i = 0;i<grantResults.length;i++){
            list.add(String.valueOf(grantResults[i]));
        }
        if (isSomePermissionPermanentlyDenied(this,list)) {
            showPermissionsDialog();
        }
    }

    AlertDialog mDialog;

    private void showPermissionsDialog() {
        final String mPackName = "com.example.smsmanagertest";
        mDialog = new AlertDialog.Builder(MainActivity.this).setMessage("已禁止权限，请手动赋予")
                .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri packageURI = Uri.parse("package:" + mPackName);
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                        startActivity(intent);
                        mDialog.dismiss();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDialog.dismiss();
                    }
                }).show();

    }

    /**
     * 检测权限是否有勾选"不再询问"，也就是永久拒绝
     */
    private boolean isSomePermissionPermanentlyDenied(Activity activity, List<String> perms) {
        if (perms == null || perms.isEmpty()) {
            return false;
        }
        for (String denied : perms) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, denied)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 调用系统提供的短信接口发送短信,也就是跳转到‘短信页面’
     * 不需要发短信的权限
     *
     * @param phoneNumber 想要发送给的电话号码
     * @param message     发送的信息
     */
    private void sendSMTo(String phoneNumber, String message) {
        if (PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
            //Uri.parse("smsto") 这里是转换为指定Uri,固定写法
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phoneNumber));
            intent.putExtra("sms_body", message);
            startActivity(intent);
        }
    }

    /**
     * 调用系统提供的短信接口发送短信
     */
    private void sendSM(String phoneNumber, String message) {
        if (phoneNumber.length() == 0 || phoneNumber.equals("null")) {
            Toast.makeText(this, "请输入号码", Toast.LENGTH_SHORT).show();
            return;
        }
        //获取短信管理器
        SmsManager smsManager = SmsManager.getDefault();
        List<String> divideContents = smsManager.divideMessage(message);
        for (String text : divideContents) {
            smsManager.sendTextMessage(phoneNumber, null, text, null, null);
        }
    }
}