package com.example.internet_ex

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.core.content.FileProvider.getUriForFile
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import javax.net.SocketFactory
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import java.io.File
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {
    private val serverAddress = "developer.android.com"
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        output = findViewById(R.id.textView)

        if (isNetworkAvailable())
            Snackbar.make(output, "Network available", Snackbar.LENGTH_SHORT).show()
        else
            Snackbar.make(output, "Network unavailable", Snackbar.LENGTH_SHORT).show()

        findViewById<Button>(R.id.java_socket).setOnClickListener { javaSocket() }
        findViewById<Button>(R.id.java_http).setOnClickListener { httpLib() }
        findViewById<Button>(R.id.retrofit).setOnClickListener { retrofitWithCoroutine() }
        findViewById<Button>(R.id.download).setOnClickListener { downloadManager() }
        findViewById<Button>(R.id.openDownload).setOnClickListener { openDownload() }
        findViewById<Button>(R.id.volley).setOnClickListener { volley() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.restApi -> startActivity(Intent(this, RestActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        println("${actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)}, ${actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)}, " +
                "${actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)}")
        return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

    }

    private fun javaSocket() {
        output.text = ""

        // THIS IS A BAD EXAMPLE. Just for showing that Java Socket API can be used in Android.
        CoroutineScope(Dispatchers.IO).launch {
            val sock = SocketFactory.getDefault().createSocket(serverAddress, 80)
            //val sock = Socket("naver.com", 80)
            val istream = sock.getInputStream()
            val ostream = sock.getOutputStream()
            ostream.write("GET /\r\n".toByteArray())
            ostream.flush()
            val r = istream.readBytes()
            withContext(Dispatchers.Main) {
                output.text = r.decodeToString()
            }
            sock.close()
        }
    }

    private fun httpLib() {
        output.text = ""

        CoroutineScope(Dispatchers.IO).launch {
            val conn = URL("https://$serverAddress").openConnection() as HttpsURLConnection
            val istream = conn.inputStream
            val r = istream.readBytes()
            withContext(Dispatchers.Main) {
                output.text = r.decodeToString()
            }
            conn.disconnect()
        }
        // https://developer.android.com/reference/java/net/HttpURLConnection
    }

    interface RestApi {
        @GET("/")
        fun getRoot(): Call<String>

        @GET("/")
        suspend fun getRoot2(): String
    }

    private fun retrofitWithCoroutine() {
        output.text = ""

        val retrofit = Retrofit.Builder()
            .baseUrl("https://$serverAddress")
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        val api = retrofit.create(RestApi::class.java)
        var result : String

        CoroutineScope(Dispatchers.IO).launch {
            try {
                result = api.getRoot2()
            } catch (e: Exception) {
                result = "Failed to connect $serverAddress"
            }
            withContext(Dispatchers.Main) {
                output.text = result
            }
        }

    }

    private fun retrofit() {
        output.text = ""

        val retrofit = Retrofit.Builder()
            .baseUrl("https://$serverAddress")
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        val api = retrofit.create(RestApi::class.java)

        api.getRoot().enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: retrofit2.Response<String>) {
                output.text = response.body()
            }
            override fun onFailure(call: Call<String>, t: Throwable) {
                output.text = "Failed to connect $serverAddress"
            }
        })
    }

    private fun volley() {
        output.text = ""

        val queue = Volley.newRequestQueue(this)
        val url = "https://$serverAddress"

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response -> output.text = response },
            { output.text = "Failed to connect $serverAddress" })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            when (intent?.action) {
                DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                    val filePath = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "testdownload.png")
                    println("Download Completed! $filePath ${filePath.length()}")
                }
                else -> {
                    // DownloadManager.ACTION_NOTIFICATION_CLICKED
                    // DownloadManager.ACTION_VIEW_DOWNLOADS
                    println(intent?.action)
                }
            }
        }

    }

    private fun downloadManager() {
        val downloadURL = Uri.parse("https://www.hansung.ac.kr/sites/hansung/images/common/logo.png")

        val iFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(receiver, iFilter)  // need to register/unregister appropriately by the lifecycle of Activity.
        val dManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(downloadURL).apply {
            setTitle("Download")
            setDescription("Downloading a File")
            // setRequiresDeviceIdle(true)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(baseContext, Environment.DIRECTORY_DOWNLOADS, "testdownload.png")
            // setAllowedOverMetered(false)
        }
        val dID = dManager.enqueue(request)
        // you can use the dID for removing/deleting the download. dManager.remove(dID)
    }

    private fun openDownload() {
        val filePath = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "testdownload.png")
        val contentUri: Uri =
            getUriForFile(this, "com.example.internet_ex.file_provider", filePath)
        // we need <provider> in AndroidManifest.xml for the getUriForFile
        println(contentUri)
        val i = Intent().apply {
            action = Intent.ACTION_VIEW
            data = contentUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(i)
    }
}