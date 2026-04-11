package com.example.traveling.TravelShare.Inscription

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*

class register_birthdate : AppCompatActivity() {

    private lateinit var etDate: TextInputEditText
    private lateinit var btnContinue: MaterialButton
    private lateinit var btnBack: ImageButton

    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_birthdate)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etDate = findViewById(R.id.etDate)
        btnContinue = findViewById(R.id.btnContinue)
        btnBack = findViewById(R.id.btnBack)
        val tilDate = findViewById<TextInputLayout>(R.id.tilDate)

        fun showDatePicker() {
            val calendar = Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, day ->

                    val date = String.format("%02d/%02d/%d", day, month + 1, year)
                    etDate.setText(date)
                    selectedDate = date
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        etDate.setOnClickListener { showDatePicker() }
        tilDate.setEndIconOnClickListener { showDatePicker() }

        btnContinue.setOnClickListener {

            val email = intent.getStringExtra("email")
            val nom = intent.getStringExtra("nom")
            val prenom = intent.getStringExtra("prenom")
            val password = intent.getStringExtra("password")

            if (email.isNullOrEmpty() || nom.isNullOrEmpty() || prenom.isNullOrEmpty() || password.isNullOrEmpty()) {
                showError("Données utilisateur manquantes")
                return@setOnClickListener
            }

            if (selectedDate.isNullOrEmpty()) {
                showError("Veuillez sélectionner votre date de naissance")
                return@setOnClickListener
            }

            val parts = selectedDate!!.split("/")
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()

            val calendar = Calendar.getInstance()
            val todayYear = calendar.get(Calendar.YEAR)
            val todayMonth = calendar.get(Calendar.MONTH) + 1
            val todayDay = calendar.get(Calendar.DAY_OF_MONTH)

            if (year > todayYear ||
                (year == todayYear && month > todayMonth) ||
                (year == todayYear && month == todayMonth && day > todayDay)
            ) {
                showError("Date invalide : elle ne peut pas être dans le futur")
                return@setOnClickListener
            }

            val intent = Intent(this, register_country::class.java)
            intent.putExtra("birthdate", selectedDate)
            intent.putExtra("email", email)
            intent.putExtra("nom", nom)
            intent.putExtra("prenom", prenom)
            intent.putExtra("password", password)

            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun showError(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Erreur")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}