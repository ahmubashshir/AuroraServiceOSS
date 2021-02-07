package com.aurora.services.view.epoxy

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.RelativeLayout
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import com.aurora.services.R
import com.aurora.services.data.model.App
import com.aurora.services.databinding.ViewAppBinding

@ModelView(
    autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseView::class
)

class AppView : RelativeLayout {

    private lateinit var B: ViewAppBinding

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
        val view = inflate(context, R.layout.view_app, this)
        B = ViewAppBinding.bind(view)
    }

    @ModelProp
    fun app(app: App) {
        B.line1.text = app.displayName
        B.line2.text = app.packageName
        B.line3.text = ("${app.versionName}.${app.versionCode}")
        B.img.setImageDrawable(app.drawable)
    }

    @ModelProp
    fun markChecked(isChecked: Boolean) {
        B.checkbox.isChecked = isChecked
    }

    @CallbackProp
    fun checked(onCheckedChangeListener: CompoundButton.OnCheckedChangeListener?) {
        B.checkbox.setOnCheckedChangeListener(onCheckedChangeListener)
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