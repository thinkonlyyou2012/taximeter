package com.example.jy1.termproject6;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import static com.example.jy1.termproject6.MapActivity.CONTENT;
import static com.example.jy1.termproject6.MapActivity.PHONENUM;

/**
 * Created by 임지영 on 2017-11-03.
 */

public class main_page extends AppCompatActivity {
    public static final int PICK_CONTACT_REQUEST = 1;
    private String number;
    private EditText editText;
    private Button extraBTN;
    private int isExtra;
    public static final String ISEXTRA = "ISEXTRA";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);

        Button btn = (Button) findViewById(R.id.searchPhonenum);
        btn.setOnClickListener(btnClickLister);

        editText = (EditText) findViewById(R.id.phoneNumber);

        number = "";

        final String[] location = {"서울"};
        Spinner spinner = (Spinner) findViewById(R.id.spinner1);
        ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, location);
        spinner.setAdapter(adapter);

        extraBTN = (Button) findViewById(R.id.extra);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("오차가 발생할 수 있으니 참고용으로 사용 해주세요.");
        builder.setPositiveButton(R.string.confirm, null);
        builder.create().show();
    }


    public void startBtnClicked(View v) {
        Intent intent = new Intent(main_page.this, MapActivity.class);

        intent.putExtra(PHONENUM, editText.getText().toString().trim());
        intent.putExtra(CONTENT, "도와주세요..! 현재 택시를 타고 있는데 긴급 상황입니다. 차량 번호: " + ((EditText) findViewById(R.id.carID)).getText().toString().trim());
        intent.putExtra(ISEXTRA, isExtra);
        startActivity(intent);
    }

    public void extraBtnClicked(View v){
        if(!extraBTN.isSelected()){
            extraBTN.setSelected(true);
            // 할증 적용
            isExtra = 1;
        } else{
            extraBTN.setSelected(false);
            // 할증 해제
            isExtra = 0;
        }
    }


    View.OnClickListener btnClickLister = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
            pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
        }

    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri contactUri = data.getData();
                String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
                Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
                cursor.moveToFirst();

                int column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                number = cursor.getString(column).replace("-", "");

                editText.setText(number);
            }
        }
    }


}
