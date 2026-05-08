package com.ekenahmetfaruk.vigna_scan.ui.result

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ekenahmetfaruk.vigna_scan.R
import com.ekenahmetfaruk.vigna_scan.databinding.FragmentResultBinding
import com.ekenahmetfaruk.vigna_scan.ml.ModelResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val args: ResultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCapturedImage()
        bindModel1Result(args.model1Result)
        bindModel2Result(args.model2Result)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnRescan.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun loadCapturedImage() {
        try {
            val bitmap = BitmapFactory.decodeFile(args.imagePath)
            binding.ivCaptured.setImageBitmap(bitmap)
        } catch (e: Exception) {
            binding.ivCaptured.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun bindModel1Result(result: ModelResult) {
        with(binding) {
            tvDisease1.text = if (result.isReliable) {
                result.predictedClass
            } else {
                "Tanımlanamadı"
            }

            val confidencePercent = (result.confidence * 100).toInt()
            tvConfidence1.text = "%$confidencePercent"
            progressConfidence1.progress = confidencePercent
            tvTime1.text = "${result.inferenceTimeMs} ms"

            // Kart rengini güven skoruna göre ayarla
            cardModel1.setCardBackgroundColor(
                requireContext().getColor(
                    if (result.isReliable) R.color.success_container
                    else R.color.error_container
                )
            )

            // Progress bar rengini güven skoruna göre ayarla
            progressConfidence1.progressTintList =
                android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(
                        if (result.isReliable) R.color.success
                        else R.color.error
                    )
                )

            tvConfidence1.setTextColor(
                requireContext().getColor(
                    if (result.isReliable) R.color.success
                    else R.color.error
                )
            )
        }
    }

    private fun bindModel2Result(result: ModelResult) {
        with(binding) {
            tvDisease2.text = if (result.isReliable) {
                result.predictedClass
            } else {
                "Tanımlanamadı"
            }

            val confidencePercent = (result.confidence * 100).toInt()
            tvConfidence2.text = "%$confidencePercent"
            progressConfidence2.progress = confidencePercent
            tvTime2.text = "${result.inferenceTimeMs} ms"

            cardModel2.setCardBackgroundColor(
                requireContext().getColor(
                    if (result.isReliable) R.color.success_container
                    else R.color.error_container
                )
            )

            progressConfidence2.progressTintList =
                android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(
                        if (result.isReliable) R.color.success
                        else R.color.error
                    )
                )

            tvConfidence2.setTextColor(
                requireContext().getColor(
                    if (result.isReliable) R.color.success
                    else R.color.error
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}