package com.example.weatherapp
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherapp.network.RetrofitInstance
import com.example.weatherapp.network.WeatherResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CityWeatherActivity : AppCompatActivity() {
    private lateinit var etCityName: EditText
    private lateinit var btnFetchWeather: Button
    private lateinit var tvCityWeather: TextView
    private lateinit var btnReturnToMain: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_city_weather)
        etCityName = findViewById(R.id.etCityName)
        btnFetchWeather = findViewById(R.id.btnFetchWeather)
        tvCityWeather = findViewById(R.id.tvCityWeather)
        btnReturnToMain = findViewById(R.id.btnReturnToMain)
        btnFetchWeather.setOnClickListener {
            val cityName = etCityName.text.toString()
            if (cityName.isNotEmpty()) {
                fetchCityWeather(cityName)
            } else {
                Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show()
            }
        }

        btnReturnToMain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


    }

    private fun fetchCityWeather(city: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.weatherService.getCityWeather(city, "e78f22a368367759ac270454d275de51")
                if (response.isSuccessful && response.body() != null) {
                    val weatherData = response.body()!!
                    runOnUiThread {
                        updateCityWeatherUI(weatherData)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@CityWeatherActivity, "Failed to fetch weather", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CityWeatherError", "Error fetching weather", e)
                runOnUiThread {
                    Toast.makeText(this@CityWeatherActivity, "Error fetching weather", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateCityWeatherUI(weather: WeatherResponse) {
        val tempCelsius = weather.main.temp - 273.15
        tvCityWeather.text = "City: ${weather.name}\nTemp: ${"%.1f".format(tempCelsius)}°C, ${weather.weather.first().description}"
    }
}
