package com.example.weatherleveling

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherleveling.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private companion object{
        private const val RC_SIGN_IN = 100
        const val TAG = "GOOGLE_SIGN_IN_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Silahkan Tunggu")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle click, begin login
        binding.loginBtn.setOnClickListener {
            validateData()
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //init firebase auth
//        firebaseAuth = FirebaseAuth.getInstance()
//        checkUser()

        //handle click, not have account, goto register screen
        binding.regisBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        //google SignIn Button, Click to begin Google SignIn
        binding.googleSignInBtn.setOnClickListener {
            //begin Google SignIn
            Log.d(TAG, "onCreate: begin Google SignIn")
            val intent = googleSignInClient.signInIntent
            startActivityForResult(intent, RC_SIGN_IN)
        }
    }

    private var email = ""
    private var password = ""
    private fun validateData() {
        //1. input data
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        //2. validate data
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this, "Format email salah...", Toast.LENGTH_SHORT).show()
        }
        else if (password.isEmpty()){
            Toast.makeText(this, "Masukan sandi...", Toast.LENGTH_SHORT).show()
        }
        else{
            loginUser()
        }
    }

    private fun loginUser() {
        //3. Login - Firebase auth
        progressDialog.setMessage("Masuk...")
        progressDialog.show()
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                //login success
                checkPengguna()
            }
            .addOnFailureListener { e->
                //failed login
                progressDialog.dismiss()
                Toast.makeText(this, "Login gagal karena ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkPengguna() {
        /*4. Check user type - firebase auth
        * if user - move to user dashboard
        * if admin - move to admin dashboard */
        progressDialog.setMessage("Memeriksa Pengguna...")

        val firebaseUser = firebaseAuth.currentUser!!
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    progressDialog.dismiss()
                    //get user type e.g. user or admin
                    val userType = snapshot.child("userType").value
                    if (userType == "user"){
                        //its simple user, open user dashboard
                        startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                        finish()
                    }
                    else if (userType == "admin"){
                        //its admin, open admin dashboard
                        startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

//    private fun checkUser() {
//        //check if user is logged in or not
//        val firebaseUser = firebaseAuth.currentUser
//        if (firebaseUser !=null){
//            //user is already logged in
//            //start profile activity
//            startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
//            finish()
//        }
//    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN){
            Log.d(TAG, "onActivityResult: Google Sign intent result")
            val accountTask = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                //google SingIn Success, now auth with firebase
                val account = accountTask.getResult(ApiException::class.java)
                firebaseAuthWithGoogleAccount(account)
            }
            catch (e: Exception){
                //failed google SignIn
                Log.d(TAG, "onActivityResult: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogleAccount(account: GoogleSignInAccount?) {
        Log.d(TAG, "firebaseAuthWithGoogleAccount: begin firebase auth with google account")

        val credential = GoogleAuthProvider.getCredential(account!!.idToken,null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                //login success
                Log.d(TAG, "firebaseAuthWithGoogleAccount: LoggedIn")

                //get loggedIn user
                val firebaseUser = firebaseAuth.currentUser
                //get user info
                val uid = firebaseUser!!.uid
                val email = firebaseUser.email

                Log.d(TAG, "firebaseAuthWithGoogleAccount: Uid : $uid")
                Log.d(TAG, "firebaseAuthWithGoogleAccount: Email : $email")

                //check if user is new or existing
                if (authResult.additionalUserInfo!!.isNewUser){
                    Log.d(TAG, "firebaseAuthWithGoogleAccount: Account created... \n$email")
                    Toast.makeText(this@MainActivity, "Account created... \n$email", Toast.LENGTH_SHORT).show()
                }
                else{
                    //existing user - loggedOn
                    Log.d(TAG, "firebaseAuthWithGoogleAccount: Existing... \n$email")
                    Toast.makeText(this@MainActivity, "LoggedIn... \n$email", Toast.LENGTH_SHORT).show()
                }
                //start profile activity
                startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                finish()
            }
            .addOnFailureListener{ e ->
                //login failed
                Log.d(TAG, "firebaseAuthWithGoogleAccount: Loggin Failed due to ${e.message}")
                Toast.makeText(this@MainActivity, "Loggin Failed due to ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }
}