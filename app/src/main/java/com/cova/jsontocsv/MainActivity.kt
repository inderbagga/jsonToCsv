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
import com.cova.jsontocsv.api.Input
import com.cova.jsontocsv.utils.ConnectivityUtil.isConnected
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private  val TAG = "MainActivity"

    private lateinit var progressBar: ProgressBar
    private lateinit var textView: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView

    val dateFormat = "yyyy-MM-dd"

    private lateinit var calendar: Calendar
    private lateinit var sCalendar: Calendar

    private var year = 0
    private var month = 0
    private var day = 0

    var startDate=""
    var endDate=""

    private val channelId="2021"
    private val notificationId=1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeUI()
        createNotificationChannel()
    }

    private fun initializeUI() {
        progressBar=findViewById(R.id.progressBar)
        textView=findViewById(R.id.textView)
        tvStartDate=findViewById(R.id.startDate)
        tvEndDate=findViewById(R.id.endDate)
        calendar = Calendar.getInstance()
        sCalendar=calendar
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
                                    val jsonArray=it.getJSONArray("data")

                                    textView.text = "${jsonArray.length()} records have been found."

                                    this@MainActivity.saveCsv(jsonArray)
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

        val thread:Thread=Thread {

            val jsonTree = ObjectMapper().readTree(jsonArray.toString())

            val csvSchemaBuilder = CsvSchema.builder()
            val firstObject = jsonTree.elements().next()
            firstObject.fieldNames().forEach { fieldName: String? -> csvSchemaBuilder.addColumn(fieldName) }
            val csvSchema = csvSchemaBuilder.build().withHeader()

            val date=Date().toGMTString().replace(":", "_").substring(0, 10)
            val fileNAME="Patient_List_${date.replace(" ", "_")}.csv"

            val csvMapper = CsvMapper()
            csvMapper.writerFor(JsonNode::class.java)
                    .with(csvSchema)
                    .writeValue(File(filesDir,fileNAME), jsonTree)

            exportingCsv( fileNAME)
            sendingNotification(fileNAME)
        }
        thread.start()
    }

    private fun exportingCsv( fileName: String) {

        try {

            val context: Context = applicationContext
            val filePath = File(filesDir, fileName)
            val path: Uri = FileProvider.getUriForFile(
                    context,
                    "com.cova.jsontocsv.fileprovider",
                    filePath
            )
            val fileIntent = Intent(Intent.ACTION_SEND)
            fileIntent.type = "text/csv"
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Patient List")
            fileIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            fileIntent.putExtra(Intent.EXTRA_STREAM, path)
            startActivity(Intent.createChooser(fileIntent, "Sharing $fileName"))
        } catch (e: Exception) {
            Log.e(TAG, "exportingCsv: $e")
        }
    }

    private fun sendingNotification(fileName: String) {
        try {

            var builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Ready to Export!")
                .setContentText("$fileName has been generated..")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(this)) {
                notify(notificationId, builder.build())
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendingNotification: $e")
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

            val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
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

            val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
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
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}