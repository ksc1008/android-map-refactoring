package ksc.campus.tech.kakao.map.data.datasources

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import ksc.campus.tech.kakao.map.data.apis.KakaoSearchRetrofitService
import ksc.campus.tech.kakao.map.data.entities.Document
import ksc.campus.tech.kakao.map.data.entities.KakaoSearchDTO
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject

class SearchResultRemoteDataSource @Inject constructor(
    private val retrofitService: KakaoSearchRetrofitService
) {

    private fun isQueryValid(query: String): Boolean = query.isNotBlank()

    private fun responseToArray(response: Response<KakaoSearchDTO>): List<Document> =
        response.body()?.documents ?: listOf()

    fun searchResult(
        query: String,
        apiKey: String,
        batchCount: Int
    ): Flow<List<Document>> {
        return flow {
            try {
                val result = if (query == "") listOf() else batchSearchByKeyword(
                    query,
                    apiKey,
                    1,
                    batchCount
                )
                Log.d("KSC", "Searched")
                emit(result)
            } catch (e: HttpException) {
                Log.e("KSC", e.message ?: "")
                emit(listOf())
            } catch (e: Exception) {
                Log.e("KSC", e.message ?: "")
                emit(listOf())
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun batchSearchByKeyword(
        query: String,
        apiKey: String,
        page: Int,
        batchCount: Int
    ): List<Document> {
        if (page > batchCount)
            return listOf()

        if (!isQueryValid(query))
            return listOf()

        val result = mutableListOf<Document>()
        val response =
            retrofitService.requestSearchResultByKeyword("KakaoAK $apiKey", query, page).execute()

        if (!response.isSuccessful) {
            Log.e("KSC", "Message: ${response.message()}")
            return listOf()
        }

        result += responseToArray(response)
        if (response.body()?.meta?.isEnd == false) {
            result += batchSearchByKeyword(
                query,
                apiKey,
                page + 1,
                batchCount
            )
        }

        return result
    }
}
