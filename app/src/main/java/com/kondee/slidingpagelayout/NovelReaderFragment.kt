package com.kondee.slidingpagelayout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kondee.slidingpagelayout.databinding.FragmentNovelReaderBinding

class NovelReaderFragment : Fragment() {

    private var binding: FragmentNovelReaderBinding? = null

    private val position by lazy {
        arguments?.getInt(KEY_POSITION) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentNovelReaderBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding?.webView?.loadUrl("https://www.tunwalai.com")
        binding?.textViewPosition?.setText("Position #$position")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {

        private const val KEY_POSITION = "position"

        fun newInstance(position: Int): Fragment {
            val fragment = NovelReaderFragment()
            val args = Bundle()
            args.putInt(KEY_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }
}