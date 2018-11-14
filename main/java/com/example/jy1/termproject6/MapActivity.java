package com.example.jy1.termproject6;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.jy1.termproject6.main_page.ISEXTRA;

/**
 * Created by 임지영 on 2017-11-04.
 */

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap googleMap;
    private static int REQUEST_PERMISSIONS = 1111;
    private static int CHANGE_PHONENUM = 2222;
    private LocationManager lm;
    private Marker marker;
    private TextView infoTV;

    private TextView totalTimeTV;
    private TextView totalDistanceTV;
    private TextView speedTV;
    private TextView chargeTV;
    private TextView distanceTV;

    private GoogleApiClient mGoogleApiClient;

    private String phonennum;

    public static final String PHONENUM = "PHONENUM";
    public static final String CONTENT = "CONTENT";
    private static int CHARGE = 100;        //단위요금
    private static int BASE_CHARGE = 3000; //기본료
    private static double METER = 4.057;    //초당 미터거리
    private static double METER_142 = 142; //기본 미터거리

    private Button startBTN;
    private Timer timer = new Timer();
    private TimerTask timerTask;

    private Button extraBTN; //할증버튼
    private int isExtra;

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("SMS_SENT_ACTION");
        intentFilter.addAction("SMS_DELIVERED_ACTION");
        registerReceiver(broadcastReceiver, intentFilter);
        registerSensor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lm != null) {
//            lm.removeUpdates(mLocationListener);
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }
        }
        unregisterReceiver(broadcastReceiver);
        unRegisterSensor();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        isExtra = getIntent().getIntExtra(ISEXTRA, 0);
        phonennum = getIntent().getStringExtra(PHONENUM);

        charge = BASE_CHARGE;

        infoTV = (TextView) findViewById(R.id.info);

        totalTimeTV = (TextView) findViewById(R.id.total_time);
        totalDistanceTV = (TextView) findViewById(R.id.total_distance);
        speedTV = (TextView) findViewById(R.id.speed);
        chargeTV = (TextView) findViewById(R.id.charge);
        distanceTV = (TextView) findViewById(R.id.distance);

        //지도 초기화
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        startBTN = (Button) findViewById(R.id.start);
        startBTN.setOnClickListener(onClickListener);
        findViewById(R.id.stop).setOnClickListener(onClickListener);
        findViewById(R.id.setting).setOnClickListener(onClickListener);

        //센서 초기화
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        extraBTN = (Button) findViewById(R.id.extra);
        extraBTN.setOnClickListener(onClickListener);

        //할증처리
        if (isExtra == 0) {
            // 할증버튼 활성화 아닌데 할증 시간인 경우(할증O)
            if(isExtraTime()){
                charge = (int) (BASE_CHARGE * 1.2f);
                CHARGE = 120;
                extraBTN.setSelected(true);
            } else{
                // 할증시간 아닌경우(할증X)
                charge = BASE_CHARGE;
                CHARGE = 100;
                extraBTN.setSelected(false);
            }
        } else {
            // 할증O
            charge = (int) (BASE_CHARGE * 1.2f);
            CHARGE = 120;
            extraBTN.setSelected(true);
        }

    }
    // 할증시간 체크
    private boolean isExtraTime(){
        long currentTime = System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("H");
        Date date = new Date();
        date.setTime(currentTime);
        String hour = simpleDateFormat.format(date);
        if(Integer.parseInt(hour) >= 0 && Integer.parseInt(hour) <= 4){
            return true;
        } else{
            return false;
        }
    }

    private void setTotalTime() {
        int min = total_time / 60;
        int sec = total_time % 60;
        if (min > 0) {
            totalTimeTV.setText(String.format("총 시간 : %d분 %d초", min, sec));
        } else {
            totalTimeTV.setText(String.format("총 시간 : %d초", sec));
        }
        if(isExtraTime()){
            if(!extraBTN.isSelected()){
                extraBTN.performClick();
            }
        } else{
            if(extraBTN.isSelected()){
                extraBTN.performClick();
            }
        }
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.start:
//                    checkLocation();
                    chkGpsService();

                    setTotalTime();

                    distanceTV.setText(String.format("미터기 : %dm", (int) based_dis));
                    startBTN.setEnabled(false);
                    timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setTotalTime();
                                }
                            });
                            total_time++;
                        }
                    };
                    timer.schedule(timerTask, 1000, 1000);

                    break;

                case R.id.stop:
//                    lm.removeUpdates(mLocationListener);
                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                        countDownTimer = null;
                    }
//                    if(timer != null){
//                        timer.cancel();
//                        timer.purge();
//                    }
                    if (timerTask != null) {
                        timerTask.cancel();
                    }
                  //  total_time = 0;
                    setTotalTime();
                    startBTN.setEnabled(true);
                    break;

                case R.id.setting:
                    Intent intent = new Intent(MapActivity.this, SettingActivity.class);
                    intent.putExtra(PHONENUM, phonennum);
                    startActivityForResult(intent, CHANGE_PHONENUM);
                    break;

                case R.id.extra:
                    if (!extraBTN.isSelected()) {
                        extraBTN.setSelected(true);
                        isExtra = 1;
                        // 할증 적용
                        CHARGE = 120;
                        if (startBTN.isEnabled()) {
                            charge = (int) (BASE_CHARGE * 1.2f);
                        }
                    } else {
                        extraBTN.setSelected(false);
                        isExtra = 0;
                        // 할증 해제
                        CHARGE = 100;
                        if (startBTN.isEnabled()) {
                            charge = BASE_CHARGE;
                        }
                    }
                    // 주행중 상태
                    if (!startBTN.isEnabled()) {
                        chargeTV.setText(String.format("요금 : %d원" + (isExtra == 1 ? "(할증)" : ""), charge));
                    }
                    break;
            }
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        setDefaultLocation();

        // 맵이 로드되면 GoogleApi 확인
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void permissionCheck() {
        // onRequestPermissionsResult()으로 결과 넘어감
        ArrayList<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CALL_PHONE);
        }

        if (permissions.size() > 0) {
            requestPermissions(permissions.toArray(new String[permissions.size()]), REQUEST_PERMISSIONS);
            return;
        }

        chkGpsService();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("ddd", "onActivityResult");

        if (requestCode == CHANGE_PHONENUM && resultCode == RESULT_OK) {
            phonennum = data.getStringExtra(PHONENUM);
        } else {
            chkGpsService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSIONS || grantResults.length < 1) {
            return;
        }

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                // 퍼미션 거부 했으면
                showPermissionDailog();
                return;
            }
        }
        chkGpsService();
    }

    private void showPermissionDailog() {

        // 설정에서 사용자가 직접 권한설정
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.location_permission_ask);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.setting, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // 설정화면 에서 onActivityResult()로 결과 넘어감
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_PERMISSIONS);
            }
        });
        builder.setCancelable(false);
        builder.create().show();
    }

    private boolean chkGpsService() {
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

            // GPS OFF 일때 Dialog 표시
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setMessage(R.string.location_service_setting_message);
            builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // GPS설정 화면으로 이동
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, REQUEST_PERMISSIONS);
                }
            });
//            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                    return;
//                }
//            });
            builder.create().show();
            return false;
        } else {
            Log.i("ddd", "444");
            checkLocation();
            return true;
        }
    }

    private LocationRequest locationRequest = new LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(1000)
            .setFastestInterval(500);

    private void checkLocation() {
        latlngs = new ArrayList<>();
        makeMarkerBitmap();
        calDistance = new CalDistance();
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, mLocationListener);

    }

    private ArrayList<LatLng> latlngs;
    private Polyline polyline;

    private CalDistance calDistance;
    private double total_dist; // 총 이동 거리
    private double charge_dist = METER_142; // 요금 거리
    private int total_time = 0; // 총 이동 시간
    private double dist; //현재 이동거리

    private double avg_speed;// 평균 속도

    private double start_lat; // 시작지점 경도
    private double start_long;// 시작지점 위도
    private long start_time;// 시작 시간


    private double prev_lat;// 이전 위도
    private double prev_long;// 이전 경도
    private long prev_time;//이전시간

    private int charge; //요금


    private CountDownTimer countDownTimer;
    private double based_dis = 2000;
    private boolean firstCharge;    // 처음 요금 부과확인

    private LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(final Location location) {
            Log.d("ddd", "onLocationChanged : ");

            //여기서 위치값이 갱신되면 이벤트가 발생한다.
            //값은 Location 형태로 리턴되며 좌표 출력 방법은 다음과 같다.

            double longitude = location.getLongitude(); //경도
            double latitude = location.getLatitude();   //위도
            float accuracy = location.getAccuracy();    //정확도

            //Gps 위치제공자에 의한 위치변화. 오차범위가 좁다.
            //Network 위치제공자에 의한 위치변화. Gps에 비해 정확도가 많이 떨어진다.

            if (!startBTN.isEnabled()) {
                /* 시작 시간,위치 */
                if (start_lat == 0L) {
                    start_lat = latitude;
                    start_long = longitude;
                    start_time = System.currentTimeMillis();
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 17.5f));
                }

                final long current_time = System.currentTimeMillis();
                if (prev_lat > 0) {

                    dist = calDistance.getDistance(prev_lat, prev_long, latitude, longitude);
//                dist = (int) (dist * 100) / 100.0; // 소수점 둘째 자리 계산
                    Log.i("ddd", "dist=" + dist + " accuracy=" + accuracy);
                    // 주행중 제약조건
                    avg_speed = (int) ((location.getSpeed() * 3600) / 1000);
                    if (location.getAccuracy() < 30.f && dist < 50) {
                        if (based_dis > 0) {
                            if (location.getSpeed() > METER) {
                                based_dis -= dist;
                            } else {
                                based_dis -= METER * ((current_time - prev_time) / 1000);
                            }
                            distanceTV.setText(String.format("미터기 : %dm", based_dis > 0 ? (int) based_dis : 0));
                        }

                        total_dist += dist > 2 ? dist : 0;

                        // 카메라 이동
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 17.5f));

                        // 마커 설정
                        if(marker != null) marker.remove();
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(new LatLng(latitude, longitude));
                        markerOptions.draggable(false);
                        markerOptions.title("현재위치");
                        markerOptions.snippet("위도 : " + longitude + " 경도 : " + latitude + " 정확도 : "+accuracy);
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(mDotMarkerBitmap));
                        marker = googleMap.addMarker(markerOptions);
//                  lm.removeUpdates(mLocationListener);

                        LatLng latLng = new LatLng(latitude, longitude);
                        latlngs.add(latLng);

                        if (polyline != null) polyline.remove();
                        polyline = googleMap.addPolyline(new PolylineOptions().color(0xFFFF0000).addAll(latlngs));
                    }

                    // 2km 이상일 때 요금 추가
                    if (based_dis < 0) {
                        // 첫 요금 부과
                        if (!firstCharge) {
                            charge += CHARGE;
                            firstCharge = true;
                        }

                        if (location.getSpeed() > (float) METER) {
                            charge_dist -= dist;
                        } else {
                            charge_dist -= METER * ((current_time - prev_time) / 1000);
                        }
                        distanceTV.setText(String.format("미터기 : %dm", charge_dist > 0 ? (int) charge_dist : 0));

                        if (countDownTimer == null) {
                            countDownTimer = new CountDownTimer(35000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) { //밀리언초를 분초밀리언초로
                                    Log.i("ddd", "millisUntilFinished=" + millisUntilFinished);

                                    if (charge_dist < 0) {
                                        charge += CHARGE;
                                        charge_dist = METER_142;
                                        distanceTV.setText(String.format("미터기 : %dm", (int) charge_dist));
                                        // 142m 이상 이동
                                        chargeTV.setText(String.format("요금 : %d원" + (isExtra == 1 ? "(할증)" : ""), charge));
                                        countDownTimer.cancel();
                                        countDownTimer.start();
                                    }
                                }

                                @Override
                                public void onFinish() {
                                    Log.i("ddd", "timer onFinish");
                                    charge += CHARGE;
                                    charge_dist = METER_142;
                                    distanceTV.setText(String.format("미터기 : %dm", (int) charge_dist));
                                    chargeTV.setText(String.format("요금 : %d원" + (isExtra == 1 ? "(할증)" : ""), charge));
                                    countDownTimer.start();
                                }
                            };
                            countDownTimer.start();
                        }

                        // 평균 속도 계산하기
                     //   avg_speed = (int) ((location.getSpeed() * 3600) / 1000); // 속도 km/h
//                    avg_speed = dist / TimeUnit.MILLISECONDS.toSeconds(current_time - prev_time);
//                    avg_speed = (int) (avg_speed * 100) / 100.0; // 소수점 둘째 자리
                    }
                }

                // 처리를 한 후 현재위치를 이전 위치로 지정
                prev_lat = latitude;
                prev_long = longitude;
                prev_time = current_time;


                totalDistanceTV.setText("총거리 : " + String.format("%.2fKm", (float) total_dist / 1000));
                speedTV.setText("평균속도 : " + String.format("%.2fKm/h", avg_speed));
                chargeTV.setText(String.format("요금 : %d원" + (isExtra == 1 ? "(할증)" : ""), charge));
                setTotalTime();
            }


        }

        public void onProviderDisabled(String provider) {
            // Disabled시
            Log.d("ddd", "onProviderDisabled, provider:" + provider);
            chkGpsService();
        }

        public void onProviderEnabled(String provider) {
            // Enabled시
            Log.d("ddd", "onProviderEnabled, provider:" + provider);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            // 변경시
            Log.d("ddd", "onStatusChanged, provider:" + provider + ", status:" + status + " ,Bundle:" + extras);
        }
    };

    private Bitmap mDotMarkerBitmap;

    private void makeMarkerBitmap() {
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        mDotMarkerBitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(mDotMarkerBitmap);
        Drawable shape = getResources().getDrawable(R.drawable.map_dot_red);
        shape.setBounds(0, 0, mDotMarkerBitmap.getWidth(), mDotMarkerBitmap.getHeight());
        shape.draw(canvas);
    }

    private String getFormattedTime(long time) {
        Date date = new Date(time);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy년MM월dd일 HH:mm:ss");
        return sdfNow.format(date);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("SMS_SENT_ACTION")) {
                intent.getStringExtra("aaa");
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        // 전송 성공
                        Toast.makeText(context, "전송 완료", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        // 전송 실패
                        Toast.makeText(context, "전송 실패", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        // 서비스 지역 아님
                        Toast.makeText(context, "서비스 지역이 아닙니다", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        // 무선 꺼짐
                        Toast.makeText(context, "무선(Radio)가 꺼져있습니다", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        // PDU 실패
                        Toast.makeText(context, "PDU Null", Toast.LENGTH_SHORT).show();
                        break;
                }

            } else if (intent.getAction().equals("SMS_DELIVERED_ACTION")) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        // 도착 완료
                        Toast.makeText(context, "SMS 도착 완료", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        // 도착 안됨
                        Toast.makeText(context, "SMS 도착 실패", Toast.LENGTH_SHORT).show();
                        break;
                }
            }


        }
    };


    public void onSensorChanged(SensorEvent event) {


        String phonennum = getIntent().getStringExtra(PHONENUM);
        String conetent = getIntent().getStringExtra(CONTENT);

        Intent intent = new Intent("SMS_SENT_ACTION");
        intent.putExtra("aaa", "aaa");
        PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        PendingIntent deliveredIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED_ACTION"), 0);

        SmsManager mSmsManager = SmsManager.getDefault();
        mSmsManager.sendTextMessage("01067635286", null, conetent, sentIntent, deliveredIntent);


    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("ddd", "onConnected");
        // GoogleApi 로드됨
        permissionCheck();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("ddd", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // GoogleApi 로드 실패
        Log.i("ddd", "onConnectionFailed");
    }


    public void setDefaultLocation() {
        //디폴트 위치, Seoul
        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
        String markerTitle = "위치정보 가져올 수 없음";
        String markerSnippet = "위치 퍼미션과 GPS 활성 여부 확인하세요";

        if (marker != null) marker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        marker = googleMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15);
        googleMap.moveCamera(cameraUpdate);

    }

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;

    private void registerSensor() {
        if (accelerometerSensor != null) {
            sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private void unRegisterSensor() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            processAccSensor(event);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private long lastTime;
    private float lastX;
    private float lastY;
    private float lastZ;
    private float x, y, z;
    private float speed;

    private static final int SHAKE_THRESHOLD = 4000;            // Shake speed
    private static final int DATA_X = 0;
    private static final int DATA_Y = 1;
    private static final int DATA_Z = 2;

    private int shakeCount;
    private AlertDialog phoneNumDialog;

    private void processAccSensor(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long gabOfTime = (currentTime - lastTime);

            if (gabOfTime > 100) {
                lastTime = currentTime;
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                speed = Math.abs(x + y + z - lastX - lastY - lastZ) / gabOfTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    String phoneNum = getIntent().getStringExtra(PHONENUM);
                    // 휴대폰 번호가 없으면 다이얼로그 생성
                    if (phoneNum == null || phoneNum.equals("") && (phoneNumDialog == null || !phoneNumDialog.isShowing())) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                        builder.setMessage("휴대폰 번호를 설정 해 주세요.");
                        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(MapActivity.this, SettingActivity.class);
                                intent.putExtra(PHONENUM, phonennum);
                                startActivityForResult(intent, CHANGE_PHONENUM);
                            }
                        });
                        builder.setNegativeButton(R.string.cancel, null);
                        phoneNumDialog = builder.create();
                        phoneNumDialog.show();
                    } else {
                        Log.i("ddd", "shaked");
                        // 흔들었음
                        shakeCount++;
                        if (shakeCount > 17) {

                            String content = getIntent().getStringExtra(CONTENT);
                            content += "\nhttp://maps.google.com/maps?q=loc:" + String.format("%f,%f", prev_lat, prev_long);

                            SmsManager mSmsManager = SmsManager.getDefault();
                            ArrayList<String> parts = mSmsManager.divideMessage(content);

                            mSmsManager.sendMultipartTextMessage(phonennum, null, parts, null, null);
//                            mSmsManager.sendTextMessage(phoneNum, null, content, null, null);
                        }
                    }
                }

                lastX = event.values[DATA_X];
                lastY = event.values[DATA_Y];
                lastZ = event.values[DATA_Z];
            }
        }
    }

    private int emergency;
    private int emergency2;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                emergency++;
                if (emergency > 4) {
                    String uri = "tel:112";
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(uri));
                    startActivity(intent);
                }
                break;

            case KeyEvent.KEYCODE_VOLUME_UP:
                emergency2++;
                if (emergency2 > 4) {
                    String uri = "tel:" + phonennum;
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse(uri));
                    startActivity(intent);
                }
                break;

            case KeyEvent.KEYCODE_BACK:
                AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                builder.setMessage("종료 하시겠습니까?");
                builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.create().show();
                break;
        }
        return true;
    }
}
