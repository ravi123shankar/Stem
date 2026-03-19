package com.stem.assistant

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnActivate = findViewById<Button>(R.id.btnActivate)

        btnActivate.setOnClickListener {
            Toast.makeText(this, "Stem is listening...", Toast.LENGTH_SHORT).show()
        }
    }
}