package com.example.mobilefinalproject.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mobilefinalproject.databinding.FragmentWorkoutLoggingBinding

class WorkoutLoggingFragment : Fragment() {

    private var _binding: FragmentWorkoutLoggingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(WorkoutLoggingViewModel::class.java)

        _binding = FragmentWorkoutLoggingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewModel.recentWorkouts.observe(viewLifecycleOwner) { recent ->
            binding.textRecentWorkouts.text = recent
        }

        binding.buttonLogWorkout.setOnClickListener {
            val exercise = binding.editExerciseName.text.toString()
            val sets = binding.editSets.text.toString()
            val reps = binding.editReps.text.toString()
            val weight = binding.editWeightUsed.text.toString()

            if (exercise.isNotEmpty() && sets.isNotEmpty() && reps.isNotEmpty() && weight.isNotEmpty()) {
                val logSummary = "Logged: $exercise - $sets sets x $reps reps @ $weight lbs"
                binding.textRecentWorkouts.text = logSummary
                Toast.makeText(requireContext(), "Workout Set Saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
