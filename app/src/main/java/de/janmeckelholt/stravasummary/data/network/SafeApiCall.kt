package de.janmeckelholt.stravasummary.data.network

sealed class StravaResult<out T> {
    data class Success<T>(val data: T) : StravaResult<T>()
    data class Error(val code: Int, val message: String?) : StravaResult<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): StravaResult<T> {
    return try {
        StravaResult.Success(apiCall())
    } catch (e: retrofit2.HttpException) {
        val errorBody = e.response()?.errorBody()?.string()
        StravaResult.Error(e.code(), errorBody)
    } catch (e: Exception) {
        StravaResult.Error(-1, e.localizedMessage)
    }
}