package com.example.fall_detection_app.ui.onboarding

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fall_detection_app.ui.onboarding.slides.AlwaysWatchingFragment
import com.example.fall_detection_app.ui.onboarding.slides.HowItWorksFragment
import com.example.fall_detection_app.ui.onboarding.slides.RequirementsFragment

class OnboardingAdapter(fragment: Fragment) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AlwaysWatchingFragment()
            1 -> HowItWorksFragment()
            2 -> RequirementsFragment()
            else -> AlwaysWatchingFragment()
        }
    }
}