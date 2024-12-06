package com.example.weatherapp

import
OpenAIService
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.example.weatherapp.network.RetrofitInstance
import com.example.weatherapp.network.WeatherResponse
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var tvLocation: TextView
    private lateinit var tvWeather: TextView
    private lateinit var tvWindSpeed: TextView
    private lateinit var tvUVIndex: TextView
    private lateinit var tvRain: TextView
    private lateinit var tvRecommendations: TextView
    private lateinit var btnGetRecommendations: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnCityWeather: Button
    private val PERMISSION_REQUEST_CODE = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        tvLocation = findViewById(R.id.tvGPSLocation)
        tvWeather = findViewById(R.id.tvWeather)
        tvWindSpeed = findViewById(R.id.tvWindSpeed)
        tvUVIndex = findViewById(R.id.tvUVIndex)
        tvRain = findViewById(R.id.tvRain)
        tvRecommendations = findViewById(R.id.tvRecommendations)
        btnGetRecommendations = findViewById(R.id.btnGetRecommendations)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkPermissions()
        btnGetRecommendations.setOnClickListener {
            getRecommendations()
        }
        btnCityWeather = findViewById(R.id.btnCityWeather)
        btnCityWeather.setOnClickListener {
            val intent = Intent(this, CityWeatherActivity::class.java)
            startActivity(intent)
        }

    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
        } else {
            requestLocationUpdates()
        }
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.firstOrNull()?.let { location ->
                    fetchWeather(location.latitude, location.longitude)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = RetrofitInstance.weatherService.getWeather(lat, lon, "e78f22a368367759ac270454d275de51")
            if (response.isSuccessful && response.body() != null) {
                val weatherData = response.body()!!
                runOnUiThread {
                    updateWeatherUI(weatherData)
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to fetch weather data: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateWeatherUI(weather: WeatherResponse) {
        val tempCelsius = weather.main.temp - 273.15
        tvWeather.text = "Temp: ${"%.1f".format(tempCelsius)}°C, ${weather.weather.first().description}"
        tvWindSpeed.text = "Wind Speed: ${weather.wind.speed} m/s"
        tvUVIndex.text = "UV Index: ${weather.uvi}"
        tvRain.text = "Rain: ${weather.rain?.`1h` ?: 0.0} mm/h"
        tvLocation.text = "City: ${weather.name}"

        val weatherIcon = findViewById<ImageView>(R.id.weatherIcon)
        val weatherCondition = weather.weather.first().description.lowercase()

        when {
            "sun" in weatherCondition -> weatherIcon.setImageResource(R.drawable.sunny)
            "cloud" in weatherCondition -> weatherIcon.setImageResource(R.drawable.cloudy)
            "rain" in weatherCondition -> weatherIcon.setImageResource(R.drawable.rainy)
            "clear" in weatherCondition -> weatherIcon.setImageResource(R.drawable.sunny)
            else -> weatherIcon.setImageResource(R.drawable.cloudy)
        }
    }
    private fun getRecommendations() {
        val weatherDescription = tvWeather.text.toString()
        val windSpeed = tvWindSpeed.text.toString()
        val uvIndex = tvUVIndex.text.toString()
        val rain = tvRain.text.toString()
        val prompt = "The current weather is $weatherDescription, with wind speed of $windSpeed, UV index of $uvIndex, and rainfall of $rain. What clothing would you recommend wearing?"
        val apiKey = "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"
        val openAIService = OpenAIService(apiKey)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages = listOf(
                    ChatMessage(
                        role = ChatRole.User,
                        content = prompt
                    )
                )
                val recommendations = openAIService.generateChatResponse(ModelId("gpt-3.5-turbo"), messages)
                runOnUiThread {
                    tvRecommendations.text = recommendations
                }
            } catch (e: Exception) {
                Log.e("RecommendationsError", "Failed to fetch recommendations", e)
                runOnUiThread {
                    tvRecommendations.text = "Error fetching recommendations. Please try again later."
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates()
        } else {
            Toast.makeText(this, "Permission denied to access location", Toast.LENGTH_SHORT).show()
        }
    }
    
}
