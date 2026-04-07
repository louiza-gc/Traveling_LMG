package com.example.traveling.TravelShare.Inscription

import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import android.app.DatePickerDialog
import android.content.Intent
import androidx.core.view.WindowInsetsCompat
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*

class register_birthdate : AppCompatActivity() {

    private lateinit var etDate: TextInputEditText
    private lateinit var calendarView: CalendarView
    private lateinit var btnContinue: MaterialButton
    private lateinit var btnBack: ImageButton

    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_birthdate)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialisation
        etDate = findViewById(R.id.etDate)
        btnContinue = findViewById(R.id.btnContinue)
        btnBack = findViewById(R.id.btnBack)
        val tilDate = findViewById<TextInputLayout>(R.id.tilDate)

        val showDatePicker = {

            val calendar = Calendar.getInstance()

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this,
                { _, selectedYear, selectedMonth, selectedDay ->

                    val date = String.format(
                        "%02d/%02d/%d",
                        selectedDay,
                        selectedMonth + 1,
                        selectedYear
                    )

                    etDate.setText(date)
                    selectedDate = date

                }, year, month, day)

            datePicker.show()
        }

        // clic champ
        etDate.setOnClickListener { showDatePicker() }

        // clic icône
        tilDate.setEndIconOnClickListener { showDatePicker() }

        // Récupérer l'email le nom et le prenom
        val email = intent.getStringExtra("email")
        val nom = intent.getStringExtra("nom")
        val prenom = intent.getStringExtra("prenom")

        if (email != null) {
            Toast.makeText(this, "Email reçu : $email", Toast.LENGTH_SHORT).show()
        }

        // Bouton Continuer
        btnContinue.setOnClickListener {
            if (selectedDate.isEmpty()) {
                Toast.makeText(this, "Veuillez sélectionner une date", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Date sélectionnée : $selectedDate", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, register_country::class.java)
                intent.putExtra("birthdate", selectedDate)
                intent.putExtra("email", email)
                intent.putExtra("nom", nom)
                intent.putExtra("prenom", prenom)

                startActivity(intent)
            }
        }

        //Bouton Retour
        btnBack.setOnClickListener {
            finish()
        }
    }
}