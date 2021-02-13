package com.example.smsmanagertest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
    //发送状态的 PendingIntent
    private PendingIntent mSentPI;
    //接收状态的 PendingIntent
    private PendingIntent mDeliverPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvSend = findViewById(R.id.tvSend);
        mEtMessage = findViewById(R.id.etMessage);
        //这里这样写实际上不好，实际上要写两个广播接收器的实体类，而不是匿名内部类，这样才可以反注册。
        getSmsSentIntent(MainActivity.this);
        getDeliverIntent(MainActivity.this);
        mTvSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
            }
        });
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
            smsManager.sendTextMessage(phoneNumber, null, text, mSentPI, mDeliverPI);
        }
    }

    /**
     * 获得返回发送状态的sentIntent
     *
     * @param mContext
     */
    private void getSmsSentIntent(final Context mContext) {
        //处理返回的发送状态
        String SENT_SMS_ACTION = "SENT_SMS_ACTION";
        Intent sentIntent = new Intent(SENT_SMS_ACTION);
        mSentPI = PendingIntent.getBroadcast(mContext, 0, sentIntent, 0);
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int resultCode = getResultCode();
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(context, "短信发送成功", Toast.LENGTH_SHORT).show();
                } else if (resultCode == SmsManager.RESULT_ERROR_GENERIC_FAILURE) {
                    Toast.makeText(context, "普通错误", Toast.LENGTH_SHORT).show();
                } else if (resultCode == SmsManager.RESULT_ERROR_RADIO_OFF) {
                    Toast.makeText(context, "无线广播被明确地关闭", Toast.LENGTH_SHORT).show();
                } else if (resultCode == SmsManager.RESULT_ERROR_NULL_PDU) {
                    Toast.makeText(context, "没有提供pdu", Toast.LENGTH_SHORT).show();
                } else if (resultCode == SmsManager.RESULT_ERROR_NO_SERVICE) {
                    Toast.makeText(context, "服务当前不可用", Toast.LENGTH_SHORT).show();
                }

            }
        }, new IntentFilter(SENT_SMS_ACTION));
    }

    /**
     * 处理返回接收状态的deliverIntent
     */
    private void getDeliverIntent(Context context) {
        //处理返回的接收状态
        String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
        //创建返回接收状态的Intent
        Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);
        mDeliverPI = PendingIntent.getBroadcast(context, 0, deliverIntent, 0);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "短信接收成功", Toast.LENGTH_SHORT).show();
            }
        }, new IntentFilter(DELIVERED_SMS_ACTION));
    }

    /********************关于校验的*****************************************************/

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
        if (requestCode == KEY_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSM(mEtMessage.getText().toString(), "哈哈哈哈哈哈");
            }
        }
        List<String> list = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            list.add(String.valueOf(grantResults[i]));
        }
        if (isSomePermissionPermanentlyDenied(this, list)) {
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
}