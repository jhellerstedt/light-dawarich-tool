package com.jhellerstedt.dawarich.api

sealed class DawarichApiException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class Network(message: String, cause: Throwable? = null) :
        DawarichApiException(message, cause)

    class ApiError(val statusCode: Int, message: String) :
        DawarichApiException("HTTP $statusCode: $message")

    class MalformedResponse(message: String) :
        DawarichApiException(message)

    class NotConfigured(message: String = "Dawarich base URL and API key are required") :
        DawarichApiException(message)
}
