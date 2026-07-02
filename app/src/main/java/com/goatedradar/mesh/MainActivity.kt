package com.goatedradar.mesh

import android.Manifest; import android.bluetooth.*; import android.bluetooth.le.*
import android.content.*; import android.content.pm.*; import android.graphics.*; import android.graphics.drawable.*
import android.os.*; import android.view.*; import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat; import androidx.core.content.ContextCompat
import java.util.*; import java.util.concurrent.*; import kotlin.math.*

class MainActivity : AppCompatActivity() {
    val bg=0xFF050510.toInt();val grn=0xFF00ff88.toInt();val red=0xFFff3344.toInt();val wh=0xCCffffff.toInt();val dim=0xFF444466.toInt()
    val devices=ConcurrentHashMap<String,Blip>();var scanner:BluetoothLeScanner?=null;var sweepAngle=0f
    var myAddr="";var painting=false;lateinit var radar:RadarView;lateinit var cnt:TextView;lateinit var stat:TextView
    val handler=Handler(Looper.getMainLooper());var uid="";var lat=0.0;var lng=0.0
    data class Blip(val addr:String,var name:String,var rssi:Int,var angle:Float,var radius:Float,var ts:Long)

    override fun onCreate(s:Bundle?){super.onCreate(s)
        uid=getSharedPreferences("rr",0).getString("uid",null)?:UUID.randomUUID().toString().take(6)
        getSharedPreferences("rr",0).edit().putString("uid",uid).apply()
        if(!check())return
        window.statusBarColor=bg;window.navigationBarColor=bg;myAddr=BluetoothAdapter.getDefaultAdapter()?.address?:""
        build();setContentView(findViewById(android.R.id.content) ?: run{val l=LinearLayout(this);setContentView(l);l})
        scanner=BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner
        startSweep();startScan()}

    fun check():Boolean{val p=listOf(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.ACCESS_FINE_LOCATION)
        val n=p.filter{checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED};if(n.isNotEmpty()){requestPermissions(n.toTypedArray(),42);return false};return true}

    fun build(){val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(0,32,0,0)}
        val hdr=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(18,14,18,10);gravity=Gravity.CENTER_VERTICAL}
        hdr.addView(tv("G O A T E D   R A D A R",wh,18f,true).apply{layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        cnt=tv("0 blips",grn,12f,true).apply{setBackgroundDrawable(pill(grn));setPadding(12,4,12,4)};hdr.addView(cnt);root.addView(hdr)
        stat=tv("\u25CF scanning...",grn,10f,false).apply{setPadding(18,2,18,4)};root.addView(stat)
        // Radar view
        radar=RadarView(this);root.addView(radar,LinearLayout.LayoutParams(-1,-1))
        // Legend
        val lg=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(18,8,18,12);gravity=Gravity.CENTER}
        lg.addView(tv("<1m",grn,10f,false).apply{setPadding(0,0,12,0)})
        lg.addView(tv("3m",0xFF88cc00.toInt(),10f,false).apply{setPadding(0,0,12,0)})
        lg.addView(tv("10m",0xFFffaa00.toInt(),10f,false).apply{setPadding(0,0,12,0)})
        lg.addView(tv("20m+",red,10f,false));root.addView(lg)
        setContentView(root)}

    fun startSweep(){Thread{while(true){sweepAngle=(sweepAngle+3)%360;handler.post{radar.invalidate()};Thread.sleep(30)}}.start()}

    fun startScan(){scanner?:return
        try{scanner!!.startScan(null,ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(),object:ScanCallback(){
            override fun onScanResult(cb:Int,r:ScanResult){val ad=r.device.address;if(ad==myAddr)return
                val angle=Random().nextFloat()*360f;val radius=(r.rssi+100f)/100f
                val name=r.device.name?:r.scanRecord?.deviceName?:"Unknown"
                devices[ad]=Blip(ad,name,r.rssi,angle,radius,System.currentTimeMillis());handler.post{updateCount();radar.invalidate()}}
            override fun onScanFailed(c:Int){stat.text="Scan error"}})}catch(e:Exception){}}

    fun updateCount(){cnt.text="${devices.size} blips";stat.text="\u25CF ${devices.size} devices on radar"}

    fun tv(t:String,c:Int,s:Float,bold:Boolean=false)=TextView(this).apply{text=t;setTextColor(c);textSize=s;if(bold)setTypeface(null,Typeface.BOLD)}
    fun pill(c:Int)=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;cornerRadius=dp(4).toFloat();setColor(c);setStroke(1,c and 0x20ffffff.toInt())}
    fun dp(d:Int):Int=(d*resources.displayMetrics.density).toInt()
    override fun onRequestPermissionsResult(rc:Int,p:Array<String>,g:IntArray){super.onRequestPermissionsResult(rc,p,g);if(g.all{it==PackageManager.PERMISSION_GRANTED})recreate()}

    inner class RadarView(ctx:Context):View(ctx){
        val ringPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;color=grn and 0x30ffffff.toInt();strokeWidth=1.5f}
        val sweepPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=grn and 0x15ffffff.toInt();style=Paint.Style.FILL}
        val dotPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.FILL}
        val textPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=wh;textSize=dp(8).toFloat();textAlign=Paint.Align.CENTER}
        val centerPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=grn;style=Paint.Style.FILL}

        override fun onDraw(c:Canvas){super.onDraw(c)
            val cx=width/2f;val cy=height/2f;val maxR=min(cx,cy)*0.85f
            // Draw rings
            for(i in 1..4){c.drawCircle(cx,cy,maxR*i/4,ringPaint)}
            // Crosshair
            c.drawLine(cx,cy-maxR,cx,cy+maxR,ringPaint);c.drawLine(cx-maxR,cy,cx+maxR,cy,ringPaint)
            // Sweep
            val sweepRad=Math.toRadians(sweepAngle.toDouble()).toFloat()
            c.drawArc(RectF(cx-maxR,cy-maxR,cx+maxR,cy+maxR),sweepAngle-45,90,true,sweepPaint)
            // Blips
            val now=System.currentTimeMillis()
            devices.values.forEach{b->
                val r=b.radius*maxR;val ang=Math.toRadians(b.angle.toDouble()).toFloat()
                val x=cx+r*cos(ang);val y=cy+r*sin(ang)
                val clr=when{b.rssi>-50->grn;b.rssi>-70->0xFF88cc00.toInt();b.rssi>-85->0xFFffaa00.toInt();else->red}
                val alpha=min(255f,max(50f,(now-b.ts)/5000f*255f)).toInt()
                dotPaint.color=clr and (alpha shl 24 or 0xFFFFFF)
                c.drawCircle(x,y,dp(4).toFloat(),dotPaint)
                // Name label for strongest signals
                if(b.rssi>-70){
                    textPaint.color=wh and (alpha shl 24 or 0xFFFFFF)
                    c.drawText(b.name.take(8),x,y-dp(10).toFloat(),textPaint)}
            }
            // Center dot
            c.drawCircle(cx,cy,dp(5).toFloat(),centerPaint)
            // GPS
            if(lat!=0.0){
                textPaint.color=wh;c.drawText("\u2022 ${"%.4f".format(lat)},${"%.4f".format(lng)}",cx,cy+dp(16).toFloat(),textPaint)}
            invalidate()}
    }
}
