package com.gyzo.kotlindevexam

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val genderOptions = arrayOf("Choose Gender", "Male", "Female", "Other")

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        val fullNameEditText = findViewById<EditText>(R.id.fullName)
        val emailEditText = findViewById<EditText>(R.id.email)
        val mobileNumberEditText = findViewById<EditText>(R.id.mobileNumber)
        val dateOfBirthEditText = findViewById<EditText>(R.id.dateOfBirth)
        val ageTextView = findViewById<TextView>(R.id.age)
        val genderSpinner = findViewById<Spinner>(R.id.gender)
        val submitButton = findViewById<Button>(R.id.submitButton)

        val genderAdapter = ArrayAdapter(this, R.layout.gender_spinner_item, genderOptions)
        genderAdapter.setDropDownViewResource(R.layout.gender_dropdown)
        genderSpinner.adapter = genderAdapter

        dateOfBirthEditText.setOnClickListener {
            showDatePicker(dateOfBirthEditText)
        }

        dateOfBirthEditText.addTextChangedListener {
            calculateAge(it.toString(), ageTextView)
        }

        submitButton.setOnClickListener {
            if (validateForm(fullNameEditText, emailEditText, mobileNumberEditText, dateOfBirthEditText, ageTextView, genderSpinner)) {
                Toast.makeText(this, "Form submitted!", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun showDatePicker(
        dateOfBirthEditText: EditText
    ) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            {
            _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateOfBirthEditText.setText(dateFormat.format(selectedDate.time))
            },
            year, month, day
        )

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun calculateAge(dateOfBirth: String, ageTextView: TextView): Int {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateOfBirthDate = dateFormat.parse(dateOfBirth)
        val today = Calendar.getInstance()
        val calculate = Calendar.getInstance()
        calculate.time = dateOfBirthDate

        var age = today.get(Calendar.YEAR) - calculate.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < calculate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        ageTextView.text = age.toString()

        return age
    }

    private fun validateForm(
        fullNameEditText: EditText,
        emailEditText: EditText,
        mobileNumberEditText: EditText,
        dateOfBirthEditText: EditText,
        ageTextView: TextView,
        genderSpinner: Spinner
    ): Boolean {
        val fullName = fullNameEditText.text.toString()
        val email = emailEditText.text.toString().trim()
        val mobileNumber = mobileNumberEditText.text.toString().trim()
        val dateOfBirth = dateOfBirthEditText.text.toString().trim()
        val gender = genderSpinner.selectedItem.toString()

        if (fullName.isEmpty() || email.isEmpty() || mobileNumber.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!isValidMobileNumber(mobileNumber)) {
            Toast.makeText(this, "Invalid mobile number", Toast.LENGTH_SHORT).show()
            return false
        }

        val age = calculateAge(dateOfBirth, ageTextView)
        if (age < 18) {
            Toast.makeText(this, "You must be 18 or above", Toast.LENGTH_SHORT).show()
            return false
        }

        if (gender == "Choose Gender") {
            Toast.makeText(this, "Select a gender", Toast.LENGTH_SHORT).show()
            return false
        }

        Toast.makeText(this, "Form success", Toast.LENGTH_SHORT).show()
        sendFormData(fullName, email, mobileNumber, dateOfBirth, age, gender)
        return true
    }


    private fun isValidMobileNumber(mobileNumber: String): Boolean {
        val numberRegex = Regex("^09\\d{9}\$")
        return mobileNumber.matches(numberRegex)
    }

    private fun sendFormData(
        fullName: String,
        email: String,
        mobileNumber: String,
        dateOfBirth: String,
        age: Int,
        gender: String
    ) {
        val url = "https://6666ab29a2f8516ff7a45128.mockapi.io/fullName"
        val jsonObject = JSONObject().apply {
            put("fullName", fullName)
            put("emailAddress", email)
            put("mobileNumber", mobileNumber)
            put("dateOfBirth", dateOfBirth)
            put("age", age)
            put("gender", gender)
        }
        val jsonString = jsonObject.toString()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonString.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    withContext(context = Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Response: $responseData", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Exception: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

//    private fun sendFormData(
//        fullName: String,
//        email: String,
//        mobileNumber: String,
//        dateOfBirth: String,
//        age: Int,
//        gender: String
//    ) {
//        val url = "https://run.mocky.io/v3/ed702c99-0ac2-4910-bfa4-0e73ca297aa5"
//
//        val requestBody = JSONObject().apply {
//            put("fullName", fullName)
//            put("emailAddress", email)
//            put("mobileNumber", mobileNumber)
//            put("dateOfBirth", dateOfBirth)
//            put("age", age)
//            put("gender", gender)
//        }.toString()
//
//        val request = Request.Builder()
//            .url(url)
//            .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
//
//        val thread = Thread {
//            try {
//                val urlObject = URL(url)
//                val httpURLConnection = urlObject.openConnection() as HttpURLConnection
//                httpURLConnection.requestMethod = "POST"
//                httpURLConnection.setRequestProperty("Content-Type", "application/json")
//                httpURLConnection.setRequestProperty("Accept", "application/json")
//                httpURLConnection.doOutput = true
//
//                val jsonInputString = """{"fullName": $fullName, "emailAddress": $email, "mobileNumber": $mobileNumber, "dateOfBirth": $dateOfBirth, "age": $age, "gender": $gender}"""
//
//                OutputStreamWriter(httpURLConnection.outputStream).use {
//                    writer ->
//                    writer.write(jsonInputString)
//                    writer.flush()
//                }
//
//                val responseCode = httpURLConnection.responseCode
//                if (responseCode == HttpURLConnection.HTTP_OK) {
//                    val inputStream = httpURLConnection.inputStream
//                    val reader = BufferedReader(InputStreamReader(inputStream))
//                    val response = StringBuilder()
//                    var line: String?
//                    while (reader.readLine().also { line = it } != null) {
//                        response.append(line)
//                    }
//                    println("Response: $response")
//                    inputStream.close()
//                } else {
//                    println("Error: $responseCode")
//                }
//
//                httpURLConnection.disconnect()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//        thread.start()
//    }
    }
}


