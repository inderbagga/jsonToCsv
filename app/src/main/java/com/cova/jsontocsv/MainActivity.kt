package com.cova.jsontocsv

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.cova.jsontocsv.api.Api
import com.cova.jsontocsv.data.model.DataWrapper
import com.cova.jsontocsv.data.model.Input
import com.cova.jsontocsv.utils.ConnectivityUtil.isConnected
import com.cova.jsontocsv.utils.iterate
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var textView: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView

    private  val TAG = "MainActivity"
    val myFormat = "yyyy-MM-dd"

    var startDate=""
    var endDate=""

    val CHANNEL_ID="1999"
    val notificationId=1

    private lateinit var calendar: Calendar
    private lateinit var sCalendar: Calendar

    private var year = 0
    private var month = 0
    private var day = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar=findViewById(R.id.progressBar)
        textView=findViewById(R.id.textView)
        tvStartDate=findViewById(R.id.startDate)
        tvEndDate=findViewById(R.id.endDate)
        calendar = Calendar.getInstance()
        sCalendar=calendar

        createNotificationChannel()
    }

    private fun fetchRtPcrData(input: Input) {
        progressBar.visibility= View.VISIBLE

        AndroidNetworking.post(Api.ENDPOINT + Api.URI_RTPCR_DATA)
            .addByteBody(Gson().toJson(input).toByteArray())
            .addHeaders(
                "Authorization",
                Api.TOKEN
            )
            .setContentType("application/json")
            .setPriority(Priority.HIGH)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    try {
                        progressBar.visibility = View.GONE

                        response?.let {

                            when (it.getString("response")) {

                                "1" -> {

                                    val dataWrapper = Gson().fromJson(
                                        it.toString(),
                                        DataWrapper::class.java
                                    )

                                    textView.text = "${dataWrapper.data.size} records have been found."

                                    saveCsv(it.getJSONArray("data"))
                                }
                                else -> {
                                    textView.text =
                                        response?.getString("sys_message").toString()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        progressBar.visibility = View.GONE
                        textView.text = e.toString()
                    }
                }

                override fun onError(anError: ANError?) {
                    try {
                        progressBar.visibility = View.GONE
                        textView.text = anError?.message.toString()
                    } catch (e: Exception) {
                        textView.text = e.toString()
                    }
                }
            })
    }

    private fun saveCsv(jsonArray: JSONArray) {

        var CSV_HEADER=""

        val header=jsonArray.getJSONObject(0)
        val headerKeys= mutableListOf<String>()

        for(column in iterate(header.keys())!!){
            headerKeys.add(column)
            CSV_HEADER= "$CSV_HEADER$column,"
        }
        CSV_HEADER=CSV_HEADER.substring(0,CSV_HEADER.length-1)

        Log.e(TAG,"$CSV_HEADER")

        val buffer= StringBuffer(CSV_HEADER)
        val thread:Thread=Thread {
            try {

                buffer?.apply {
                    append('\n')
                    Log.e(TAG,"initial newline")
                    (0 until jsonArray.length()).forEach { i ->
                        var jsonObject = jsonArray.getJSONObject(i)

                        headerKeys.forEachIndexed { index, _ ->

                            val item= jsonObject.getString(headerKeys[index])

                            if(item.isEmpty())
                            {
                                append("NA")
                            }
                            else {
                                append(item)
                            }

                            if(headerKeys.size-1!=index)
                                append(',')
                            else{
                                Log.e(TAG,"for array${i} at column${index}")
                            }
                        }
                        append('\n')
                        Log.e(TAG,"for array${i} newline")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val fileName=Date().toGMTString().replace(":", "_").substring(0, 10)
                val file="Patient_List_${fileName.replace(" ", "_")}.csv"

                exportingCsv(buffer.toString(),file)

                sendingNotification(file)
            }
        }
        thread.start()
    }

    private fun exportingCsv(payload:String,file: String) {

        try {
            //saving the file into device
            val out: FileOutputStream = openFileOutput(file, Context.MODE_PRIVATE)
            out.write(payload.toByteArray())
            out.close()

            //exporting
            val context: Context = applicationContext
            val filelocation = File(filesDir, file)
            val path: Uri = FileProvider.getUriForFile(
                context,
                "com.cova.jsontocsv.fileprovider",
                filelocation
            )
            val fileIntent = Intent(Intent.ACTION_SEND)
            fileIntent.type = "text/csv"
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Patient List")
            fileIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            fileIntent.putExtra(Intent.EXTRA_STREAM, path)
            startActivity(Intent.createChooser(fileIntent, "Sharing ${file}"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendingNotification(fileName:String) {
        try {

            var builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Ready to Export!")
                .setContentText("$fileName has been generated..")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(this)) {
                // notificationId is a unique int for each notification that you must define
                notify(notificationId, builder.build())
            }
        } catch (e: java.lang.Exception) {
            Log.e("Constants Error", "writeFileToSdCard append: $e")
        }
    }

    fun selectStartDate(view: View) {

        year = calendar.get(Calendar.YEAR)
        month = calendar.get(Calendar.MONTH)
        day = calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(this, { _, year, month, day_of_month ->
            calendar[Calendar.YEAR] = year
            calendar[Calendar.MONTH] = month
            calendar[Calendar.DAY_OF_MONTH] = day_of_month

            val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
            startDate = sdf.format(calendar.time)
            tvStartDate.text = startDate
            sCalendar = calendar
        }, calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DAY_OF_MONTH])
        calendar.add(Calendar.YEAR, 0)
        dialog.datePicker.maxDate = calendar.timeInMillis
        dialog.show()
    }

    fun selectEndDate(view: View) {

        if (startDate.isEmpty())
        {
            Toast.makeText(this, "Please select start date.", Toast.LENGTH_LONG).show()
            return
        }

        year = sCalendar.get(Calendar.YEAR)
        month = sCalendar.get(Calendar.MONTH)
        day = sCalendar.get(Calendar.DAY_OF_MONTH)

        val edialog = DatePickerDialog(this, { _, year, month, day_of_month ->
            sCalendar[Calendar.YEAR] = year
            sCalendar[Calendar.MONTH] = month
            sCalendar[Calendar.DAY_OF_MONTH] = day_of_month

            val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
            endDate = sdf.format(sCalendar.time)
            tvEndDate.text = endDate
        }, sCalendar[Calendar.YEAR], sCalendar[Calendar.MONTH], sCalendar[Calendar.DAY_OF_MONTH])
        sCalendar.add(Calendar.YEAR, 0)
        edialog.datePicker.minDate=sCalendar.timeInMillis
        edialog.datePicker.maxDate = Calendar.getInstance().timeInMillis
        edialog.show()
    }

    fun onSubmit(view: View) {

        if(startDate.isEmpty()||endDate.isEmpty()){
            Toast.makeText(this, "Please select start/end dates.", Toast.LENGTH_LONG).show()
        }else{

            Log.e(TAG, "$startDate and $endDate")

            val input=Input("C", startDate, endDate, "63719")

            if(isConnected(this))
                fetchRtPcrData(input)
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Channel Name"
            val descriptionText = "Channel Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}