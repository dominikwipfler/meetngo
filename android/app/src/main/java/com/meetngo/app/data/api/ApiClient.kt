package com.meetngo.app.data.api

import com.meetngo.app.data.repository.AuthRepository
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Use your machine's LAN IP here, not localhost — the emulator/device
 * resolves localhost to itself, not your dev machine running the backend.
 */
const val BASE_URL = "http://10.0.2.2:3001/"

/**
 * Zentrale Fabrik für die Retrofit-[ApiService]-Instanz.
 *
 * Hält das aktuelle Auth-Token als Singleton-Zustand, damit es ohne
 * zusätzliche Parameterübergabe in jeden ausgehenden Request injiziert
 * werden kann (siehe [authInterceptor] in [create]).
 */
object ApiClient {
    /** Aktuelles JWT, wird vom [AuthRepository] nach Login/Logout gesetzt. `@Volatile`, da von mehreren Threads (UI/IO) gelesen/geschrieben werden kann. */
    @Volatile
    private var token: String? = null

    /** Setzt (oder löscht mit `null`) das Token, das künftigen Requests als Bearer-Header mitgegeben wird. */
    fun setToken(t: String?) {
        token = t
    }

    /**
     * Baut einen neuen [ApiService] inkl. OkHttp-Client mit Logging- und
     * Auth-Interceptor. [authRepository] wird aktuell nicht direkt verwendet,
     * steht aber als Erweiterungspunkt zur Verfügung (z. B. für künftiges
     * automatisches Token-Refresh).
     */
    fun create(authRepository: AuthRepository): ApiService {
        // Loggt Request/Response-Zeilen (Methode, URL, Statuscode) für einfacheres Debugging.
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        // Hängt bei jedem Request automatisch den Authorization-Header an, sofern ein Token gesetzt ist.
        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            token?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }
            chain.proceed(requestBuilder.build())
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
