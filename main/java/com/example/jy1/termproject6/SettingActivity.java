package com.example.jy1.termproject6;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static com.example.jy1.termproject6.MapActivity.PHONENUM;
import static com.example.jy1.termproject6.main_page.PICK_CONTACT_REQUEST;

/**
 * Created by 임지영 on 2017-11-25.
 */

public class SettingActivity extends AppCompatActivity {

    private TextView prevNumTV;
    private Button changeBTN;
    private String phonenum;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setting);

        prevNumTV = (TextView) findViewById(R.id.phoneNumber);

        phonenum = getIntent().getStringExtra(PHONENUM);
        prevNumTV.setText(phonenum);

        changeBTN = (Button) findViewById(R.id.change);
        changeBTN.setOnClickListener(onClickListener);

        findViewById(R.id.ok).setOnClickListener(onClickListener);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.change:
                    Intent pickContactIntent = new Intent( Intent.ACTION_PICK, Uri.parse("content://contacts"));
                    pickContactIntent.setType( ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                    startActivityForResult( pickContactIntent, PICK_CONTACT_REQUEST);
                    break;

                case R.id.ok:
                    finish();
                    break;

            }
        }
    };

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data){
        if( requestCode == PICK_CONTACT_REQUEST){
            if( resultCode == RESULT_OK){
                Uri contactUri = data.getData();
                String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
                Cursor cursor = getContentResolver().query( contactUri, projection, null, null, null);
                cursor.moveToFirst();

                int column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                phonenum = cursor.getString(column).replace("-", "");
                prevNumTV.setText(phonenum);
                Intent intent = new Intent();
                intent.putExtra(PHONENUM, phonenum);
                setResult(RESULT_OK, intent);
            }
        }
    }
}
