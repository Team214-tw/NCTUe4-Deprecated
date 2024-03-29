package com.team214.nctue4.course

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.team214.nctue4.R
import com.team214.nctue4.model.DocGroupItem
import kotlinx.android.synthetic.main.item_course_doc_group.view.*

class DocListAdapter(private val dataSet: ArrayList<DocGroupItem>,
                     private val itemClickListener: (DocGroupItem) -> Unit) :
        RecyclerView.Adapter<DocListAdapter.ViewHolder>() {

    class ViewHolder(private val view: View,
                     private val itemClickListener: (DocGroupItem) -> Unit) : RecyclerView.ViewHolder(view) {
        fun bind(doc: DocGroupItem) {
            view.doc_group_display_name.text = doc.displayName
            view.course_doc_type.text = doc.docType
            view.course_doc_group_list_item?.setOnClickListener {
                itemClickListener(doc)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): DocListAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_course_doc_group, parent, false)
        return ViewHolder(view, itemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])

    }

    override fun getItemCount() = dataSet.size
}