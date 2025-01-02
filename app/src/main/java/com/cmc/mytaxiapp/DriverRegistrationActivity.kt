package com.cmc.mytaxiapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.provider.MediaStore
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences

class DriverRegistrationActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivProfileImage: ImageView
    private lateinit var btnPickImage: Button
    private lateinit var btnRegister: Button
    private lateinit var sharedPreferences: SharedPreferences

    private val PICK_IMAGE_REQUEST = 1

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_registration)

        // Initialiser SharedPreferences
        sharedPreferences = getSharedPreferences("DriverPreferences", Context.MODE_PRIVATE)

        // Vérifier si l'utilisateur est déjà enregistré
        if (sharedPreferences.getBoolean("is_registered", false)) {
            // Si l'utilisateur est déjà enregistré, rediriger vers la page d'accueil
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()  // Fermer cette activité
        }

        // Initialiser les vues
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        btnPickImage = findViewById(R.id.btnPickImage)
        btnRegister = findViewById(R.id.btnRegister)

        // Vérifier les permissions pour accéder à la galerie
        checkPermissions()

        val driverImageUri = sharedPreferences.getString("driver_image_uri", "")
        if (!driverImageUri.isNullOrEmpty()) {
            ivProfileImage.setImageURI(Uri.parse(driverImageUri))
        }

        // Configurer le bouton pour choisir une image de profil
        btnPickImage.setOnClickListener {
            // Ouvrir le sélecteur d'images
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Gérer l'enregistrement du conducteur
        btnRegister.setOnClickListener {
            val driverName = etName.text.toString()
            val driverEmail = etEmail.text.toString()
            val driverPhone = etPhone.text.toString()
            val driverPassword = etPassword.text.toString()

            // Sauvegarder les données dans SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putString("driver_name", driverName)
            editor.putString("driver_email", driverEmail)
            editor.putString("driver_phone", driverPhone)
            editor.putString("driver_password", driverPassword)
            editor.putString("driver_image_uri", sharedPreferences.getString("driver_image_uri", "")) // Sauvegarder l'URI de l'image
            editor.putBoolean("is_registered", true) // Marquer comme enregistré
            editor.apply()

            // Aller vers l'écran d'accueil
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish() // Fermer l'activité d'enregistrement
        }
    }

    // Gérer le résultat du sélecteur d'images
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST) {
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                ivProfileImage.setImageURI(selectedImageUri)

                // Sauvegarder l'URI de l'image dans SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putString("driver_image_uri", selectedImageUri.toString())
                editor.apply()
            }
        }
    }

    // Vérifier les permissions d'accès au stockage
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            // Demander la permission si elle n'est pas accordée
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                100)
        }
    }

    // Gérer la réponse de la demande de permission
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée, vous pouvez choisir une image
            } else {
                Toast.makeText(this, "Permission requise pour choisir une image", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
