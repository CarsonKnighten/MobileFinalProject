package com.example.mobilefinalproject.ui.weightloss

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mobilefinalproject.databinding.FragmentWeightLossBinding

class WeightLossFragment : Fragment() {

    private var _binding: FragmentWeightLossBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val weightLossViewModel =
            ViewModelProvider(this).get(WeightLossViewModel::class.java)

        _binding = FragmentWeightLossBinding.inflate(inflater, container, false)
        val root: View = binding.root

        weightLossViewModel.analysisText.observe(viewLifecycleOwner) { analysis ->
            binding.textAnalysisSummary.text = analysis
        }

        binding.buttonSaveWeightLog.setOnClickListener {
            val intake = binding.editCalorieIntake.text.toString()
            val weight = binding.editCurrentWeight.text.toString()
            if (intake.isNotEmpty() && weight.isNotEmpty()) {
                Toast.makeText(requireContext(), "Weight and Calorie Entry Saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please enter both values", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
