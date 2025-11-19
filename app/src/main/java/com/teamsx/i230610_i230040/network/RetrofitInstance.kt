package com.teamsx.i230610_i230040.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    // IMPORTANT: Update this IP address based on your setup:
    //
    // 1. For Android Emulator:
    //    Use: http://10.0.2.2/socially_api/endpoints/
    //    (10.0.2.2 is the special IP for emulator to access host machine's localhost)
    //
    // 2. For Physical Device on same WiFi:
    //    Find your PC's IP:
    //    - Open Command Prompt (cmd)
    //    - Type: ipconfig
    //    - Look for "IPv4 Address" under your WiFi adapter
    //    - Example: http://192.168.1.100/socially_api/endpoints/
    //
    // 3. Make sure:
    //    - XAMPP Apache is running (port 80)
    //    - XAMPP MySQL is running (port 3306 or 3307)
    //    - Windows Firewall allows connections on port 80
    //    - Your PHP files are in: C:\xampp\htdocs\socially_api\
    //
    // Current IP: Use 10.0.2.2 for EMULATOR or 192.168.100.6 for PHYSICAL DEVICE
    // CURRENT SETTING: For EMULATOR (change to 192.168.100.6 for physical device)
    private const val BASE_URL = "http://192.168.100.6/socially_api/endpoints/"

    // Test URL (publicly accessible for debugging)
    const val TEST_URL = "http://192.168.100.6/socially_api/test.php"
    const val CURRENT_BASE = BASE_URL

    // HTTP Logging Interceptor for debugging
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttp Client with timeouts and logging
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Retrofit instance
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // API Service
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}

