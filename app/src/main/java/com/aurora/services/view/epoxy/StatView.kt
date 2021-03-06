package com.aurora.services.view.epoxy

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import com.aurora.services.R
import com.aurora.services.data.model.Stat
import com.aurora.services.data.utils.Util.millisToDayOrTime
import com.aurora.services.databinding.ViewStatBinding

@ModelView(
    autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseView::class
)

class StatView : RelativeLayout {

    private lateinit var B: ViewStatBinding

    constructor(context: Context?) : super(context) {
        init(context, null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context?, attrs: AttributeSet?) {
        val view = inflate(context, R.layout.view_stat, this)
        B = ViewStatBinding.bind(view)
    }

    @ModelProp
    fun app(stat: Stat) {
        B.line1.text = stat.installerPackageName
        B.line2.text = stat.packageName

        val extra = listOf(
            millisToDayOrTime(stat.timeStamp),
            if (stat.granted)
                context.getString(R.string.perm_granted)
            else
                context.getString(R.string.perm_not_granted),
            if (stat.install)
                context.getString(R.string.action_install)
            else
                context.getString(R.string.action_uninstall)
        ).joinToString(separator = " • ")

        B.line3.text = extra
    }

    @CallbackProp
    fun click(onClickListener: OnClickListener?) {
        B.root.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun longClick(onClickListener: OnLongClickListener?) {
        B.root.setOnLongClickListener(onClickListener)
    }

    @OnViewRecycled
    fun clear() {

    }
}