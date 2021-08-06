package com.cova.jsontocsv.api

import com.google.gson.annotations.SerializedName

data class Input (

    @SerializedName("form_status") var formStatus : String,
    @SerializedName("start_date") var startDate : String,
    @SerializedName("end_date") var endDate : String,
    @SerializedName("user_id") var userId : String
)