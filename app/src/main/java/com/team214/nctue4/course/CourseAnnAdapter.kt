package com.team214.nctue4.course

import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.team214.nctue4.R
import com.team214.nctue4.model.AnnItem
import com.team214.nctue4.utility.htmlCleaner
import kotlinx.android.synthetic.main.item_course_announcement.view.*
import java.text.SimpleDateFormat
import java.util.*

class CourseAnnAdapter(private val dataSet: ArrayList<AnnItem>,
                       private val itemClickListener: (AnnItem) -> Unit) :
        RecyclerView.Adapter<CourseAnnAdapter.ViewHolder>() {

    class ViewHolder(val view: View,
                     private val itemClickListener: (AnnItem) -> Unit) : RecyclerView.ViewHolder(view) {
        fun bind(ann: AnnItem) {
            view.announcement_caption.text = ann.caption
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
                view.announcement_content.text = Html.fromHtml(htmlCleaner(ann.content))
                        .replace("\\s+".toRegex(), " ")
            } else {
                view.announcement_content.text = Html.fromHtml(htmlCleaner(ann.content),
                        Html.FROM_HTML_MODE_COMPACT).replace("\\s+".toRegex(), " ")
            }
            val sdf = SimpleDateFormat("MM/dd", Locale.TAIWAN)
            view.announcement_beginDate.text = sdf.format(ann.beginDate)
            view.setOnClickListener {
                itemClickListener(ann)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_course_announcement, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount() = dataSet.size
}