package com.ayc.canalguide.repos

import android.util.Log
import com.ayc.canalguide.network.NoNetworkException
import retrofit2.Response
import java.io.IOException

open class BaseRepository {


    suspend fun <T : Any> safeApiCallPair(call: suspend () -> Response<T>, errorMessage: String): Pair<T, String>? {
        val result: Result<Pair<T, String>> = safeApiResultPair(call, errorMessage)
        var data: Pair<T, String>? = null

        when (result) {
            is Result.Success ->
                data = result.data
            is Result.Error -> {
                Log.d("Repository", "$errorMessage & Exception - ${result.exception}")
            }
        }

        return data
    }

    private suspend fun <T : Any> safeApiResultPair(call: suspend () -> Response<T>, errorMessage: String): Result<Pair<T, String>> {
        var exception: Exception? = null
        try {
            val response = call.invoke()
            if (response.isSuccessful)
                return Result.Success(Pair(response.body()!!, response.headers()["Last-Modified"]!!))
        } catch (e: NoNetworkException) {
            exception = e
        } catch (e: Exception) {
            exception = e
        }

        return Result.Error(IOException("Error Occurred during getting safe Api result, exception = $exception, Custom ERROR - $errorMessage"))
    }

    suspend fun <T : Any> safeApiCall(call: suspend () -> Response<T>, errorMessage: String): T? {
        val result: Result<T> = safeApiResult(call, errorMessage)
        var data: T? = null

        when (result) {
            is Result.Success ->
                data = result.data
            is Result.Error -> {
                Log.d("Repository", "$errorMessage & Exception - ${result.exception}")
            }
        }

        return data
    }

    private suspend fun <T : Any> safeApiResult(call: suspend () -> Response<T>, errorMessage: String): Result<T> {
        var exception: Exception? = null
        try {
            val response = call.invoke()
            if (response.isSuccessful)
                return Result.Success(response.body()!!)
        } catch (e: NoNetworkException) {
            exception = e
        } catch (e: Exception) {
            exception = e
        }

        return Result.Error(IOException("Error Occurred during getting safe Api result, exception = $exception, Custom ERROR - $errorMessage"))
    }

    suspend fun safeApiHeadLastModified(call: suspend () -> Response<Void>, errorMessage: String): String? {
        var exception: Exception? = null
        try {
            val response = call.invoke()
            if (response.isSuccessful)
                return response.headers()["Last-Modified"]
        } catch (e: NoNetworkException) {
            exception = e
        } catch (e: Exception) {
            exception = e
        }

        Log.d("Repository", "error getting head $errorMessage & Exception - $exception")
        return null
    }

}