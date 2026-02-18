package com.ekenahmetfaruk.vigna_scan

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ekenahmetfaruk.vigna_scan.ui.camera.CameraFragment

class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, CameraFragment.newInstance())
                .commitNow()
        }
    }
}