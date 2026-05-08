package com.ekenahmetfaruk.vigna_scan

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ekenahmetfaruk.vigna_scan.databinding.ActivityCameraBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}