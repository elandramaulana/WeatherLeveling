package com.example.weatherleveling

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherleveling.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding : ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Silahkan Tunggu")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle back button click,
        binding.backBtn.setOnClickListener {
            onBackPressed()// goto previous scren
        }

        //handle click,  begin register
        binding.registerBtn.setOnClickListener {
            validateData()
        }
    }
    private var name = ""
    private var email = ""
    private var password = ""

    private fun validateData() {
        //input data
        name = binding.nameEt.text.toString().trim()
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        val cPassword = binding.cPasswordEt.text.toString().trim()
        //validate Data
        if (name.isEmpty()){
            Toast.makeText(this, "Masukan nama anda...", Toast.LENGTH_SHORT).show()
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this, "Email tidak valid...", Toast.LENGTH_SHORT).show()
        }
        else if (password.isEmpty()){
            //empty password
            Toast.makeText(this, "Masukan sandi...", Toast.LENGTH_SHORT).show()
        }
        else if (cPassword.isEmpty()){
            //empty password
            Toast.makeText(this, "Konfirmasi sandi...", Toast.LENGTH_SHORT).show()
        }
        else if (password != cPassword){
            Toast.makeText(this, "Kata sandi tidak cocok...", Toast.LENGTH_SHORT).show()
        }
        else{
            createUserAccount()
        }
    }

    private fun createUserAccount() {
        //create account - firebase Auth
        progressDialog.setMessage("Membuat akun...")
        progressDialog.show()
        //create user in firebase auth
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                //account created, now add user info in db
                updateUserInfo()
            }
            .addOnFailureListener { e->
                //failed creating account
                progressDialog.dismiss()
                Toast.makeText(this, "Gagal membuat akun karena ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserInfo() {
        //save user info - firebase Realtime Database
        progressDialog.setMessage("Menyimpan info...")
        //timestamp
        val timestamp = System.currentTimeMillis()

        //get current user uid, since user is registered so we can get it now
        val uid = firebaseAuth.uid
        //setup data to add in db
        val hashMap: HashMap<String, Any?> = HashMap()
        hashMap["uid"] = uid
        hashMap["email"] = email
        hashMap["name"] = name
        hashMap["profileImage"] = "" //add empty,will do in profile edit
        hashMap["userType"] = "user" //possible values are user/admin,will change value to make admin manually on firebase db
        hashMap["timestamp"] = timestamp
        //set data to db
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                //user info saved, open user dashboard
                progressDialog.dismiss()
                Toast.makeText(this, "Account Created...", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, DashboardActivity::class.java))
                finish()
            }
            .addOnFailureListener { e->
                //failed adding data to db
                progressDialog.dismiss()
                Toast.makeText(this, "Failed saving user info due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

