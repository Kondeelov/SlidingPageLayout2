package com.kondee.slidingpagelayout

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class SlidingPageAdapter(fm: FragmentManager) : SlidingPageLayout.Adapter(fm) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Fragment {
        return NovelReaderFragment.newInstance(position)
    }
}