package com.example.lehydrosys

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2 // Number of pages

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DisplayFragment() // First Page (Sensor Data)
            1 -> GraphFragment()   // Second Page (Graphs)
            else -> DisplayFragment()
        }
    }
}
