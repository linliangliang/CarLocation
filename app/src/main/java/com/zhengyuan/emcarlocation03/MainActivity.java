package com.zhengyuan.emcarlocation03;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.CoordinateConverter;
import com.amap.api.location.DPoint;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class MainActivity extends CheckPermissionsActivity implements AMapLocationListener,
        LocationSource, View.OnClickListener {

    public EditText mJingDdEditText;
    public EditText mWeiDuEditText;
    public Button mRefreshMapButton;
    public ToggleButton mapStyleTB;
    public EditText showLocationInfoEditText;
    public Button getLocationButton;
    public Button resetLocation;
    public Button endGetLocationButton;
    public TextView getInfoState;
    //获取NotificationManager实例
    public NotificationManager notifyManager;
    private int notifyId = 10001;//只有一个通知类型，可用写死
    private Button test;//显示悬浮框的button按钮，经过测试不能保证
    private Button permanentButton;//生成一个像素，永驻前台

    private MapView mapView;
    private AMap aMap;


    //定位需要的声明
    private AMapLocationClient mLocationClient = null;//定位发起端
    private AMapLocationClientOption mLocationOption = new AMapLocationClientOption();//定位参数
    private OnLocationChangedListener mListener = null;//定位监听器
    private LocationManager locationManager;
    //标识，用于判断是否只显示一次定位信息和用户重新定位
    private boolean isFirstLoc = true;


    private final int FLOATING_PERMISSION = 100;//获取悬浮窗的权限
    ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取地图控件引用

        mapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mapView.onCreate(savedInstanceState)，创建地图
        mapView.onCreate(savedInstanceState);
        initView();
        initEvent();
        init();
        //设置显示定位按钮 并且可以点击
        UiSettings settings = aMap.getUiSettings();
        //设置定位监听
        aMap.setLocationSource(this);
        // 是否显示定位按钮
        settings.setMyLocationButtonEnabled(true);
        //定位的小图标
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.icon_geo));
        myLocationStyle.radiusFillColor(android.R.color.transparent);
        myLocationStyle.strokeColor(android.R.color.transparent);
        aMap.setMyLocationStyle(myLocationStyle);

        //初始化定位
        initLocation();
        //开始定位
        //initLoc();
        //禁用测试其他方法 initScreenBroadcastReceiver();//锁屏广播处理
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.refreshMapButton:
                //刷新地图显示
                String lngStr = mJingDdEditText.getText().toString().trim();
                String latStr = mWeiDuEditText.getText().toString().trim();
                if (lngStr == null || "".equals(lngStr) || latStr == null || "".equals(latStr)) {
                    Toast.makeText(MainActivity.this, "经度或纬度不能为空！", Toast.LENGTH_SHORT).show();
                } else {
                    double lngDou = Double.parseDouble(lngStr);
                    double latDou = Double.parseDouble(latStr);
                    boolean tem = refreshMapViewByLngLat(lngDou, latDou);
                    if (!tem) {
                        Toast.makeText(MainActivity.this, "该坐标不能用于高德地图", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.getLocationButton:
                startLocation();
                getInfoState.setText("正在定位...");
                sendNotification();
                break;
            case R.id.resetLocation:
                //重置经纬度
                mJingDdEditText.setText("114.28266610326988");
                mWeiDuEditText.setText("30.629447843744206");
                break;
            case R.id.endGetLocationButton:
                //停止定位
                mLocationClient.stopLocation();
                getInfoState.setText("定位关闭状态");
                cancelNotification();
                break;
            case R.id.otherActivity:
                startFloatingButtonService(view);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FLOATING_PERMISSION) {//悬浮窗
            if (!Settings.canDrawOverlays(this)) {
                //Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                startService(new Intent(MainActivity.this, FloatingCarLocationTip.class));
            }
        }
    }

    /*
    * 初始化控件ID
    * */
    public void initView() {

        mJingDdEditText = (EditText) findViewById(R.id.jingDuEditText);
        mWeiDuEditText = (EditText) findViewById(R.id.wenDuEditText);
        mRefreshMapButton = (Button) findViewById(R.id.refreshMapButton);
        mapStyleTB = (ToggleButton) findViewById(R.id.mapStyleTB);
        showLocationInfoEditText = (EditText) findViewById(R.id.showLocationInfoEditText);
        getLocationButton = (Button) findViewById(R.id.getLocationButton);
        resetLocation = (Button) findViewById(R.id.resetLocation);
        endGetLocationButton = (Button) findViewById(R.id.endGetLocationButton);
        getInfoState = (TextView) findViewById(R.id.getInfoState);
        test = (Button) findViewById(R.id.otherActivity);
        permanentButton = (Button) findViewById(R.id.permanentButton);
    }

    /*
    * 初始化控件响应事件
    * */
    public void initEvent() {
        mRefreshMapButton.setOnClickListener(this);
        getLocationButton.setOnClickListener(this);
        resetLocation.setOnClickListener(this);
        endGetLocationButton.setOnClickListener(this);
        test.setOnClickListener(this);
        permanentButton.setOnClickListener(this);
        mapStyleTB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // 设置使用卫星地图
                    aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                } else {
                    // 设置使用普通地图
                    aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                }
            }
        });
    }

    /*
    * 初始化地图显示
    * */
    public void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            // 设置地图的默认放大级别
            CameraUpdate cu = CameraUpdateFactory.zoomTo(20);
            aMap.moveCamera(cu);
            // 改变地图的倾斜度
            CameraUpdate tiltUpdate = CameraUpdateFactory.changeTilt(30);
            aMap.moveCamera(tiltUpdate);
        }
    }

    private void initLocation() {
        //初始化client
        mLocationClient = new AMapLocationClient(this.getApplicationContext());
        //设置定位参数
        mLocationClient.setLocationOption(getDefaultOption());
        // 设置定位监听
        mLocationClient.setLocationListener(locationListener);
    }

    /**
     * 获取默认参数设置
     *
     * @return
     */
    private AMapLocationClientOption getDefaultOption() {
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.setGpsFirst(true);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.setInterval(10000);//可选，设置定位间隔。默认为2秒
        mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        mOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
        mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
        mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        mOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
        return mOption;
    }

    /**
     * 定位监听
     */
    AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation loc) {
            if (null != loc) {
                //解析定位结果
                String result = Utils.getLocationStr(loc);
                Log.i("LocationInfo", result);

                Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                //刷新显示
                refreshMapViewByLngLat(Double.parseDouble(Utils.mlng), Double.parseDouble(Utils.mlat));
                mJingDdEditText.setText(Utils.mlng);
                mWeiDuEditText.setText(Utils.mlat);
                showLocationInfoEditText.setText(result);
            } else {
                showLocationInfoEditText.setText("定位失败。。。");
            }
        }
    };

    private void startLocation() {
        /*// 设置定位参数
        mLocationClient.setLocationOption(mLocationOption);*/
        // 启动定位
        mLocationClient.startLocation();
    }

    //定位
    private void initLoc() {
//          SDK在Android 6.0以上的版本需要进行运行检测的动态权限如下：
//                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                Manifest.permission.READ_EXTERNAL_STORAGE,
//                Manifest.permission.READ_PHONE_STATE

        /*//这里用到ACCESS_FINE_LOCATION与ACCESS_COARSE_LOCATION权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    WRITE_COARSE_LOCATION_REQUEST_CODE);//自定义的code
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    WRITE_COARSE_LOCATION_REQUEST_CODE);//自定义的code
        }*/
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置是否只定位一次,默认为false
        mLocationOption.setOnceLocation(false);
        //设置是否强制刷新WIFI，默认为强制刷新
        mLocationOption.setWifiActiveScan(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(5000);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    public boolean refreshMapViewByLngLat(Double dLng, Double dLat) {
        //如何判断该数据是高德地图可用的纬经度
        if (checkIsAvaliable(dLng, dLat)) {//可用
            // 将用户输入的经度、纬度封装成LatLng,第一个参数为纬度，第二个为精度
            LatLng pos = new LatLng(dLat, dLng);
            // 创建一个设置经纬度的CameraUpdate
            CameraUpdate cu = CameraUpdateFactory.changeLatLng(pos);
            // 更新地图的显示区域
            aMap.moveCamera(cu);
            // 创建MarkerOptions对象
            MarkerOptions markerOptions = new MarkerOptions();
            // 设置MarkerOptions的添加位置
            markerOptions.position(pos);
            // 设置MarkerOptions的标题
            markerOptions.title("当前位置");
            // 设置MarkerOptions的摘录信息
            //markerOptions.snippet("详细信息。。。。");
            // 设置MarkerOptions的图标
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            markerOptions.draggable(true);
            // 添加MarkerOptions（实际上就是添加Marker）
            aMap.clear();
            Marker marker = aMap.addMarker(markerOptions);
            return true;
        } else {
            return false;
        }
    }

    //判断坐标是否高德地图可用
    private boolean checkIsAvaliable(double lng, double lat) {
        //构造一个示例坐标，第一个参数是纬度，第二个参数是经度
        DPoint examplePoint = new DPoint(lat, lng);
        //初始化坐标工具类
        CoordinateConverter converter = new CoordinateConverter(
                getApplicationContext());
        //判断是否高德地图可用的坐标
        return converter.isAMapDataAvailable(examplePoint.getLatitude(), examplePoint.getLongitude());
    }


    //定位回调函数
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见官方定位类型表
                amapLocation.getLatitude();//获取纬度
                amapLocation.getLongitude();//获取经度
                amapLocation.getAccuracy();//获取精度信息
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(amapLocation.getTime());
                df.format(date);//定位时间
                amapLocation.getAddress();  // 地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                amapLocation.getCountry();  // 国家信息
                amapLocation.getProvince();  // 省信息
                amapLocation.getCity();  // 城市信息
                amapLocation.getDistrict();  // 城区信息
                amapLocation.getStreet();  // 街道信息
                amapLocation.getStreetNum();  // 街道门牌号信息
                amapLocation.getCityCode();  // 城市编码
                amapLocation.getAdCode();//地区编码

                // 如果不设置标志位，此时再拖动地图时，它会不断将地图移动到当前的位置
                if (isFirstLoc) {
                    //设置缩放级别
                    aMap.moveCamera(CameraUpdateFactory.zoomTo(20));
                    //将地图移动到定位点
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(amapLocation.getLatitude(),
                            amapLocation.getLongitude())));
                    //点击定位按钮 能够将地图的中心移动到定位点
                    mListener.onLocationChanged(amapLocation);
                    //添加图钉
                    aMap.addMarker(getMarkerOptions(amapLocation));
                    //获取定位信息
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(amapLocation.getCountry() + "" + amapLocation.getProvince()
                            + "" + amapLocation.getCity() + "" + amapLocation.getProvince()
                            + "" + amapLocation.getDistrict() + "" + amapLocation.getStreet()
                            + "" + amapLocation.getStreetNum());
                    Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_LONG).show();
                    isFirstLoc = false;
                }
            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + amapLocation.getErrorCode() + ", errInfo:"
                        + amapLocation.getErrorInfo());
                Toast.makeText(getApplicationContext(), "定位失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //  自定义一个图钉，并且设置图标，当我们点击图钉时，显示设置的信息
    private MarkerOptions getMarkerOptions(AMapLocation amapLocation) {
        //设置图钉选项
        MarkerOptions options = new MarkerOptions();
        //图标
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_geo));
        //位置
        options.position(new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude()));
        StringBuffer buffer = new StringBuffer();
        buffer.append(amapLocation.getCountry() + "" + amapLocation.getProvince() + ""
                + amapLocation.getCity() + "" + amapLocation.getDistrict()
                + "" + amapLocation.getStreet() + "" + amapLocation.getStreetNum());
        //标题
        options.title(buffer.toString());
        //子标题
        options.snippet("（您目前所在的位置）");
        //设置多少帧刷新一次图片资源
        options.period(60);
        return options;
    }

    /**
     * android3.0以上可用
     */
    private void sendNotification() {
        //int id = (int) (System.currentTimeMillis() / 1000);
        //实例化NotificationCompat.Builde并设置相关属性
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                //设置小图标
                .setSmallIcon(R.mipmap.notification16)
                //设置通知标题
                .setContentTitle("正在定位中...")
                //设置通知内容
                .setContentText("")
                //设置通知时间，默认为系统发出通知的时间，通常不用设置
                .setWhen(System.currentTimeMillis())
                //通知方式为声音
                .setDefaults(Notification.DEFAULT_SOUND)
                .setOngoing(true)  //不能滑动删除
                .setAutoCancel(false)//不能点击删除
                .setPriority(NotificationCompat.PRIORITY_MAX);  //优先级最高

        notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //通过builder.build()方法生成Notification对象,并发送通知,id=1
        if (notifyManager != null) {
            notifyManager.notify(notifyId, builder.build());
        }
    }

    /**
     * 取消通知栏的通知
     */
    private void cancelNotification() {
        if (notifyManager != null && notifyId > 0) {
            notifyManager.cancel(notifyId);
        }
    }

    /**
     * @param view 打开悬浮窗
     */
    public void startFloatingButtonService(View view) {
        if (FloatingCarLocationTip.isStarted) {
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT);
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), FLOATING_PERMISSION);
        } else {
            startService(new Intent(MainActivity.this, FloatingCarLocationTip.class));
        }
    }


    //--------------1像素--------------
    private ScreenReceiverUtil screenReceiverUtil;// 动态注册锁屏等广播

    private ScreenManager screenManager;// 1像素Activity管理类

    private void initScreenBroadcastReceiver() {
        screenReceiverUtil = new ScreenReceiverUtil(this);
        screenReceiverUtil.setScreenReceiverListener(mScreenListener);
        screenManager = ScreenManager.getScreenManagerInstance(this);
    }

    private ScreenReceiverUtil.ScreenStateListener mScreenListener = new ScreenReceiverUtil.ScreenStateListener() {
        @Override
        public void onScreenOn() {
            // 亮屏，移除"1像素"
            Log.i("Location", "onScreenOn");
            screenManager.finishActivity();
        }

        @Override
        public void onScreenOff() {
            Log.i("Location", "onScreenOff");
            screenManager.startActivity();
        }

        @Override
        public void onUserPresent() {
            // 解锁，暂不用，保留
            Log.i("Location", "onUserPresent");
        }
    };

    /**
     * @param mContext
     * @param packageName
     * @return 判断Activity是否存活
     */
    public static boolean isAPPALive(Context mContext, String packageName) {
        boolean isAPPRunning = false;
        // 获取activity管理对象
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        // 获取所有正在运行的app
        List<ActivityManager.RunningAppProcessInfo> appProcessInfoList = activityManager.getRunningAppProcesses();
        // 遍历，进程名即包名
        for (ActivityManager.RunningAppProcessInfo appInfo : appProcessInfoList) {
            if (packageName.equals(appInfo.processName)) {
                isAPPRunning = true;
                break;
            }
        }
        return isAPPRunning;
    }


    // 激活定位
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
    }

    // 停止定位
    @Override
    public void deactivate() {
        mListener = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelNotification();
        //在activity执行onDestroy时执行mapView.onDestroy()，销毁地图
        mapView.onDestroy();
        if (null != mLocationClient) {
            mLocationClient.onDestroy();
            mLocationClient = null;
        }
        mLocationOption = null;

        if (screenReceiverUtil != null) screenReceiverUtil.stopScreenReceiverListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mapView.onResume ()，重新绘制加载地图
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mapView.onPause ()，暂停地图的绘制
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }
}
