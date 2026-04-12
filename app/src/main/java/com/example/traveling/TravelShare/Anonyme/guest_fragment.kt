package com.example.traveling.TravelShare.Anonyme

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.traveling.R
import com.example.traveling.TravelShare.Connection.login
import com.google.android.material.button.MaterialButton

class guest_fragment : Fragment(R.layout.fragment_guest_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val intent = Intent(requireContext(), login::class.java)
            startActivity(intent)
        }
    }
}