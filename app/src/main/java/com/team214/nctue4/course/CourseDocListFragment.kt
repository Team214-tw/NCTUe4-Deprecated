package com.team214.nctue4.course

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.team214.nctue4.R
import com.team214.nctue4.connect.OldE3Connect
import com.team214.nctue4.connect.OldE3Interface
import com.team214.nctue4.model.DocGroupItem
import com.team214.nctue4.utility.DataStatus
import kotlinx.android.synthetic.main.fragment_course_doc.*
import kotlinx.android.synthetic.main.status_empty.*
import kotlinx.android.synthetic.main.status_error.*


class CourseDocListFragment : Fragment() {

    private lateinit var oldE3Service: OldE3Connect
    private var dataStatus = DataStatus.INIT

    override fun onStop() {
        super.onStop()
        oldE3Service.cancelPendingRequests()
        if (dataStatus == DataStatus.INIT) dataStatus = DataStatus.STOPPED
    }

    override fun onStart() {
        super.onStart()
        if (dataStatus == DataStatus.STOPPED) getData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_course_doc, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getData()
    }

    private fun getData() {
        error_request?.visibility = View.GONE
        progress_bar?.visibility = View.VISIBLE
//TODO NEWe3
        oldE3Service = (activity as CourseActivity).oldE3Service!!
        val courseId = arguments!!.getString("courseId")
        oldE3Service.getMaterialDocList(courseId, context!!) { status, response ->
            when (status) {
                OldE3Interface.Status.SUCCESS -> {
                    updateList(response!!)
                }
                else -> {
                    error_request?.visibility = View.VISIBLE
                    dataStatus = DataStatus.INIT
                    error_request_retry?.setOnClickListener { getData() }
                }
            }
            dataStatus = DataStatus.FINISHED
            progress_bar?.visibility = View.GONE
        }
    }

    private fun updateList(docGroupItems: ArrayList<DocGroupItem>) {
        if (docGroupItems.size == 0) {
            empty_request?.visibility = View.VISIBLE
        } else {
            course_doc_list_recycler_view?.layoutManager = LinearLayoutManager(context)
            course_doc_list_recycler_view?.adapter = CourseDocListAdapter(docGroupItems) {
                val dialog = CourseDocDialog()
                val bundle = Bundle()
                bundle.putString("documentId", it.documentId)
                bundle.putString("courseId", it.courseId)
                dialog.arguments = bundle
                dialog.show(fragmentManager, "TAG")
            }
            course_doc_list_recycler_view?.visibility = View.VISIBLE
        }
    }
}