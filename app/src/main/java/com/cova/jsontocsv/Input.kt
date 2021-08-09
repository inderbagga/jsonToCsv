package com.cova.jsontocsv

import com.google.gson.annotations.SerializedName

const val ENDPOINT = "https://cova.punjab.gov.in/api/cova/citizen/idsp/v1/"
const val URI_RTPCR_DATA = "fetch-rtpcr-data-all"

data class Input (

    @SerializedName("form_status") var formStatus : String="C",
    @SerializedName("start_date") var startDate : String,
    @SerializedName("end_date") var endDate : String,
    @SerializedName("user_id") var userId : String="63719"
)