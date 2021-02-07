package com.aurora.services.view.epoxy

import android.view.View
import com.airbnb.epoxy.EpoxyModel

abstract class BaseView<T : View?> : EpoxyModel<T>() {

    override fun bind(view: T) {
        super.bind(view)
    }

    override fun unbind(view: T) {
        super.unbind(view)
    }
}