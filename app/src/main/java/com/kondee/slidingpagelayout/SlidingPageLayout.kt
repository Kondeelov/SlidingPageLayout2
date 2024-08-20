package com.kondee.slidingpagelayout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.customview.widget.ViewDragHelper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SlidingPageLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewDragHelper =
        ViewDragHelper.create(this, 1.0f, SlidingPageViewDragHelperCallback())

    private var viewFocused: SlidingItemLayout? = null
    private var viewUnFocused: SlidingItemLayout? = null

    private var viewTemp: SlidingItemLayout? = null

    private var isAnimating: Boolean = false

    private val scaleMinimumFlingVelocity = 3500

    override fun onFinishInflate() {
        super.onFinishInflate()

        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        viewFocused = SlidingItemLayout(context).apply {
            id = View.generateViewId()
            layoutParams = lp
            isClickable = true
            setBackgroundColor(Color.parseColor("#5500FF00"))
        }

        viewUnFocused = SlidingItemLayout(context).apply {
            id = View.generateViewId()
            layoutParams = lp
            isClickable = true
            setBackgroundColor(Color.parseColor("#55FF0000"))
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        if (changed) {
            viewUnFocused?.let {
                it.offsetLeftAndRight(0)
                it.offsetTopAndBottom(height)
            }
        }
        Log.i("Kondee", "changed: $changed, top: $t")
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {

        if (event.action == MotionEvent.ACTION_DOWN) {
            startY = event.rawY
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            if (mState == State.EXPAND) {
                val delta = event.rawY - startY
                if (delta < 0) {
                    mAdapter?.getCurrentItem()?.view?.let {
                        (it as ViewGroup).children.forEach { view ->
                            if (view.canScrollVertically(-1)) {
                                return false
                            }
                        }
                    }
                } else {
                    mAdapter?.getCurrentItem()?.view?.let {
                        (it as ViewGroup).children.forEach { view ->
                            if (view.canScrollVertically(1)) {
                                return false
                            }
                        }
                    }
                }
            }
        }

        if (isAnimating || viewDragHelper.shouldInterceptTouchEvent(event)) {
            return true
        }
        return super.onInterceptTouchEvent(event)
    }

    private var startY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isAnimating) {
            return true
        }

        val pointerId: Int = event.getPointerId(0)

        if (event.action == MotionEvent.ACTION_MOVE) {
            val delta = event.rawY - startY
            when (mState) {
                State.EXPAND -> {

                }
                State.COLLAPSED_PREVIOUS -> {
                    if (delta > 0) {
                        viewTemp = viewFocused
                        viewFocused = viewUnFocused
                        viewUnFocused = viewTemp
                        viewTemp = null

                        viewUnFocused?.offsetTopAndBottom(height)

                        mState = State.COLLAPSED_NEXT
                    } else {

                    }
                }
                State.COLLAPSED_NEXT -> {
                    if (delta < 0) {
                        viewTemp = viewFocused
                        viewFocused = viewUnFocused
                        viewUnFocused = viewTemp
                        viewTemp = null

                        viewUnFocused?.offsetTopAndBottom(-height)

                        mState = State.COLLAPSED_PREVIOUS
                    }
                }
                State.END -> {

                }
            }
        }

        viewFocused?.let {
            viewDragHelper.captureChildView(it, pointerId)
        }

        viewDragHelper.processTouchEvent(event)

        return true
    }

    override fun computeScroll() {
        super.computeScroll()

        /**
         * Check if the animation has finished.
         * If not then continue the animation.
         */
        if (viewDragHelper.continueSettling(true)) {
            isAnimating = true
            postInvalidateOnAnimation()
        } else {

            /**
             * When scroll animation ended.
             * Set the new position of the unfocused view to top or bottom according to the state.
             */
            if (mState == State.COLLAPSED_PREVIOUS) {
                var focusedPosition = 0
                viewFocused?.let {
                    val offsetY = (-it.top) + height
                    it.offsetTopAndBottom(offsetY)
                    focusedPosition = mAdapter?.getViewPosition(it) ?: 0
                }
                val unfocusedPosition = focusedPosition - 1
                viewUnFocused?.let {
                    if (unfocusedPosition != (mAdapter?.getViewPosition(it) ?: 0)) {
                        mAdapter?.replaceFragment(unfocusedPosition, it)
                        val offsetY = (-it.top) - height
                        it.offsetTopAndBottom(offsetY)
                    }
                }
            } else if (mState == State.COLLAPSED_NEXT) {
                var focusedPosition = 0
                viewFocused?.let {
                    val offsetY = (-it.top) - height
                    it.offsetTopAndBottom(offsetY)
                    focusedPosition = mAdapter?.getViewPosition(it) ?: 0
                }
                val unfocusedPosition = focusedPosition + 1
                viewUnFocused?.let {
                    if (unfocusedPosition != (mAdapter?.getViewPosition(it) ?: 0)) {
                        mAdapter?.replaceFragment(unfocusedPosition, it)
                        val offsetY = (-it.top) + height
                        it.offsetTopAndBottom(offsetY)
                    }
                }
            }

            isAnimating = false
        }
    }

    inner class SlidingItemLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
    ) : FrameLayout(context, attrs) {

        private var offsetX = 0
        private var offsetY = 0

        /**
         * Save offsetY
         */
        override fun offsetTopAndBottom(offset: Int) {
            super.offsetTopAndBottom(offset)
            offsetY = offset
        }

        /**
         * Save offsetX
         */
        override fun offsetLeftAndRight(offset: Int) {
            super.offsetLeftAndRight(offset)
            offsetX = offset
        }

        /**
         * Restore offset whenever SlidingItemLayout onLayout gets called and changed flag is true.
         */
        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            if (changed) {
                offsetLeftAndRight(offsetX)
                offsetTopAndBottom(offsetY)
            }
        }
    }

    inner class SlidingPageViewDragHelperCallback : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return true
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return height
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            if ((mAdapter?.currentPosition ?: 0) == 0 && top > 0) {
                return 0
            }
            return min(max(top, -height), height)
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)

            viewFocused?.let {

                when (mState) {
                    State.EXPAND -> {
                        if (abs(yvel) > scaleMinimumFlingVelocity) {
                            if (yvel > 0) {
                                viewDragHelper.smoothSlideViewTo(it, 0, height)
                                mAdapter?.pauseFragment(mAdapter?.currentPosition ?: 0)
                                mAdapter?.currentPosition = -1
                                mState = State.COLLAPSED_PREVIOUS
                            } else {
                                viewDragHelper.smoothSlideViewTo(it, 0, -height)
                                mAdapter?.pauseFragment(mAdapter?.currentPosition ?: 0)
                                mAdapter?.currentPosition = -1
                                mState = State.COLLAPSED_NEXT
                            }
                        } else {
                            viewDragHelper.smoothSlideViewTo(it, 0, 0)
                        }
                    }
                    State.COLLAPSED_NEXT -> {
                        if (abs(yvel) > scaleMinimumFlingVelocity) {
                            if (yvel > 0) {
                                viewDragHelper.smoothSlideViewTo(it, 0, 0)
                                mAdapter?.currentPosition = mAdapter?.getViewPosition(it) ?: 0
                                mAdapter?.resumeFragment(mAdapter?.currentPosition ?: 0)
                                mState = State.EXPAND
                            } else {
                                viewDragHelper.smoothSlideViewTo(it, 0, -height)
                            }
                        } else {
                            viewDragHelper.smoothSlideViewTo(it, 0, -height)
                        }
                    }
                    State.COLLAPSED_PREVIOUS -> {
                        if (abs(yvel) > scaleMinimumFlingVelocity) {
                            if (yvel > 0) {
                                viewDragHelper.smoothSlideViewTo(it, 0, height)
                            } else {
                                viewDragHelper.smoothSlideViewTo(it, 0, 0)
                                mAdapter?.currentPosition = mAdapter?.getViewPosition(it) ?: 0
                                mAdapter?.resumeFragment(mAdapter?.currentPosition ?: 0)
                                mState = State.EXPAND
                            }
                        } else {
                            viewDragHelper.smoothSlideViewTo(it, 0, height)
                        }
                    }
                    State.END -> {

                    }
                }
            }

            isAnimating = true

            postInvalidateOnAnimation()
        }
    }

    private var mState = State.EXPAND

    enum class State {
        EXPAND,
        COLLAPSED_PREVIOUS,
        COLLAPSED_NEXT,
        END
    }

    private var mAdapter: Adapter? = null

    fun setAdapter(adapter: Adapter) {

        mAdapter = adapter

        val itemCount = adapter.getItemCount()
        if (itemCount >= 2) {
            addView(viewUnFocused)
        }
        if (itemCount >= 1) {
            addView(viewFocused)
            mAdapter?.replaceFragment(0, viewFocused, Lifecycle.State.RESUMED)
        }
    }

//    private fun setupAdapterInternal() {
//        if (mState == State.EXPAND) {
//            mAdapter?.resumeFragment(0)
//        }
//    }

    abstract class Adapter(private val fm: FragmentManager) {

        var currentPosition: Int = 0

        open fun getItemCount(): Int {
            return 0
        }

        abstract fun getItem(position: Int): Fragment

        fun replaceFragment(
            position: Int,
            view: View?,
            maxLifecycle: Lifecycle.State = Lifecycle.State.CREATED
        ) {
//            Modify this function to replace fragment with custom maxLifecycle for each fragments.
            view?.let {
                val fragment = getItem(position)
                fm.beginTransaction()
                    .replace(it.id, fragment, "fragment_$position")
                    .setMaxLifecycle(fragment, maxLifecycle)
                    .commit()
            }
        }

        fun resumeFragment(position: Int) {
            getFragmentFromPosition(position)?.let { fragment ->
                fm.beginTransaction()
                    .setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
                    .commit()
            }
        }

        fun pauseFragment(position: Int) {
            getFragmentFromPosition(position)?.let { fragment ->
                fm.beginTransaction()
                    .setMaxLifecycle(fragment, Lifecycle.State.STARTED)
                    .commit()
            }
        }

        fun getFragmentFromPosition(position: Int): Fragment? {
            return fm.findFragmentByTag("fragment_$position")
        }


        fun getCurrentItem(): Fragment? {
            return fm.findFragmentByTag("fragment_$currentPosition")
        }

        fun getViewPosition(view: View): Int {
            val fragment = fm.findFragmentById(view.id)
            return fragment?.tag?.replace("fragment_", "")?.toIntOrNull() ?: 0
        }
    }
}