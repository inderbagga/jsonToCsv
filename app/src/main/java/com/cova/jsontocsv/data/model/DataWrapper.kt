package com.cova.jsontocsv.data.model

import com.google.gson.annotations.SerializedName

data class DataWrapper (

    @SerializedName("response") val response : Int,
    @SerializedName("sys_message") val sys_message : String,
    @SerializedName("data") val data : List<Data>
)
