package com.camo.ip_project.util


data class Resource<out T>(val status: Status, val data: T?, val errorInfo: String?) {
    companion object {
        fun <T> idle(): Resource<T> = Resource(status = Status.IDLE, null, null)
        fun <T> success(data: T): Resource<T> =
            Resource(status = Status.SUCCESS, data = data, errorInfo = null)

        fun <T> error(data: T? = null, errorInfo: String): Resource<T> =
            Resource(status = Status.ERROR, data = data, errorInfo = errorInfo)

        fun <T> loading(data: T? = null): Resource<T> =
            Resource(status = Status.LOADING, data = data, errorInfo = null)
    }
}

enum class Status {
    IDLE,
    SUCCESS,
    ERROR,
    LOADING
}