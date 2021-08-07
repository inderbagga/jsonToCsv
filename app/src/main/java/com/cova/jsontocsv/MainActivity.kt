package com.cova.jsontocsv

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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

import com.cova.jsontocsv.Utils.isConnected

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var progressBar: ProgressBar
    private lateinit var textView: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView

    private lateinit var calendar1: Calendar
    private lateinit var calendar2: Calendar

    private var year = 0
    private var month = 0
    private var day = 0

    var startDate=""
    var endDate=""

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
        calendar1 = Calendar.getInstance()
        calendar2=calendar1
    }

    private fun fetchRtPcrData(input: Input) {
        progressBar.visibility= View.VISIBLE

        AndroidNetworking.post(ENDPOINT + URI_RTPCR_DATA)
            .addByteBody(Gson().toJson(input).toByteArray())
            .addHeaders(
                "Authorization",
                TOKEN
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
                                    val jsonArray = it.getJSONArray("data")

                                    textView.text =
                                        "${jsonArray.length()} records have been found."

                                    this@MainActivity.generateCsv(jsonArray)
                                }
                                else -> {
                                    textView.text =
                                        response?.getString("sys_message").toString()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        progressBar.visibility = View.GONE
                        textView.text ="fetchRtPcrData:onResponse-> \n $e.toString()"
                    }
                }

                override fun onError(anError: ANError?) {
                    try {
                        progressBar.visibility = View.GONE
                        this@MainActivity.textView.text = anError?.message.toString()
                    } catch (e: Exception) {
                        textView.text ="fetchRtPcrData:onError->\n $e.toString()"
                    }
                }
            })
    }

    private fun generateCsv(jsonArray: JSONArray) {

        val thread:Thread=Thread {

            val jsonTree = ObjectMapper().readTree(jsonArray.toString())

            val csvSchemaBuilder = CsvSchema.builder()
            val firstObject = jsonTree.elements().next()
            firstObject.fieldNames().forEach { fieldName: String? -> csvSchemaBuilder.addColumn(
                fieldName
            ) }
            val csvSchema = csvSchemaBuilder.build().withHeader()

            val date=Date().toGMTString().replace(":", "_").substring(0, 10)
            val fileName="Patient_List_${date.replace(" ", "_")}.csv"

            val csvMapper = CsvMapper()
            csvMapper.writerFor(JsonNode::class.java)
                .with(csvSchema)
                .writeValue(File(filesDir, fileName), jsonTree)

            sendingNotification(fileName)
        }
        thread.start()
    }

    private fun sendingNotification(fileName: String) {
        try {

            val context: Context = applicationContext
            val authority = BuildConfig.APPLICATION_ID + ".provider"

            val shareFilePath = File(filesDir, fileName)

            val out: FileOutputStream = openFileOutput("exported$fileName", Context.MODE_PRIVATE)
            out.write(shareFilePath.readBytes())
            out.close()

            val viewFilePath = File(filesDir, "exported$fileName")

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type= "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Patient List")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                    context,
                    authority,
                    shareFilePath
                ))
            }

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setDataAndType( FileProvider.getUriForFile(
                    context,
                    authority,
                    viewFilePath
                ), "text/csv")
            }

            val pendingShareIntent = PendingIntent.getActivity(
                context, 0, Intent.createChooser(
                    shareIntent,
                    "Sharing $fileName"
                ),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val pendingViewIntent = PendingIntent.getActivity(
                context, 0, viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            var builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(fileName)
                .setContentText(getString(R.string.notify_content_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .addAction(
                    android.R.drawable.ic_menu_view,
                    getString(R.string.notify_action_view),
                    pendingViewIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_share,
                    getString(R.string.notify_action_share),
                    pendingShareIntent
                )

            with(NotificationManagerCompat.from(this)) {
                notify(notificationId, builder.build())
            }
        } catch (e: Exception) {
            textView.text="sendingNotification: ${e.toString()}"
        }
    }

    fun selectStartDate(view: View) {

        year = calendar1.get(Calendar.YEAR)
        month = calendar1.get(Calendar.MONTH)
        day = calendar1.get(Calendar.DAY_OF_MONTH)

        val dialog1 = DatePickerDialog(this, { _, year, month, day_of_month ->
            calendar1[Calendar.YEAR] = year
            calendar1[Calendar.MONTH] = month
            calendar1[Calendar.DAY_OF_MONTH] = day_of_month

            val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
            startDate = sdf.format(calendar1.time)
            tvStartDate.text = startDate
            calendar2 = calendar1
        }, calendar1[Calendar.YEAR], calendar1[Calendar.MONTH], calendar1[Calendar.DAY_OF_MONTH])
        calendar1.add(Calendar.YEAR, 0)
        dialog1.datePicker.maxDate = calendar1.timeInMillis
        dialog1.show()
    }

    fun selectEndDate(view: View) {

        if (startDate.isEmpty())
        {
            Toast.makeText(this, getString(R.string.required_start_date), Toast.LENGTH_LONG).show()
            return
        }

        year = calendar2.get(Calendar.YEAR)
        month = calendar2.get(Calendar.MONTH)
        day = calendar2.get(Calendar.DAY_OF_MONTH)

        val dialog2 = DatePickerDialog(this, { _, year, month, day_of_month ->
            calendar2[Calendar.YEAR] = year
            calendar2[Calendar.MONTH] = month
            calendar2[Calendar.DAY_OF_MONTH] = day_of_month

            val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
            endDate = sdf.format(calendar2.time)
            tvEndDate.text = endDate
        }, calendar2[Calendar.YEAR], calendar2[Calendar.MONTH], calendar2[Calendar.DAY_OF_MONTH])
        calendar2.add(Calendar.YEAR, 0)
        dialog2.datePicker.minDate=calendar2.timeInMillis
        dialog2.datePicker.maxDate = Calendar.getInstance().timeInMillis
        dialog2.show()
    }

    fun onSubmit(view: View) {

        if(startDate.isEmpty()||endDate.isEmpty()){
            Toast.makeText(this, getString(R.string.required_dates), Toast.LENGTH_LONG).show()
        }else{
            textView.text="Fetching records from $startDate to $endDate"

            val input=Input(startDate=startDate, endDate= endDate)

            if(isConnected(this))
                fetchRtPcrData(input)
            else textView.text=getString(R.string.required_internet)
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.app_description)
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