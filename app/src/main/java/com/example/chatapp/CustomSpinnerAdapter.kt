package com.example.chatapp

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat

class CustomSpinnerAdapter(context: Context, countries: Array<String>) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, countries) {

    override fun getCount(): Int {
        return super.getCount() + 1
    }

    override fun getItem(position: Int): String? {
        return if (position == 0) {
            "Выберите свою страну"
        } else {
            super.getItem(position - 1)
        }
    }

    override fun getItemId(position: Int): Long {
        return if (position == 0) {
            -1
        } else {
            super.getItemId(position - 1)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        if (position == 0) {
            (view as TextView).setTextColor(ContextCompat.getColor(context, R.color.red))
        }
        return view
    }
}