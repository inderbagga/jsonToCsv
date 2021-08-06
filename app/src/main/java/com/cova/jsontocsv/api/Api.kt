package com.cova.jsontocsv.api

interface Api {

    companion object {
        const val ENDPOINT = "https://cova.punjab.gov.in/api/cova/citizen/idsp/v1/"
        const val URI_RTPCR_DATA = "fetch-rtpcr-data-all"
        const val TOKEN="Bearer eyJhbGciOiJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzA0L3htbGRzaWctbW9yZSNobWFjLXNoYTI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOiI0NiIsInVzZXJ0eXBlIjoiMSIsInRzIjoiOTg1IiwiZXhwIjoxNjU3ODYwODk3LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjYzODg0IiwiYXVkIjoiaHR0cDovL2xvY2FsaG9zdDo2Mzg4NCJ9.T36k83aqVtkPFK97RKKilGOcUymtE_w6oLY3rUSd8Tc "
    }
}
