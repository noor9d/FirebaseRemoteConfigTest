package com.example.remoteconfigtest

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.remoteconfigtest.databinding.ActivityMainBinding
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class MainActivity : AppCompatActivity() {
    private val binding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var remoteConfig: FirebaseRemoteConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val defaults: HashMap<String, Any> = hashMapOf(
                LOADING_PHRASE_CONFIG_KEY to "Fetching config…",
                WELCOME_MESSAGE_KEY to "Welcome to my awesome app!",
                WELCOME_MESSAGE_CAPS_KEY to "false",
                NEW_VERSION_CODE_KEY to "1"
            )

        binding.fetchButton.setOnClickListener { fetchWelcome() }

        // Get Remote Config instance.
        remoteConfig = FirebaseRemoteConfig.getInstance()

        val configSettings = FirebaseRemoteConfigSettings.Builder().apply {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) {
                0 // Kept 0 for quick debug
            } else {
                60 * 60 // Change this based on your requirement
            }
        }.build()

        remoteConfig.setConfigSettingsAsync(configSettings)

//        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.setDefaultsAsync(defaults)

        fetchWelcome()
    }

    /**
     * Fetch a welcome message from the Remote Config service, and then activate it.
     */
    private fun fetchWelcome() {
        binding.welcomeTextView.text = remoteConfig.getString(LOADING_PHRASE_CONFIG_KEY)
        binding.tvVersionCode.text = "Version: ${getVersionCode()}"

        // [START fetch_config_with_callback]
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d(TAG, "Config params updated: $updated")
                    Toast.makeText(this, "Fetch and activate succeeded",
                        Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Fetch failed",
                        Toast.LENGTH_SHORT).show()
                }
                displayWelcomeMessage()
            }
    }

    private fun getVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    private fun showNewDialog(version: Int) {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Update")
        alertDialog.setMessage("The version is absolute, please update to version: $version")
        alertDialog.setPositiveButton("Update") { dialog, which ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }.show()
    }

    /**
     * Display a welcome message in all caps if welcome_message_caps is set to true. Otherwise,
     * display a welcome message as fetched from welcome_message.
     */
    private fun displayWelcomeMessage() {
        val version = remoteConfig.getString(NEW_VERSION_CODE_KEY)
        if (version > getVersionCode().toString()) {
            showNewDialog(version.toInt())
        }


        val welcomeMessage = remoteConfig.getString(WELCOME_MESSAGE_KEY)
        binding.welcomeTextView.isAllCaps = remoteConfig.getBoolean(WELCOME_MESSAGE_CAPS_KEY)
        binding.welcomeTextView.text = welcomeMessage
    }

    companion object {

        private const val TAG = "MainActivity"

        // Remote Config keys
        private const val LOADING_PHRASE_CONFIG_KEY = "loading_phrase"
        private const val WELCOME_MESSAGE_KEY = "welcome_message"
        private const val WELCOME_MESSAGE_CAPS_KEY = "welcome_message_caps"
        private const val NEW_VERSION_CODE_KEY = "new_version_code"
    }
}