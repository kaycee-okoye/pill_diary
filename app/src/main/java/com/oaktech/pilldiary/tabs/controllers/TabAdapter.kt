package com.oaktech.pilldiary.tabs.controllers

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Class used to manage the tabs in the main activity
 *
 * @param fa activity in which the the tab view will be displayed
 * @param fragments the fragments to be displayed in
 * the tab view
 */
class TabAdapter(fa: FragmentActivity, private val fragments: List<Fragment>) :
    FragmentStateAdapter(fa) {
    /**
     * Method to return the fragment for the current tab index in MainActivity
     *
     * @param position current tab index
     *
     * @return appropriate Fragment to be displayed
     */
    override fun createFragment(position: Int): Fragment = fragments[position]

    /**
     * Method to get the number of tabs in the MainActivity
     *
     * @return total number of tabs in the MainActivity
     */
    override fun getItemCount(): Int = fragments.size
}