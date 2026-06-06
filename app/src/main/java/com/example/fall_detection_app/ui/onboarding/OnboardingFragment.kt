package com.example.fall_detection_app.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.fall_detection_app.R
import com.example.fall_detection_app.databinding.OnboardingBinding

class OnboardingFragment : Fragment() {

    private var _binding: OnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = OnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // set up ViewPager2 with the 3 slides
        val adapter = OnboardingAdapter(this)
        binding.viewPager.adapter = adapter

        // update dots when page changes
        binding.viewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
            }
        })

        // start on first slide with dot 1 active
        updateDots(0)

        // next button behaviour
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < 2) {
                // go to next slide
                binding.viewPager.currentItem = current + 1
            } else {
                // last slide — navigate to permissions screen
                findNavController().navigate(R.id.action_onboarding_to_permissions)
            }
        }
    }

    private fun updateDots(position: Int) {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index == position) R.drawable.dot_active
                else R.drawable.dot_inactive
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // avoid memory leaks
    }
}