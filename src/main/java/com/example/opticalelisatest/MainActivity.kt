package com.example.opticalelisatest

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.dhaval2404.imagepicker.ImagePicker
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.Core.addWeighted
import org.opencv.imgproc.Imgproc.*
import java.io.File
import java.util.*
//import android.R
import android.view.View

import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import android.widget.*
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
//import java.awt.Button
//import javax.swing.text.html.ImageView
import kotlin.math.pow
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import java.lang.System.currentTimeMillis

class MainActivity : AppCompatActivity()
{
    private val writePermission = false
    private var image_State = "Image_State"
    var imagePicker: ImageView? = null
    var analysis: Button? = null
    var detect = false
    var graph = false
    var multiplex = false
    var dfList = Array(6, {0})
    var concEditText = ""
    var dilutionFactors = listOf("")
    
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        OpenCVLoader.initDebug();
        Log.i("opencv" , "opencv init debug works");

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //val storage: Button? = findViewById(R.id.storage)

        val fileButton = findViewById<View>(R.id.fileBtn) as ImageButton
        fileButton.setOnClickListener { v: View? ->
            val fIntent = Intent(applicationContext, FileSelect::class.java)
            startActivity(fIntent)
        }
        val multiplexToggle: Switch = findViewById(R.id.multiplex_switch)
        Log.i("debug","Initialized switch")
        multiplexToggle.setOnClickListener()
        {
            multiplex = true
        }
        
        var analyzer = findViewById(R.id.analyzerbutton) as Button

        if (savedInstanceState != null)
        {
            image_State = savedInstanceState.getString(image_State, "select")
        }

imagePicker = findViewById(R.id.picker_image)
        val imagePick: Button = findViewById(R.id.imagepick) 
        analyzer = findViewById(R.id.analyzerbutton)
        imagePick.setOnClickListener() 
        {

            ImagePicker.with(this).start()
            Log.i("debug","image picker starts")
        }
        analyzer.setOnClickListener() 
        {
            Log.i("debug","analyzer starts")
            detect = true
            val concEditText = findViewById<EditText>(R.id.concEditText)
            dilutionFactors = concEditText.text.split(" ");
            dfList = Array(dilutionFactors.size) { 0 }
            for ((count, fac) in dilutionFactors.withIndex())
            {
                //val f = fac.toInt()
                dfList[count] = fac.toInt()
            }
            Log.i("debug","dilution factors captured")
        }
    }
    private fun checkPermission(permission: String, requestCode: Int)
    {
        if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
        } else {
            Toast.makeText(this@MainActivity, "Permission already granted", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@MainActivity, "Camera Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@MainActivity, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK && requestCode == ImagePicker.REQUEST_CODE) {

            val uri: Uri = data?.data!!

            imagePicker?.setImageURI(data?.data)

            Log.i("URI",uri.toString());

            val bmpFactoryOptions: BitmapFactory.Options = BitmapFactory.Options();

            bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

            val bmp = MediaStore.Images.Media.getBitmap(this.contentResolver, uri);

            val obj = Mat(bmp.width, bmp.height, CvType.CV_8UC4)

            Utils.bitmapToMat(bmp, obj) //converts the output of image picker to Mat
            Log.i("URI" , "MAT OBJECT CREATED SUCCESSFULLY");

            if (detect && !multiplex)
            {
                graph = true
            }
            val aaChartView = findViewById<AAChartView>(R.id.aa_chart_view)
            if (graph)
            {
                //change to val (results, enoughCircles)
                val (results,enoughCircles) = BiomarkerDetector(obj, 60, 2, multiplex)
                if (!enoughCircles){
                    Toast.makeText(this,"not all wells detectable, try another image",Toast.LENGTH_SHORT).show()
                }
                val r0 = Array<Any>(results[0].size) {0.0}

                var count = 0
                for (i in r0)
                {
                    r0[count] = results[0][count]
                    count++
                }
                val r1 = Array<Any>(results[1].size) {0.0}
                count = 0
                for (i in r1)
                {
                    r1[count] = results[1][count]
                    count++
                }
                val r2 = Array<Any>(results[2].size) {0.0}
                count = 0
                for (i in r2)
                {
                    r2[count] = results[2][count]
                    count++
                }
                val r3 = Array<Any>(results[3].size) {0.0}

                count = 0
                for (i in r3)
                {
                    r3[count] = results[3][count]
                    count++
                }
                val R0 = r0.toList()
                val R1 = r1.toList()
                val R2 = r2.toList()
                val R3 = r3.toList()
                val df = dfList.toList()
                val resultsList = listOf(R0,R1,R2,R3,df)
                val dfA = Array(dilutionFactors.size,{""})
                Log.i("debug","size of dilutionfactors: "+dilutionFactors.size)

                count = 0
                for (i in dilutionFactors)
                {
                    dfA[count] = i
                    count++
                }


                val aaChartModel : AAChartModel = AAChartModel()
                    .chartType(AAChartType.Area)
                    //.xAxisLabelsEnabled(true)

                    .yAxisTitle("Grey Scale Values")
                    .categories(dfA)
                    .title("Gray Scale Value versus Dilution")
                    //.backgroundColor("#4b2b7f")
                    .dataLabelsEnabled(true)
                    .series(arrayOf(
                        AASeriesElement()
                            .name("IgG, N")
                            //.dataLabels()
                            .data(r2),

                        AASeriesElement()
                            .name("IgG, S")
                            .data(r3),

                        AASeriesElement()
                            .name("healthy, N")
                            .data(r2),
                        AASeriesElement()
                            .name("healthy, S")
                            .data(r3),


                        )
                    )
                //The chart view object calls the instance object of AAChartModel and draws the final graphic
                Log.i("debug", "Added datapoints to chart")

                aaChartView.aa_drawChartWithChartModel(aaChartModel)

                //new file code here **********

                val docPath = File(applicationContext.filesDir, "text")
                if (!docPath.exists()) {
                    docPath.mkdir()
                }
                val currentDate = SimpleDateFormat("MM_dd_yy_", Locale.getDefault()).format(
                    Date()
                )
                val currentTime = SimpleDateFormat("HH_mm_ss", Locale.getDefault()).format(Date())
                val title = "$currentDate$currentTime.txt"
                val writing: File = File(docPath, title)
                var writer: FileWriter? = null

                try {
                    writer = FileWriter(writing)
                    CsvWriter().writeAll(resultsList, writing, append = true)
                    writer.flush()
                    writer.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}


fun BiomarkerDetector(
    image: Mat,
    SIZE: Int = 64,
    replicates: Int = 2,
    Multiplex: Boolean = false
): Pair<Array<DoubleArray>,Boolean> 
{

    val radius = 5
    val contrast = 2.5
    val brightness = -190.0
    Log.i("debug","Function starts successfully")

    val outputs = Array(SIZE) { DoubleArray(3) {0.0} }
    
    val im = preProcessing(image,contrast,brightness,radius)

    val dims = im.size()
    val rows = dims.height
    val cols = dims.width
    Log.i("debug","Image is $rows x $cols")

    val (circles, enoughCircles) = enoughCircles(im,SIZE,radius)

   
    Log.i("debug","Hough doesn't crash")
    val cdims = circles.size()

    Log.i("debug","circles: ${circles[0,1]}")
    //if circles doesn't contain size * 3 or w/e elements, and multiplex is not enabled, Toast not all wells detected
    Log.i("debug", "cdims: $cdims , should be ${SIZE}")

    var count = 0
    val cCols = (cdims.width).toInt()
    //Log.i("debug","cdims doesn't crash")

        for( j in 0 until cCols)
        {

            val area = 1..radius
            val circle = circles[0,j]
            //Log.i("debug","a init")

            val x = circle[0].toInt()
            val y = circle[1].toInt()
            //Log.i("debug","x,y init")

            if(count<SIZE)
            {
                //Log.i("debug", "before reading im")
                var mean = try {//shouldn't actually be necessary
                    val pixel = im[y, x][0] // y comes first
                    Log.i("debug","pixel: $pixel, x: $x, y: $y, r: ${circle[2].toInt()}")
                    pixel
                } catch (e: java.lang.NullPointerException) {
                    Log.i("debug", "out of bounds for im: $im")
                    -1.0
                }


              // Log.i("debug","area = $area")
                var areaSize = radius
                for (k in area)
                {
                    //Log.i("debug","k in area loop starts") //runs once
                    try {//averaging together pixels in an area
                        Log.i("debug","a pixel value: ${im[y,x][0]}")
                        mean += im[y + k, x][0] + im[y, x + k][0] + im[y - k, x][0] + im[y, x - k][0] + im[y - k, x - k][0] + im[y + k, x + k][0]
                    }
                    catch(e: NullPointerException)
                    { // if it detects a circle very close to the edge for some reason, edge of the image may be less than the radius in pixels away
                        Log.i("debug", "index out of bounds.at x,y,k = $x, $y, $k, in image of size: ")
                        if (areaSize > 1)
                        {
                            areaSize -= 1
                        }
                    }
                }
               // Log.i("debug", "k in area works")
                mean /= ((6 * areaSize) + 1) //Taking the average

               // Log.i("debug", "pixel inits fine")
                outputs[count][0] = circle[0] // x value
                outputs[count][1] = mean
                outputs[count][2] = circle[1] //y value
              // Log.i("debug", "outputs assign works fine")
            }
            else
            {
                break
            }
            count++
        }
    Log.i("debug","circles successful")

    if (Multiplex){

        outputs.sortWith(compareBy { it.last() })
        outputs.sortWith(compareBy { it.first() })
        // Doesn't graph in this configuration right now
        // Could average together 1st and 5th element, 2nd and 6th, etc.
        // Then bar graph
        return Pair(outputs,false)
     }

    outputs.sortWith(compareBy { it.last() })
    //Log.i("debug", SIZE.toString())

    val bottomHalf = outputs.copyOfRange(SIZE/2,SIZE)
    val topHalf = outputs.copyOfRange(0,SIZE/2)


    outputs.sortWith(compareBy {it.first() })
    val control1 = outputs.copyOfRange(0,2)
    val control2 = outputs.copyOfRange(3,5)
    for (row in topHalf)
    {
        Log.i("debug tophalf", (row.contentToString()))
    }
    for (row in bottomHalf)
    {
        Log.i("debug bottomhalf", (row.contentToString()))
    }

    Log.i("debug","Array splitting successful")
    topHalf.sortWith(compareBy({it.first()})) //
    bottomHalf.sortWith(compareBy({it.first()}))//sorting by the x positions

    val topLeft = topHalf.copyOfRange(0,SIZE/4) //check
    val topRight = topHalf.copyOfRange(SIZE/4,topHalf.size)
    val bottomLeft = bottomHalf.copyOfRange(0,SIZE/4)
    val bottomRight = bottomHalf.copyOfRange(SIZE/4,bottomHalf.size)
    
    for (row in topLeft)
    {
        Log.i("debug", "topLeft: " + row.contentToString())
    }

    val mktl = mkArray(topLeft)
    val mktr = mkArray(topRight)
    val mkbl = mkArray(bottomLeft)
    val mkbr = mkArray(bottomRight)
    Log.i("debug","mktl: $mktl")
    Log.i("debug","mktr: $mktr")
    
    val quadrants  = arrayOf(mktl, mktr,mkbl, mkbr)
    Log.i("debug","quadrants[0]: ${quadrants[0]}")

    val results =  Array(12){DoubleArray(SIZE/8)}
    val stds = Array(4 ){DoubleArray(SIZE/8)} //check size
    for ((k, quad) in quadrants.withIndex())
    {
        count = 0
        for(i in 0..SIZE/4 step replicates)
        {
            Log.i("debug","i size: $i")
            Log.i("debug","quad size: ${quad.size}")
            if (count < SIZE/8) //check
            {
                val nums = quad[i..i+replicates]
                
                //Log.i("debug", "nums: $nums")
                val sum = sum(nums as D1Array<Double>)
                //Log.i("debug","initial sum: $sum")

                val mean = (sum/replicates)
                Log.i("debug", "mean: $mean")
                
                val std = stdev(nums, replicates, mean)
                Log.i("debug", "std: $std")

                results[k][count] = mean
                //Log.i("debug","q2 size okay")
                stds[k][count] = std
                //Log.i("debug","std size okay")
            }
            count++
        }
        Log.i("debug","inner quad for loop works")
    }
    results[4] = stds[0]
    results[5] = stds[1]
    results[6] = stds[2]
    results[7] = stds[3]
    val mean1 = sum(mkArray(control1))/2
    val mean2 = sum(mkArray(control2))/2
    results[8] = DoubleArray(results[0].size){mean1}
    results[9] = DoubleArray(results[0].size){mean2}
    results[10] = DoubleArray(results[0].size){stdev(mkArray(control1),replicates,mean1)}
    results[11] = DoubleArray(results[0].size){stdev(mkArray(control2),replicates,mean2)}
    Log.i("debug","final results: ")
    for (row in results) {Log.i("debug",row.contentToString())}

    Log.i("debug","main function finishes")
    return Pair(results,true)
}

fun mkArray (arr: Array<DoubleArray>): D1Array<Double> { //converts regular array to numpy like array
    val mkarr = mk.zeros<Double>(arr.size)
    for (i in arr.indices)
    {
        mkarr[i] = arr[i][1] //just the greyscale value is copied to the mk array
    }
    return mkarr
}
fun sum (arr: D1Array<Double>): Double
{
    var sum = 0.0;
    for (a in arr)
    {
        sum += a;
    }
    return sum;
}

fun stdev(nums: D1Array<Double>, replicates: Int, mean: Double): Double {
    var numerator = 0.0
    for (l in 0 until nums.size) {
        numerator += (nums[l] - mean).pow(2)
    }
    return ((1 / replicates) * numerator).pow(.5)
}

fun preProcessing(image:Mat, contrast: Double, brightness: Double, radius: Int): Mat {
    val dims = image.size()
    val rows = dims.height
    val cols = dims.width
    Log.i("debug","Image is $rows x $cols")

    val greyImage = Mat()
    cvtColor(image, greyImage, COLOR_BGR2GRAY) // converts image to greyscale
    val blurredImage = Mat()
    val im = Mat()
    GaussianBlur(
        greyImage,
        blurredImage,
        Size(radius.toDouble(), radius.toDouble()),
        2.5
    )
    addWeighted( blurredImage, contrast, blurredImage, 0.0,brightness,im)

    Log.i("debug","preprocessing successful")
    return im
}

fun enoughCircles(im:Mat, size: Int, radius: Int): Pair<Mat,Boolean> { //return a pair bool and Mat
    val startTime = currentTimeMillis()
    val circles = Mat()
    val dims = im.size()
    val rows = dims.height
    var param1 = 100.0
    var param2 = 15.0 // Least circle-like thing that gets counted as a circle
    var numCircles = 0

    while((currentTimeMillis() - startTime) < 6000) //Try to tweak settings for 6 seconds to find all the wells
    {
        HoughCircles(im,circles, HOUGH_GRADIENT, 2.0, rows/6,
            param1, param2,
            radius, 30) //
        numCircles = circles.size().width.toInt()
        Log.i("debug","numCircles: $numCircles")
        if (numCircles == size) {
            break
        }
        if(numCircles > size) {
            param2 += 0.2
            param1 -= 0.2
        }
        if(numCircles < size)
        {
            param2 -= 0.2
        }
    }
    Log.i("debug","While loop in enoughCircles exits")
    
    val enoughCircles = numCircles == size
    return Pair(circles, enoughCircles);
}