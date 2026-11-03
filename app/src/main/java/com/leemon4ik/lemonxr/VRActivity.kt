package com.leemon4ik.lemonxr

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.*
import android.net.Uri
import android.os.*
import android.view.*
import android.widget.EditText
import android.widget.Toast
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class VRActivity : Activity(), SensorEventListener {
    private val prefs by lazy { getSharedPreferences("lemonxr_settings", MODE_PRIVATE) }
    private lateinit var ui: VRView
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var landmarker: HandLandmarker? = null
    @Volatile private var bitmap: Bitmap? = null
    @Volatile private var landmarks: List<List<PointF>> = emptyList()
    @Volatile private var cameraStatus = ""
    private var opening = false
    private var pinch = false
    private var fist = false
    private var lastTap = 0L
    private lateinit var sensors: SensorManager
    @Volatile private var yaw = 0f
    @Volatile private var pitch = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = 5894 or 1024 or 512
        sensors = getSystemService(SENSOR_SERVICE) as SensorManager
        ui = VRView(this)
        setContentView(ui)
        launchCamera()
    }

    private fun launchCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 41)
        else openCamera()
    }

    override fun onRequestPermissionsResult(r: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(r, p, g)
        if (r == 41 && g.firstOrNull() == PackageManager.PERMISSION_GRANTED) openCamera()
        else { cameraStatus = "CAMERA PERMISSION DENIED"; ui.invalidate() }
    }

    private fun openCamera() {
        if (opening || isFinishing) return
        opening = true
        worker = HandlerThread("LemonXR-Camera").also { it.start() }
        handler = Handler(worker!!.looper)
        try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            val rear = manager.cameraIdList.filter { manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK }
            val id = prefs.getString("selected_camera", null)?.takeIf { it in rear } ?: rear.firstOrNull()
            if (id == null) { cameraStatus = "NO REAR CAMERA"; ui.invalidate(); return }
            val chars = manager.getCameraCharacteristics(id)
            val rotation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            val sizes = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.getOutputSizes(ImageFormat.YUV_420_888).orEmpty()
            val size = sizes.firstOrNull { it.width == 640 && it.height == 480 } ?: sizes.minByOrNull { abs(it.width * it.height - 307200) } ?: error("No camera size")
            reader = ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2)
            reader!!.setOnImageAvailableListener({ src ->
                val image = src.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    var frame = convert(image)
                    if (rotation != 0) frame = Bitmap.createBitmap(frame, 0, 0, frame.width, frame.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
                    bitmap = frame
                    track(frame)
                    runOnUiThread { ui.invalidate() }
                } catch (_: Exception) { } finally { image.close() }
            }, handler)
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(c: CameraDevice) { camera = c; preview() }
                override fun onDisconnected(c: CameraDevice) { c.close(); camera = null; cameraStatus = "CAMERA DISCONNECTED"; runOnUiThread { ui.invalidate() } }
                override fun onError(c: CameraDevice, e: Int) { c.close(); camera = null; cameraStatus = "CAMERA ERROR"; runOnUiThread { ui.invalidate() } }
            }, handler)
        } catch (_: Exception) { cameraStatus = "CAMERA UNAVAILABLE"; opening = false; ui.invalidate() }
    }

    private fun preview() {
        val cam = camera ?: return
        val surface = reader?.surface ?: return
        try {
            val request = cam.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            }
            cam.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) { session = s; try { s.setRepeatingRequest(request.build(), null, handler) } catch (_: Exception) { cameraStatus = "PREVIEW FAILED" } }
                override fun onConfigureFailed(s: CameraCaptureSession) { cameraStatus = "PREVIEW FAILED" }
            }, handler)
        } catch (_: Exception) { cameraStatus = "CAMERA SESSION FAILED" }
    }

    private fun convert(i: Image): Bitmap {
        val w = i.width; val h = i.height; val p = i.planes; val out = ByteArray(w*h*3/2); var n = 0
        val y = p[0]; val yb = y.buffer
        for (r in 0 until h) for (c in 0 until w) out[n++] = yb.get(r*y.rowStride+c*y.pixelStride)
        val u = p[1]; val v = p[2]; val ub = u.buffer; val vb = v.buffer
        for (r in 0 until h/2) for (c in 0 until w/2) { out[n++] = vb.get(r*v.rowStride+c*v.pixelStride); out[n++] = ub.get(r*u.rowStride+c*u.pixelStride) }
        val jpeg = ByteArrayOutputStream()
        YuvImage(out, ImageFormat.NV21, w, h, null).compressToJpeg(Rect(0,0,w,h), 68, jpeg)
        return BitmapFactory.decodeByteArray(jpeg.toByteArray(),0,jpeg.size())!!
    }

    private fun track(b: Bitmap) {
        if (prefs.getString("control_mode","headset") == "headset") { landmarks = emptyList(); return }
        try {
            if (landmarker == null) {
                val o = HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build())
                    .setRunningMode(RunningMode.VIDEO).setNumHands(2).build()
                landmarker = HandLandmarker.createFromOptions(this,o)
            }
            val result = landmarker!!.detectForVideo(BitmapImageBuilder(b).build(),SystemClock.uptimeMillis())
            val hands = result.landmarks().map { list -> list.map { PointF(it.x(),it.y()) } }
            landmarks = hands
            val h = hands.firstOrNull()
            if (h != null && h.size >= 21) {
                val p = sqrt((h[8].x-h[4].x)*(h[8].x-h[4].x)+(h[8].y-h[4].y)*(h[8].y-h[4].y)) < .055f
                val f = listOf(8,12,16,20).all { tip ->
                    val base = when(tip) { 8->5; 12->9; 16->13; else->17 }
                    val a=h[tip]; val w=h[0]; val m=h[base]
                    sqrt((a.x-w.x)*(a.x-w.x)+(a.y-w.y)*(a.y-w.y)) <
                        sqrt((m.x-w.x)*(m.x-w.x)+(m.y-w.y)*(m.y-w.y))*1.2f
                }
                if (f && !fist) runOnUiThread { ui.toggleMenu() }
                if (p && !pinch && SystemClock.elapsedRealtime()-lastTap>450) {
                    lastTap=SystemClock.elapsedRealtime()
                    runOnUiThread { ui.virtualTap(h[8].x*480f,h[8].y*480f) }
                }
                fist=f; pinch=p
            } else { fist=false; pinch=false }
        } catch (_: Exception) { }
    }

    override fun onPause() {
        if (::sensors.isInitialized) sensors.unregisterListener(this)
        closeCamera()
        super.onPause()
    }
    override fun onResume() {
        super.onResume()
        if (::sensors.isInitialized) {
            sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)?.let {
                sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        if (::ui.isInitialized && !opening) launchCamera()
    }
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            val matrix = FloatArray(9)
            val orientation = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(matrix, event.values)
            SensorManager.getOrientation(matrix, orientation)
            yaw = orientation[0]
            pitch = orientation[1]
            if (::ui.isInitialized) ui.invalidate()
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
    override fun onDestroy() { closeCamera(); super.onDestroy() }
    private fun closeCamera() {
        try { session?.close() } catch (_: Exception) {}; session=null
        try { camera?.close() } catch (_: Exception) {}; camera=null
        try { reader?.close() } catch (_: Exception) {}; reader=null
        try { landmarker?.close() } catch (_: Exception) {}; landmarker=null
        worker?.quitSafely(); worker=null; handler=null; opening=false
    }

    inner class VRView(ctx: Context) : View(ctx) {
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        private val names=listOf("Browser","Camera","Calculator","Calendar","Notes","Clock")
        private val icons=listOf("⌕","◉","÷","31","N","◷")
        private var page="home"; private var menu=true
        private var calc="0"; private var stored=0.0; private var op=""; private var fresh=true
        private var note=prefs.getString("quick_note","") ?: ""
        private var started=0L; private var score=0; private var target=Random.nextInt(16); private var ended=false
        private var best=prefs.getInt("reaction_best",0)

        override fun onDraw(c:Canvas) {
            val eye=width/2f; if(eye<=0 || height<=0) return
            drawEye(c,0f,eye); drawEye(c,eye,eye)
            if(page=="reaction" && started>0 && !ended) {
                if(SystemClock.elapsedRealtime()-started>=30000) { ended=true; if(score>best){best=score;prefs.edit().putInt("reaction_best",best).apply()} }
                else postInvalidateDelayed(70)
            }
        }

        private fun drawEye(c:Canvas,x:Float,w:Float) {
            val s=c.save(); c.translate(x,0f); c.scale(w/480f,height/480f)
            c.translate((-yaw*12f).coerceIn(-24f,24f),(pitch*12f).coerceIn(-24f,24f))
            val b=bitmap
            if(b!=null) {
                val src=if(b.width>b.height) { val l=(b.width-b.height)/2; Rect(l,0,l+b.height,b.height) } else { val t=(b.height-b.width)/2; Rect(0,t,b.width,t+b.width) }
                c.drawBitmap(b,src,RectF(0f,0f,480f,480f),p); p.color=Color.argb(118,0,0,0); c.drawRect(0f,0f,480f,480f,p)
            } else c.drawColor(Color.BLACK)
            p.color=Color.WHITE;p.textSize=22f;p.typeface=Typeface.create("sans-serif-medium",0);c.drawText("LEMONXR",25f,34f,p)
            p.color=Color.DKGRAY;c.drawLine(25f,57f,455f,57f,p)
            if(menu) when(page) { "home"->home(c); "all"->allApps(c); else->pageDraw(c) }
            else { p.color=Color.WHITE;p.textSize=15f;c.drawText("MAKE A FIST TO OPEN MENU",105f,250f,p) }
            if(prefs.getString("control_mode","headset")!="headset") drawHands(c)
            p.color=Color.LTGRAY;p.textSize=9f;c.drawText("LEMONXR  •  BETA 0.1.2",25f,466f,p)
            c.restoreToCount(s)
        }

        private fun panel(c:Canvas,r:RectF) {
            p.color=Color.argb(218,12,12,12);p.style=Paint.Style.FILL;c.drawRoundRect(r,9f,9f,p)
            p.color=Color.DKGRAY;p.style=Paint.Style.STROKE;c.drawRoundRect(r,9f,9f,p);p.style=Paint.Style.FILL
        }
        private fun app(c:Canvas,x:Float,y:Float,w:Float,h:Float,icon:String,name:String) {
            panel(c,RectF(x,y,x+w,y+h));p.color=Color.WHITE;p.textSize=24f
            c.drawText(icon,x+(w-p.measureText(icon))/2,y+34f,p);p.textSize=8f
            c.drawText(name,x+(w-p.measureText(name))/2,y+54f,p)
        }
        private fun cube(c:Canvas,x:Float) {
            panel(c,RectF(x,73f,x+62f,139f))
            val colors=intArrayOf(0xffff5555.toInt(),0xffffa52a.toInt(),0xffffdf45.toInt(),0xff55d477.toInt(),0xff42c5df.toInt(),0xff557cff.toInt(),0xffa56bff.toInt(),0xffe77acb.toInt(),Color.WHITE)
            for(i in 0..8){val xx=x+12f+(i%3)*12f;val y=82f+(i/3)*12f;p.color=colors[i];c.drawRoundRect(RectF(xx,y,xx+9,y+9),2f,2f,p)}
            p.color=Color.WHITE;p.textSize=8f;c.drawText("ALL APPS",x+12f,132f,p)
        }
        private fun home(c:Canvas) {
            for(i in 0..3) app(c,25f+i*52f,72f,47f,66f,icons[i],names[i])
            cube(c,235f)
            val time=SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date())
            val date=SimpleDateFormat("dd MMM",Locale.getDefault()).format(Date())
            val battery=registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1) ?: -1
            p.color=Color.WHITE;p.textSize=14f;c.drawText(time,310f,99f,p)
            p.color=Color.LTGRAY;p.textSize=9f;c.drawText(date,310f,116f,p)
            c.drawText((if(battery>=0) "$battery%" else "--%")+"  BATTERY",310f,131f,p)
            p.color=Color.WHITE;p.textSize=13f;c.drawText("APPLICATIONS",25f,166f,p)
            panel(c,RectF(25f,180f,455f,365f));p.color=Color.WHITE;p.textSize=19f;c.drawText("REACTION",45f,220f,p)
            p.color=Color.LTGRAY;p.textSize=12f;c.drawText("4 × 4 TARGET TEST",45f,244f,p);c.drawText("Tap the red circle as quickly as you can.",45f,276f,p)
            c.drawText("30 seconds  •  Best: $best",45f,300f,p);p.color=Color.WHITE;p.style=Paint.Style.STROKE;c.drawRoundRect(RectF(45f,320f,180f,348f),8f,8f,p);p.style=Paint.Style.FILL
            p.textSize=11f;c.drawText("PLAY GAME  →",70f,338f,p)
            if(cameraStatus.isNotEmpty()){p.color=Color.RED;p.textSize=10f;c.drawText(cameraStatus,25f,430f,p)}
        }
        private fun allApps(c:Canvas) {
            p.color=Color.WHITE;p.textSize=17f;c.drawText("ALL APPLICATIONS",25f,91f,p)
            val n=listOf("Browser","Camera","Calculator","Calendar","Notes","Clock","Reaction","Settings");val g=listOf("⌕","◉","÷","31","N","◷","●","⚙")
            n.forEachIndexed { i,s -> app(c,25f+(i%4)*108f,115f+(i/4)*100f,96f,82f,g[i],s) };back(c)
        }
        private fun pageDraw(c:Canvas) {
            p.color=Color.WHITE;p.textSize=17f;c.drawText(page.uppercase(Locale.getDefault()),25f,91f,p)
            when(page) {
                "calculator" -> {
                    panel(c,RectF(45f,112f,435f,165f));p.textSize=26f;c.drawText(calc.takeLast(18),415f-p.measureText(calc.takeLast(18)),148f,p)
                    val keys=listOf("7","8","9","÷","4","5","6","×","1","2","3","−","C","0","=","+")
                    keys.forEachIndexed { i,k -> val x=48f+(i%4)*98f;val y=178f+(i/4)*57f;panel(c,RectF(x,y,x+88,y+48));p.textSize=19f;p.color=Color.WHITE;c.drawText(k,x+44-p.measureText(k)/2,y+30,p) }
                }
                "calendar" -> {p.textSize=28f;c.drawText(SimpleDateFormat("EEEE",Locale.getDefault()).format(Date()),55f,190f,p);p.textSize=44f;c.drawText(SimpleDateFormat("d",Locale.getDefault()).format(Date()),55f,250f,p);p.textSize=16f;p.color=Color.LTGRAY;c.drawText(SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(Date()),55f,282f,p);c.drawText("Tap below to open device calendar.",55f,330f,p)}
                "clock" -> {p.textSize=58f;c.drawText(SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date()),65f,235f,p);p.textSize=15f;p.color=Color.LTGRAY;c.drawText(SimpleDateFormat("EEEE, dd MMMM yyyy",Locale.getDefault()).format(Date()),80f,275f,p);postInvalidateDelayed(500)}
                "notes" -> {p.color=Color.LTGRAY;p.textSize=12f;c.drawText("QUICK NOTE — TAP TO EDIT",45f,145f,p);p.color=Color.WHITE;p.textSize=14f;note.split("\n").take(8).forEachIndexed{i,t->c.drawText(t.take(48),45f,180f+i*24,p)}}
                "browser" -> {panel(c,RectF(45f,145f,435f,210f));p.color=Color.WHITE;p.textSize=15f;c.drawText("Google Search",65f,172f,p);p.color=Color.LTGRAY;p.textSize=11f;c.drawText("Tap to enter a search query.",65f,192f,p);c.drawText("Results open in your installed browser.",45f,245f,p)}
                "reaction" -> reactionDraw(c)
            };back(c)
        }
        private fun reactionDraw(c:Canvas) {
            val remain=if(started==0L)30 else (30-((SystemClock.elapsedRealtime()-started)/1000).toInt()).coerceAtLeast(0)
            p.color=Color.LTGRAY;p.textSize=12f;c.drawText("SCORE $score     TIME $remain s     BEST $best",25f,119f,p)
            for(i in 0..15){val x=116f+(i%4)*59f;val y=139f+(i/4)*59f;panel(c,RectF(x,y,x+51,y+51));if(started>0&&!ended&&i==target){p.color=Color.RED;c.drawCircle(x+25.5f,y+25.5f,15f,p)}}
            panel(c,RectF(160f,390f,320f,432f));p.color=Color.WHITE;p.textSize=13f
            val label=if(started==0L)"START" else if(ended)"PLAY AGAIN" else "RUNNING...";c.drawText(label,240f-p.measureText(label)/2,417f,p)
            if(ended){p.color=Color.LTGRAY;p.textSize=12f;c.drawText("TIME UP • SCORE $score",165f,455f,p)}
        }
        private fun drawHands(c:Canvas) {
            landmarks.forEach { h ->
                if(h.size<21)return@forEach
                val path=Path();val order=intArrayOf(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,0)
                order.forEachIndexed{i,n->val x=h[n].x*480;val y=h[n].y*480;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}
                p.color=Color.WHITE;p.style=Paint.Style.STROKE;p.strokeWidth=1.7f;c.drawPath(path,p);p.style=Paint.Style.FILL
                val tip=h[8];val x=tip.x*480;val y=tip.y*480;p.strokeWidth=1.2f;c.drawLine(x,y,x.coerceIn(52f,424f),105f,p);c.drawCircle(x.coerceIn(52f,424f),105f,4f,p)
            }
        }
        private fun back(c:Canvas){p.color=Color.LTGRAY;p.textSize=10f;c.drawText("‹ BACK",25f,445f,p)}
        fun toggleMenu(){menu=!menu;invalidate()}
        fun virtualTap(x:Float,y:Float){tap(x,y)}
        override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_UP){val eye=width/2f;if(eye>0)tap((e.x%eye)/(eye/480f),e.y/(height/480f))};return true}
        private fun tap(x:Float,y:Float){
            if(!menu)return
            if(page !in listOf("home","all")&&y>420&&x<100){page="home";invalidate();return}
            if(page=="home"){
                if(y in 72f..140f){if(x in 235f..297f)page="all" else {val i=((x-25)/52).toInt();if(i in 0..3)open(names[i])}}
                else if(y in 180f..365f){page="reaction";startGame()}
            }else if(page=="all"){
                if(y in 115f..300f){val col=((x-25)/108).toInt();val row=((y-115)/100).toInt();val n=listOf("Browser","Camera","Calculator","Calendar","Notes","Clock","Reaction","Settings");val i=row*4+col;if(col in 0..3&&row in 0..1&&i in n.indices)open(n[i])}
                if(y>420&&x<100)page="home"
            }else when(page){
                "calculator"->calcTap(x,y)
                "notes"->if(y in 130f..420f)editNote()
                "browser"->if(y in 130f..300f)search()
                "reaction"->gameTap(x,y)
                "calendar"->if(y>280)systemApp(Intent.CATEGORY_APP_CALENDAR)
                "clock"->if(y>280)systemApp(Intent.CATEGORY_APP_CLOCK)
            };invalidate()
        }
        private fun open(n:String){page=when(n){"Browser"->"browser";"Camera"->{Toast.makeText(context,"Passthrough camera is active.",Toast.LENGTH_SHORT).show();"home"};"Calculator"->"calculator";"Calendar"->"calendar";"Notes"->"notes";"Clock"->"clock";"Reaction"->"reaction";"Settings"->{startActivity(Intent(this@VRActivity,SettingsActivity::class.java));"home"};else->"home"};if(n=="Reaction")startGame();invalidate()}
        private fun calcTap(x:Float,y:Float){
            if(y !in 178f..406f)return
            val col=((x-48)/98).toInt();val row=((y-178)/57).toInt();if(col !in 0..3||row !in 0..3)return
            val k=listOf("7","8","9","÷","4","5","6","×","1","2","3","−","C","0","=","+")[row*4+col]
            when(k){"C"->{calc="0";op="";stored=0.0;fresh=true};"+","−","×","÷"->{stored=calc.toDoubleOrNull()?:0.0;op=k;fresh=true};"="->{val v=calc.toDoubleOrNull()?:0.0;val r=when(op){"+"->stored+v;"−"->stored-v;"×"->stored*v;"÷"->if(v!=0.0)stored/v else Double.NaN;else->v};calc=if(r.isFinite())r.toString().removeSuffix(".0")else"Error";fresh=true};else->{calc=if(fresh||calc=="0"||calc=="Error")k else(calc+k).take(14);fresh=false}}
        }
        private fun startGame(){score=0;target=Random.nextInt(16);started=SystemClock.elapsedRealtime();ended=false;invalidate()}
        private fun gameTap(x:Float,y:Float){
            if(started==0L||ended){if(y in 380f..440f)startGame();return}
            val col=((x-116)/59).toInt();val row=((y-139)/59).toInt();if(col !in 0..3||row !in 0..3)return
            val cx=116f+col*59f;val cy=139f+row*59f
            if(x<=cx+51f&&y<=cy+51f&&row*4+col==target){score++;target=Random.nextInt(16)}
            if(SystemClock.elapsedRealtime()-started>=30000)ended=true
        }
        private fun editNote(){val e=EditText(this@VRActivity).apply{setText(note);setTextColor(Color.BLACK);hint="Write a note"};AlertDialog.Builder(this@VRActivity).setTitle("LemonXR Notes").setView(e).setPositiveButton("SAVE"){_,_->note=e.text.toString();prefs.edit().putString("quick_note",note).apply();invalidate()}.setNegativeButton("CANCEL",null).show()}
        private fun search(){val e=EditText(this@VRActivity).apply{hint="Search Google";setTextColor(Color.BLACK)};AlertDialog.Builder(this@VRActivity).setTitle("Google Search").setView(e).setPositiveButton("SEARCH"){_,_->val q=e.text.toString().trim();if(q.isNotEmpty())try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(q))))}catch(_:Exception){Toast.makeText(context,"No browser installed.",Toast.LENGTH_SHORT).show()}}.setNegativeButton("CANCEL",null).show()}
        private fun systemApp(cat:String){try{startActivity(Intent(Intent.ACTION_MAIN).addCategory(cat))}catch(_:Exception){Toast.makeText(context,"No matching app installed.",Toast.LENGTH_SHORT).show()}}
    }
}
