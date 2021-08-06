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
import com.cova.jsontocsv.data.model.Data
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

                                    this@MainActivity.saveCsv(dataWrapper.data,it.getJSONArray("data"))
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

    private fun saveCsv(records: List<Data>, jsonArray: JSONArray) {

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

                    for (record in records) {

                        if(record.rOWNUM.isEmpty()) {
                            append("NA")
                        } else append(record.rOWNUM)
                        append(',')

                        if(record.row_Id.isEmpty()) {
                            append("NA")
                        } else append(record.row_Id)
                        append(',')

                        if(record.log_date.isEmpty()) {
                            append("NA")
                        } else append(record.log_date)
                        append(',')

                        if(record.patient_name.isEmpty()) {
                            append("NA")
                        } else append(record.patient_name)
                        append(',')

                        if(record.reinfection_YN.isEmpty()) {
                            append("NA")
                        } else append(record.reinfection_YN)
                        append(',')

                        if(record.patient_reinfection_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_reinfection_id)
                        append(',')

                        if(record.patient_month.isEmpty()) {
                            append("NA")
                        } else append(record.patient_month)
                        append(',')

                        if(record.date_of_reinfection.isEmpty()) {
                            append("NA")
                        } else append(record.date_of_reinfection)
                        append(',')

                        if(record.patient_mobile.isEmpty()) {
                            append("NA")
                        } else append(record.patient_mobile)
                        append(',')

                        if(record.patient_father_name.isEmpty()) {
                            append("NA")
                        } else append(record.patient_father_name)
                        append(',')

                        if(record.patient_state_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_state_id)
                        append(',')

                        if(record.patient_district_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_district_id)
                        append(',')

                        if(record.patient_occupation_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_occupation_id)
                        append(',')

                        if(record.region_id.isEmpty()) {
                            append("NA")
                        } else append(record.region_id)
                        append(',')

                        if(record.city_Id.isEmpty()) {
                            append("NA")
                        } else append(record.city_Id)
                        append(',')

                        if(record.city_others.isEmpty()) {
                            append("NA")
                        } else append(record.city_others)
                        append(',')

                        if(record.tehsil_id.isEmpty()) {
                            append("NA")
                        } else append(record.tehsil_id)
                        append(',')

                        if(record.village_id.isEmpty()) {
                            append("NA")
                        } else append(record.village_id)
                        append(',')

                        if(record.village_others.isEmpty()) {
                            append("NA")
                        } else append(record.village_others)
                        append(',')

                        if(record.patient_address.isEmpty()) {
                            append("NA")
                        } else append(record.patient_address)
                        append(',')

                        if(record.patient_gender_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_gender_id)
                        append(',')

                        if(record.patient_nationality_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_nationality_id)
                        append(',')

                        if(record.age.isEmpty()) {
                            append("NA")
                        } else append(record.age)
                        append(',')

                        if(record.patient_age_in.isEmpty()) {
                            append("NA")
                        } else append(record.patient_age_in)
                        append(',')

                        if(record.aadhaar.isEmpty()) {
                            append("NA")
                        } else append(record.aadhaar)
                        append(',')

                        if(record.passport.isEmpty()) {
                            append("NA")
                        } else append(record.passport)
                        append(',')

                        if(record.recieved_covidvaccine.isEmpty()) {
                            append("NA")
                        } else append(record.recieved_covidvaccine)
                        append(',')

                        if(record.patient_vaccinetype.isEmpty()) {
                            append("NA")
                        } else append(record.patient_vaccinetype)
                        append(',')

                        if(record.date_of_vaccine_dose_first.isEmpty()) {
                            append("NA")
                        } else append(record.date_of_vaccine_dose_first)
                        append(',')

                        if(record.br_spicemen_model.isEmpty()) {
                            append("NA")
                        } else append(record.br_spicemen_model)
                        append(',')

                        if(record.date_of_spicemen_collection.isEmpty()) {
                            append("NA")
                        } else append(record.date_of_spicemen_collection)
                        append(',')

                        if(record.spicemen_sample.isEmpty()) {
                            append("NA")
                        } else append(record.spicemen_sample)
                        append(',')

                        if(record.patient_zone_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_zone_id)
                        append(',')

                        if(record.cat_id.isEmpty()) {
                            append("NA")
                        } else append(record.cat_id)
                        append(',')

                        if(record.patient_status.isEmpty()) {
                            append("NA")
                        } else append(record.patient_status)
                        append(',')

                        if(record.patient_lab_confirmed.isEmpty()) {
                            append("NA")
                        } else append(record.patient_lab_confirmed)
                        append(',')

                        if(record.br_symptoms_model.isEmpty()) {
                            append("NA")
                        } else append(record.br_symptoms_model)
                        append(',')

                        if(record.symptoms_others.isEmpty()) {
                            append("NA")
                        } else append(record.symptoms_others)
                        append(',')

                        if(record.date_of_onset_of_first_symptom.isEmpty()) {
                            append("NA")
                        } else append(record.date_of_onset_of_first_symptom)
                        append(',')

                        if(record.patient_hospitalized_yn.isEmpty()) {
                            append("NA")
                        } else append(record.patient_hospitalized_yn)
                        append(',')

                        if(record.date_of_hospitalisation.isEmpty()) {
                            append("NA")
                        } else append(record.date_of_hospitalisation)
                        append(',')

                        if(record.patient_hos_state_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_hos_state_id)
                        append(',')

                        if(record.patient_hos_district_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_hos_district_id)
                        append(',')

                        if(record.hospital_name.isEmpty()) {
                            append("NA")
                        } else append(record.hospital_name)
                        append(',')

                        if(record.test_type_id.isEmpty()) {
                            append("NA")
                        } else append(record.test_type_id)
                        append(',')

                        if(record.patient_kit_type_antigen_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_kit_type_antigen_id)
                        append(',')

                        if(record.antigenkit_spicemen_sample.isEmpty()) {
                            append("NA")
                        } else append(record.antigenkit_spicemen_sample)
                        append(',')

                        if(record.patient_antigentest_result_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_antigentest_result_id)
                        append(',')

                        if(record.patient_antigentest_date.isEmpty()) {
                            append("NA")
                        } else append(record.patient_antigentest_date)
                        append(',')

                        if(record.patient_antigentest_time.isEmpty()) {
                            append("NA")
                        } else append(record.patient_antigentest_time)
                        append(',')

                        if(record.patient_lab_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_lab_id)
                        append(',')

                        if(record.is_Active.isEmpty()) {
                            append("NA")
                        } else append(record.is_Active)
                        append(',')

                        if(record.is_Deleted.isEmpty()) {
                            append("NA")
                        } else append(record.is_Deleted)
                        append(',')

                        if(record.updated_date.isEmpty()) {
                            append("NA")
                        } else append(record.updated_date)
                        append(',')

                        if(record.user_Id.isEmpty()) {
                            append("NA")
                        } else append(record.user_Id)
                        append(',')

                        if(record.is_completed_status.isEmpty()) {
                            append("NA")
                        } else append(record.is_completed_status)
                        append(',')

                        if(record.is_completed_date.isEmpty()) {
                            append("NA")
                        } else append(record.is_completed_date)
                        append(',')

                        if(record.form_step_no.isEmpty()) {
                            append("NA")
                        } else append(record.form_step_no)
                        append(',')

                        if(record.srf_Id.isEmpty()) {
                            append("NA")
                        } else append(record.srf_Id)
                        append(',')

                        if(record.remarks.isEmpty()) {
                            append("NA")
                        } else append(record.remarks)
                        append(',')

                        if(record.covid19_result_egene.isEmpty()) {
                            append("NA")
                        } else append(record.covid19_result_egene)
                        append(',')

                        if(record.ct_value_screening.isEmpty()) {
                            append("NA")
                        } else append(record.ct_value_screening)
                        append(',')

                        if(record.orf1b_confirmatory.isEmpty()) {
                            append("NA")
                        } else append(record.orf1b_confirmatory)
                        append(',')

                        if(record.ct_value_orf1b.isEmpty()) {
                            append("NA")
                        } else append(record.ct_value_orf1b)
                        append(',')

                        if(record.rdrp_confirmatory.isEmpty()) {
                            append("NA")
                        } else append(record.rdrp_confirmatory)
                        append(',')

                        if(record.ct_value_rdrp.isEmpty()) {
                            append("NA")
                        } else append(record.ct_value_rdrp)
                        append(',')

                        if(record.final_result_of_sample.isEmpty()) {
                            append("NA")
                        } else append(record.final_result_of_sample)
                        append(',')

                        if(record.sample_rdate.isEmpty()) {
                            append("NA")
                        } else append(record.sample_rdate)
                        append(',')

                        if(record.date_of_vaccine_dose_second.isEmpty()) {
                            append("NA")
                        } else append(record.date_of_vaccine_dose_second)
                        append(',')

                        if(record.icmr_Id.isEmpty()) {
                            append("NA")
                        } else append(record.icmr_Id)
                        append(',')

                        if(record.repeat_sample.isEmpty()) {
                            append("NA")
                        } else append(record.repeat_sample)
                        append(',')

                        if(record.patient_id.isEmpty()) {
                            append("NA")
                        } else append(record.patient_id)
                        append(',')

                        if(record.previously_patient_icmr_Id.isEmpty()) {
                            append("NA")
                        } else append(record.previously_patient_icmr_Id)
                        append(',')

                        if(record.ward_id.isEmpty()) {
                            append("NA")
                        } else append(record.ward_id)
                        append(',')

                        if(record.localityname.isEmpty()) {
                            append("NA")
                        } else append(record.localityname)
                        append(',')

                        if(record.other_occupation.isEmpty()) {
                            append("NA")
                        } else append(record.other_occupation)
                        append(',')

                        if(record.patient_reinfection_id_name.isEmpty()) {
                            append("NA")
                        } else append(record.patient_reinfection_id_name)
                        append(',')

                        if(record.patient_state_id_name.isEmpty()) {
                            append("NA")
                        } else append(record.patient_state_id_name)
                        append(',')

                        if(record.patient_district_id_name.isEmpty()) {
                            append("NA")
                        } else append(record.patient_district_id_name)
                        append(',')

                        if(record.patient_occupation_id_name.isEmpty()) {
                            append("NA")
                        } else append(record.patient_occupation_id_name)
                        append(',')

                        if(record.region_id_name.isEmpty()) {
                            append("NA")
                        } else append(record.region_id_name)
                        append(',')

                        if(record.patient_village_city_name.isEmpty()) {
                            append("NA")
                        } else append(record.patient_village_city_name)
                        append(',')

                        if(record.tehsil_id_name.isEmpty()) {
                            append("NA")
                        } else append(record.tehsil_id_name)
                        append(',')

                        if(record.patient_gender_id_name.isEmpty()) {
                            append("NA")
                        } else append(record.patient_gender_id_name)
                        append(',')

                        if(record.patient_vaccinetype_name.isEmpty()) {
                            append("NA")
                        } else append(record.patient_vaccinetype_name)
                        append(',')

                        if(record.patient_zone_id_name.isEmpty()) {
                            append("NA")
                        } else append(record.patient_zone_id_name)
                        append(',')

                        if(record.cat_id_name.isEmpty()) {
                            append("NA")
                        } else append(record.cat_id_name)
                        append(',')

                        if(record.patient_hos_state_id_name.isEmpty()) {
                            append("NA")
                        } else append(record.patient_hos_state_id_name)
                        append(',')

                        if(record.patient_hos_district_id_name.isEmpty()) {
                            append("NA")
                        } else append(record.patient_hos_district_id_name)
                        append(',')

                        if(record.test_type_id_name.isEmpty()) {
                            append("NA")
                        } else append(record.test_type_id_name)
                        append(',')

                        if(record.lab_name.isEmpty()) {
                            append("NA")
                        } else append(record.lab_name)
                        append(',')

                        if(record.localityname_name.isEmpty()) {
                            append("NA")
                        } else append(record.localityname_name)
                        append(',')

                        if(record.totalCount.isEmpty()) {
                            append("NA")
                        } else append(record.totalCount)
                        append(',')

                        if(record.patient_village_city_name1.isEmpty()) {
                            append("NA")
                        } else append(record.patient_village_city_name1)
                        append(',')

                        if(record.lab_idsample.isEmpty()) {
                            append("NA")
                        } else append(record.lab_idsample)
                        append(',')

                        if(record.employee_Id.isEmpty()) {
                            append("NA")
                        } else append(record.employee_Id)
                        append(',')

                        if(record.user_location.isEmpty()) {
                            append("NA")
                        } else append(record.user_location)
                        append(',')

                        if(record.village_latitude.isEmpty()) {
                            append("NA")
                        } else append(record.village_latitude)
                        append(',')

                        if(record.village_longitude.isEmpty()) {
                            append("NA")
                        } else append(record.village_longitude)
                        append(',')

                        if(record.locality_latitude.isEmpty()) {
                            append("NA")
                        } else append(record.locality_latitude)
                        append(',')

                        if(record.locality_longitude.isEmpty()) {
                            append("NA")
                        } else append(record.locality_longitude)
                        append('\n')
                    }

                    //start
               /*     (0 until jsonArray.length()).forEach { i ->
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
                    }*/
                    //end
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