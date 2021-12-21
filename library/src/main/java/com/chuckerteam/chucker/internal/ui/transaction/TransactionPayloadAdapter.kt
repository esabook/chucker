package com.chuckerteam.chucker.internal.ui.transaction

import android.annotation.SuppressLint
import android.gesture.GestureOverlayView.ORIENTATION_HORIZONTAL
import android.gesture.GestureOverlayView.ORIENTATION_VERTICAL
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.*
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.chuckerteam.chucker.R
import com.chuckerteam.chucker.databinding.ChuckerTransactionItemBodyLineBinding
import com.chuckerteam.chucker.databinding.ChuckerTransactionItemHeadersBinding
import com.chuckerteam.chucker.databinding.ChuckerTransactionItemImageBinding
import com.chuckerteam.chucker.internal.support.ChessboardDrawable
import com.esabook.webviewcode.Codeview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * Adapter responsible of showing the content of the Transaction Request/Response body.
 * We're using a [RecyclerView] to show the content of the body line by line to do not affect
 * performances when loading big payloads.
 */
internal class TransactionBodyAdapter : RecyclerView.Adapter<TransactionPayloadViewHolder>() {

    private val items = arrayListOf<TransactionPayloadItem>()

    fun setItems(bodyItems: List<TransactionPayloadItem>) {
        val previousItemCount = items.size
        items.clear()
        items.addAll(bodyItems)
        notifyItemRangeRemoved(0, previousItemCount)
        notifyItemRangeInserted(0, items.size)
    }

    override fun onBindViewHolder(holder: TransactionPayloadViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TransactionPayloadViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADERS -> {
                val headersItemBinding =
                    ChuckerTransactionItemHeadersBinding.inflate(inflater, parent, false)
                TransactionPayloadViewHolder.HeaderViewHolder(headersItemBinding)
            }
            TYPE_BODY_LINE -> {
                val bodyItemBinding =
                    ChuckerTransactionItemBodyLineBinding.inflate(inflater, parent, false)
                TransactionPayloadViewHolder.BodyLineViewHolder(bodyItemBinding)
            }
            else -> {
                val imageItemBinding =
                    ChuckerTransactionItemImageBinding.inflate(inflater, parent, false)
                TransactionPayloadViewHolder.ImageViewHolder(imageItemBinding)
            }
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TransactionPayloadItem.HeaderItem -> TYPE_HEADERS
            is TransactionPayloadItem.BodyLineItem -> TYPE_BODY_LINE
            is TransactionPayloadItem.ImageItem -> TYPE_IMAGE
        }
    }

    internal fun highlightQueryWithColors(
        newText: String,
        backgroundColor: Int,
        foregroundColor: Int
    ) {
        items.filterIsInstance<TransactionPayloadItem.BodyLineItem>()
            .withIndex()
            .forEach { (index, item) ->
                item.searchQuery.postValue(newText)

//                if (item.line.contains(newText, ignoreCase = true)) {
//                    item.line.clearSpans()
//                    item.line = item.line.toString()
//                        .highlightWithDefinedColors(newText, backgroundColor, foregroundColor)
//                    notifyItemChanged(index + 1)
//                } else {
//                    // Let's clear the spans if we haven't found the query string.
//                    val spans = item.line.getSpans(0, item.line.length - 1, Any::class.java)
//                    if (spans.isNotEmpty()) {
//                        item.line.clearSpans()
//                        notifyItemChanged(index + 1)
//                    }
//                }
            }
    }

    internal fun resetHighlight() {
        items.filterIsInstance<TransactionPayloadItem.BodyLineItem>()
            .withIndex()
            .forEach { (index, item) ->
                item.searchQuery.postValue(null)

//                val spans = item.line.getSpans(0, item.line.length - 1, Any::class.java)
//                if (spans.isNotEmpty()) {
//                    item.line.clearSpans()
//                    notifyItemChanged(index + 1)
//                }
            }
    }

    companion object {
        private const val TYPE_HEADERS = 1
        private const val TYPE_BODY_LINE = 2
        private const val TYPE_IMAGE = 3
    }
}

internal sealed class TransactionPayloadViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(item: TransactionPayloadItem)

    internal class HeaderViewHolder(
        private val headerBinding: ChuckerTransactionItemHeadersBinding
    ) : TransactionPayloadViewHolder(headerBinding.root) {
        override fun bind(item: TransactionPayloadItem) {
            if (item is TransactionPayloadItem.HeaderItem) {
                headerBinding.responseHeaders.text = item.headers
            }
        }
    }

    internal class BodyLineViewHolder(
        private val bodyBinding: ChuckerTransactionItemBodyLineBinding
    ) : TransactionPayloadViewHolder(bodyBinding.root) {

        override fun bind(item: TransactionPayloadItem) {
            bodyBinding.btAutowrap.isVisible = false
            bodyBinding.btSearchBack.isVisible = false
            bodyBinding.btSearchNext.isVisible = false

            if (item is TransactionPayloadItem.BodyLineItem) {
                if (item.line.length > 50) {
                    initAutoWrapAction(item)
                }

                handleWebGesture()
                initSearchListener(item)
                renderBody(item, bodyBinding.btAutowrap.isChecked)
            }
        }

        private fun initAutoWrapAction(item: TransactionPayloadItem.BodyLineItem) {
            bodyBinding.btAutowrap.isVisible = true
            bodyBinding.btAutowrap.setOnCheckedChangeListener { _, isChecked ->
                renderBody(item, isChecked)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun handleWebGesture() {
            var initialX = 0f
            var initialY = 0f
            val touchSlop = ViewConfiguration.get(itemView.context).scaledTouchSlop

            bodyBinding.webview.setOnTouchListener { v, e ->
                if (e.action == MotionEvent.ACTION_DOWN) {
                    initialX = e.x
                    initialY = e.y
                    blockParentTouch(true, true)
                } else if (e.action == MotionEvent.ACTION_MOVE) {
                    val dx = e.x - initialX
                    val dy = e.y - initialY

                    val scaledDx = dx.absoluteValue.times(10)
                    val scaledDy = dy.absoluteValue.times(10)

                    val canScollH = scaledDx > touchSlop && v.canScroll(ORIENTATION_HORIZONTAL, dx)
                    val canScollV = scaledDy > touchSlop && v.canScroll(ORIENTATION_VERTICAL, dy)
                    blockParentTouch(canScollV, canScollH)

                }
                return@setOnTouchListener false
            }
        }

        private fun observeSearchQuery(q: String?) {
            bodyBinding.run {
                val isEmpty = q.isNullOrEmpty()
                btSearchBack.isGone = isEmpty
                btSearchNext.isGone = isEmpty
                totalSize.isGone = isEmpty

                if (isEmpty)
                    webview.clearMatches()
                else
                    webview.findAllAsync(q!!)
            }
        }


        private fun initSearchListener(item: TransactionPayloadItem.BodyLineItem) {
            itemView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View?) {
                    item.searchQuery.observeForever(this@BodyLineViewHolder::observeSearchQuery)
                }

                override fun onViewDetachedFromWindow(v: View?) {
                    item.searchQuery.removeObserver(this@BodyLineViewHolder::observeSearchQuery)
                }
            })

            bodyBinding.run {
                btSearchBack.setOnClickListener {
                    webview.findNext(false)
                }
                btSearchNext.setOnClickListener {
                    webview.findNext(true)
                }

                webview.setFindListener { i, t, _ ->
                    val currentSelect = if (t > 0) i + 1 else i
                    "${currentSelect}/${t}".let { totalSize.text = it }
                }
            }

            item.searchQuery.observeForever(this@BodyLineViewHolder::observeSearchQuery)
        }

        private fun View.canScroll(orientation: Int, delta: Float): Boolean {
            val direction = -delta.sign.toInt()
            return when (orientation) {
                0 -> canScrollHorizontally(direction)
                1 -> canScrollVertically(direction)
                else -> throw IllegalArgumentException()
            }
        }

        private fun blockParentTouch(blockV: Boolean, blockH: Boolean) {
            // fast route to disable ViewPager when scroll X
            bodyBinding.webview.parent.parent.parent.parent?.requestDisallowInterceptTouchEvent(
                blockH
            )
            // fast route to disable RecycleView when scroll Y
            bodyBinding.webview.parent.parent?.requestDisallowInterceptTouchEvent(blockV)
        }

        private fun renderBody(
            item: TransactionPayloadItem.BodyLineItem,
            autowrap: Boolean = true
        ) {
            MainScope().launch {
                Codeview()
                    .withText(item.header ?: SpannableStringBuilder())
                    .withCode(item.line)
                    .setAutoWrap(autowrap)
                    .into(bodyBinding.webview)
            }
        }
    }

    internal class ImageViewHolder(
        private val imageBinding: ChuckerTransactionItemImageBinding
    ) : TransactionPayloadViewHolder(imageBinding.root) {

        override fun bind(item: TransactionPayloadItem) {
            if (item is TransactionPayloadItem.ImageItem) {
                imageBinding.binaryData.setImageBitmap(item.image)
                imageBinding.root.background = createContrastingBackground(item.luminance)
            }
        }

        private fun createContrastingBackground(luminance: Double?): Drawable? {
            if (luminance == null) return null

            return if (luminance < LUMINANCE_THRESHOLD) {
                ChessboardDrawable.createPattern(
                    itemView.context,
                    R.color.chucker_chessboard_even_square_light,
                    R.color.chucker_chessboard_odd_square_light,
                    R.dimen.chucker_half_grid
                )
            } else {
                ChessboardDrawable.createPattern(
                    itemView.context,
                    R.color.chucker_chessboard_even_square_dark,
                    R.color.chucker_chessboard_odd_square_dark,
                    R.dimen.chucker_half_grid
                )
            }
        }

        private companion object {
            const val LUMINANCE_THRESHOLD = 0.25
        }
    }
}

internal sealed class TransactionPayloadItem {
    internal class HeaderItem(val headers: Spanned) : TransactionPayloadItem()
    internal class BodyLineItem(
        var line: SpannableStringBuilder,
        var header: SpannableStringBuilder? = null,
        val searchQuery: MutableLiveData<String> = MutableLiveData()
    ) : TransactionPayloadItem()

    internal class ImageItem(val image: Bitmap, val luminance: Double?) : TransactionPayloadItem()
}
