package com.example.mobilefinalproject.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mobilefinalproject.MainActivity
import com.example.mobilefinalproject.R
import com.example.mobilefinalproject.databinding.FragmentDashboardBinding
import com.example.mobilefinalproject.shell.AppSession

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        binding.openStrengthTraining.setOnClickListener {
            openSection(R.id.action_dashboard_to_strength)
        }
        binding.openCalorieTracking.setOnClickListener {
            openSection(R.id.action_dashboard_to_calories)
        }
        AppSession.account?.let {
            binding.signedInAccount.text = getString(R.string.dashboard_signed_in_as, it.email)
        }
        binding.signOutButton.setOnClickListener { (requireActivity() as MainActivity).signOut() }
        return binding.root
    }

    private fun openSection(action: Int) {
        val nav = findNavController()
        if (nav.currentDestination?.id == R.id.nav_dashboard) nav.navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
