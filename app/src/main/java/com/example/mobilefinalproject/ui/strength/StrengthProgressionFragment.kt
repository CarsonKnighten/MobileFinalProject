package com.example.mobilefinalproject.ui.strength

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mobilefinalproject.databinding.FragmentStrengthProgressionBinding

class StrengthProgressionFragment : Fragment() {

    private var _binding: FragmentStrengthProgressionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(StrengthProgressionViewModel::class.java)

        _binding = FragmentStrengthProgressionBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewModel.projectionText.observe(viewLifecycleOwner) { projection ->
            binding.textProjectionSummary.text = projection
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
