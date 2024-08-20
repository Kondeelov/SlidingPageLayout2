package com.kondee.slidingpagelayout

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

class CustomWebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : WebView(context, attrs) {

    override fun canScrollVertically(direction: Int): Boolean {
        return super.canScrollVertically(direction)
    }
}