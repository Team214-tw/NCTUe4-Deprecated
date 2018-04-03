package com.team214.nctue4.connect

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import android.util.Log
import com.team214.nctue4.R
import com.team214.nctue4.model.*
import com.team214.nctue4.utility.E3Type
import com.team214.nctue4.utility.MemberType
import com.team214.nctue4.utility.forceGetJsonArray
import com.team214.nctue4.utility.htmlCleaner
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import kotlinx.android.parcel.Parcelize
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


@Parcelize
@SuppressLint("ParcelCreator")
class OldE3Connect(private var studentId: String = "",
                   private var studentPassword: String = "",
                   private var loginTicket: String = "",
                   private var accountId: String = "") : OldE3Interface, Parcelable {

    companion object {
        private val tag = OldE3Connect::class.java.simpleName
        private const val loginPath = "/Login"
    }

    private val client = OkHttpClient().newBuilder().followRedirects(false)
            .followSslRedirects(false).build()

    private fun post(path: String, params: HashMap<String, String>,
                     secondTry: Boolean = false,
                     completionHandler: (status: OldE3Interface.Status,
                                         response: JSONObject?) -> Unit) {
        params["loginTicket"] = loginTicket
        params["studentId"] = accountId
        params["accountId"] = accountId
        if (loginTicket == "" && path != loginPath) {
            getLoginTicket { status, _ ->
                if (status == OldE3Interface.Status.SUCCESS) {
                    post(path, params, secondTry, completionHandler)
                } else {
                    completionHandler(status, null)
                }
            }
        } else {
            val url = "http://e3.nctu.edu.tw/mService/Service.asmx$path"
            Log.d("OldE3URL", url)
            val formBodyBuilder = FormBody.Builder()
            params.forEach { entry -> formBodyBuilder.add(entry.key, entry.value) }
            val formBody = formBodyBuilder.build()

            val request = okhttp3.Request.Builder().url(url).post(formBody).build()

            val call = client.newCall(request)

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!secondTry && path != loginPath) {
                        getLoginTicket { _, _ ->
                            post(path, params, true, completionHandler)
                        }
                    } else completionHandler(OldE3Interface.Status.SERVICE_ERROR, null)
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    val xmlToJson = (XmlToJson.Builder(response.body().string()).build()).toJson()
                    completionHandler(OldE3Interface.Status.SUCCESS, xmlToJson)
                }
            })
        }
    }


    override fun getLoginTicket(completionHandler: (status: OldE3Interface.Status,
                                                    response: Pair<String, String>?) -> Unit) {
        post(loginPath, hashMapOf(
                "account" to studentId,
                "password" to studentPassword
        )) { status, response ->
            if (status == OldE3Interface.Status.SUCCESS) {
                val accountData = response!!.getJSONObject("AccountData")
                if (accountData.has("LoginTicket")) {
                    val studentName = accountData.getString("Name")
                    val studentEmail = accountData.getString("EMail")
                    loginTicket = accountData.getString("LoginTicket")
                    accountId = accountData.getString("AccountId")
                    completionHandler(OldE3Interface.Status.SUCCESS, Pair(studentName, studentEmail))
                } else {
                    completionHandler(OldE3Interface.Status.WRONG_CREDENTIALS, null)
                }
            } else {
                completionHandler(status, null)
            }
        }
    }

    override fun getCourseList(completionHandler: (status: OldE3Interface.Status,
                                                   response: ArrayList<CourseItem>?) -> Unit) {
        post("/GetCourseList", hashMapOf(
                "role" to "stu"
        )) { status, response ->
            if (status == OldE3Interface.Status.SUCCESS) {
                val data = response!!.getJSONObject("ArrayOfCourseData")
                        .forceGetJsonArray("CourseData")
                val courseItems = ArrayList<CourseItem>()
                (0 until data.length()).map { data.get(it) as JSONObject }
                        .forEach {
                            courseItems.add(CourseItem(it.getString("CourseNo"),
                                    it.getString("CourseName"),
                                    it.getString("TeacherName"),
                                    it.getString("CourseId"),
                                    E3Type.OLD))
                        }
                completionHandler(status, courseItems)
            } else {
                completionHandler(status, null)
            }
        }
    }

    override fun getAnnouncementListLogin(count: Int, completionHandler: (status: OldE3Interface.Status,
                                                                          response: ArrayList<AnnItem>?) -> Unit) {
        post("/GetAnnouncementList_LoginByCountWithAttach", hashMapOf(
                "ShowCount" to count.toString()
        )) { status, response ->
            if (status == OldE3Interface.Status.SUCCESS) {
                val annData = response!!.getJSONObject("ArrayOfBulletinData")
                        .forceGetJsonArray("BulletinData")
                val annItems = ArrayList<AnnItem>()
                val df = SimpleDateFormat("yyyy/M/d", Locale.US)
                (0 until annData.length()).map { annData.get(it) as JSONObject }
                        .forEach {
                            val attachItemList = ArrayList<AttachItem>()
                            val attachNames = it.forceGetJsonArray("AttachFileName")
                            val attachUrls = it.forceGetJsonArray("AttachFileURL")
                            val attachFileSizes = it.forceGetJsonArray("AttachFileFileSize")
                            if ((attachNames.get(0) as JSONObject).getString("string") != "") {
                                (0 until attachNames.length()).map {
                                    AttachItem(
                                            (attachNames.get(it) as JSONObject).getString("string").dropLast(1),
                                            (attachFileSizes.get(it) as JSONObject).getString("string").dropLast(1),
                                            (attachUrls.get(it) as JSONObject).getString("string").dropLast(1))
                                }.forEach {
                                    attachItemList.add(it)
                                }
                            }
                            annItems.add(AnnItem(
                                    it.getString("BulletinId"),
                                    it.getString("CourseName"),
                                    it.getString("Caption"),
                                    it.getString("Content"),
                                    df.parse(it.getString("BeginDate")),
                                    df.parse(it.getString("EndDate")),
                                    it.getString("CourseId"),
                                    E3Type.OLD,
                                    attachItemList
                            ))
                        }
                completionHandler(status, annItems)
            } else {
                completionHandler(status, null)
            }
        }
    }

    override fun getCourseAnn(courseId: String, courseName: String,
                              completionHandler: (status: OldE3Interface.Status,
                                                  response: ArrayList<AnnItem>?) -> Unit) {
        post("/GetAnnouncementListWithAttach", hashMapOf(
                "courseId" to courseId,
                "bulType" to "1"
        )) { status, response ->
            if (status == OldE3Interface.Status.SUCCESS) {
                val arrayOfBulletinData = response!!.getJSONObject("ArrayOfBulletinData")
                val data = arrayOfBulletinData.forceGetJsonArray("BulletinData")
                val annItems = ArrayList<AnnItem>()
                val df = SimpleDateFormat("yyyy/M/d", Locale.US)
                (0 until data.length()).map { data.get(it) as JSONObject }
                        .forEach {
                            val attachItemList = ArrayList<AttachItem>()
                            val attachNames = it.forceGetJsonArray("AttachFileName")
                            val attachUrls = it.forceGetJsonArray("AttachFileURL")
                            val attachFileSizes = it.forceGetJsonArray("AttachFileFileSize")
                            if ((attachNames.get(0) as JSONObject).getString("string") != "") {
                                (0 until attachNames.length()).map {
                                    AttachItem(
                                            (attachNames.get(it) as JSONObject).getString("string").dropLast(1),
                                            (attachFileSizes.get(it) as JSONObject).getString("string").dropLast(1),
                                            (attachUrls.get(it) as JSONObject).getString("string").dropLast(1))
                                }.forEach {
                                    attachItemList.add(it)
                                }
                            }
                            annItems.add(AnnItem(
                                    it.getString("BulletinId"),
                                    courseName,
                                    it.getString("Caption"),
                                    htmlCleaner(it.getString("Content")),
                                    df.parse(it.getString("BeginDate")),
                                    df.parse(it.getString("EndDate")),
                                    it.getString("CourseId"),
                                    E3Type.OLD,
                                    attachItemList
                            ))
                        }
                completionHandler(status, annItems)
            } else {
                completionHandler(status, null)
            }
        }
    }


    private lateinit var getMaterialDocListStatus: Array<Boolean>
    private var docGroupItems: ArrayList<DocGroupItem>? = null
    override fun getMaterialDocList(courseId: String, context: Context,
                                    completionHandler: (status: OldE3Interface.Status,
                                                        response: ArrayList<DocGroupItem>?) -> Unit) {
        docGroupItems = ArrayList()
        getMaterialDocListStatus = Array(2, { false })
        for (i in 0..1) {
            post("/GetMaterialDocList", hashMapOf(
                    "courseId" to courseId,
                    "docType" to i.toString()
            )) { status, response ->
                if (status == OldE3Interface.Status.SUCCESS) {
                    processMaterialDocList(i, response!!, context, completionHandler)
                } else {
                    completionHandler(status, null)
                }
            }
        }
    }


    private fun processMaterialDocList(which: Int, response: JSONObject, context: Context,
                                       completionHandler: (status: OldE3Interface.Status,
                                                           response: ArrayList<DocGroupItem>?) -> Unit) {

        val arrayOfMaterialDocData = response.getJSONObject("ArrayOfMaterialDocData")
        val data = arrayOfMaterialDocData.forceGetJsonArray("MaterialDocData")
        (0 until data.length()).map { data.get(it) as JSONObject }
                .forEach {
                    var dateArray: List<String> = it.getString("BeginDate").split("/")
                    docGroupItems!!.add(DocGroupItem(
                            it.getString("DisplayName"),
                            it.getString("DocumentId"),
                            it.getString("CourseId"),
                            if (which == 0) context.getString(R.string.course_doc_type_handout)
                            else context.getString(R.string.course_doc_type_reference)
                    ))
                }
        getMaterialDocListStatus[which] = true
        if (getMaterialDocListStatus[0] && getMaterialDocListStatus[1]) {
            docGroupItems?.sortByDescending { it.docType }
            completionHandler(OldE3Interface.Status.SUCCESS, docGroupItems)
            docGroupItems = null
        }
    }

    override fun getAttachFileList(documentId: String, courseId: String,
                                   completionHandler: (status: OldE3Interface.Status,
                                                       response: ArrayList<AttachItem>?) -> Unit) {
        post("/GetAttachFileList", hashMapOf(
                "resId" to documentId,
                "metaType" to "10", //No idea what is this for
                "courseId" to courseId
        )) { status, response ->
            if (status == OldE3Interface.Status.SUCCESS) {
                val data = response!!.getJSONObject("ArrayOfAttachFileInfoData")
                        .forceGetJsonArray("AttachFileInfoData")
                val attachItems = ArrayList<AttachItem>()
                (0 until data.length()).map { data.get(it) as JSONObject }
                        .forEach {
                            attachItems.add(AttachItem(
                                    it.getString("DisplayFileName"),
                                    it.getString("FileSize"),
                                    it.getString("RealityFileName")))
                        }
                completionHandler(status, attachItems)
            } else {
                completionHandler(status, null)
            }
        }
    }

    private lateinit var getMemberListStatus: Array<Boolean>
    private var memberItems: ArrayList<MemberItem>? = null
    override fun getMemberList(courseId: String,
                               completionHandler: (status: OldE3Interface.Status,
                                                   response: ArrayList<MemberItem>?) -> Unit) {
        getMemberListStatus = arrayOf(false, false, false)
        memberItems = ArrayList()
        post("/GetMemberList", hashMapOf(
                "loginTicket" to loginTicket,
                "courseId" to courseId,
                "role" to "tea"
        )) { status, response ->
            if (status == OldE3Interface.Status.SUCCESS) {
                getMemberListStatus[0] = true
                processMembers(0, response!!, completionHandler)
            } else completionHandler(status, null)
        }
        post("/GetMemberList", hashMapOf(
                "loginTicket" to loginTicket,
                "courseId" to courseId,
                "role" to "ta"
        )) { status, response ->
            if (status == OldE3Interface.Status.SUCCESS) {
                getMemberListStatus[1] = true
                processMembers(1, response!!, completionHandler)
            } else completionHandler(status, null)
        }
        post("/GetMemberList", hashMapOf(
                "loginTicket" to loginTicket,
                "courseId" to courseId,
                "role" to "stu"
        )) { status, response ->
            if (status == OldE3Interface.Status.SUCCESS) {
                getMemberListStatus[2] = true
                processMembers(2, response!!, completionHandler)
            } else completionHandler(status, null)
        }
    }

    private fun processMembers(which: Int, response: JSONObject,
                               completionHandler: (status: OldE3Interface.Status,
                                                   response: ArrayList<MemberItem>?) -> Unit) {
        Log.d("RES", response.toString())
        val data = response.getJSONObject("ArrayOfAccountData").forceGetJsonArray("AccountData")
        Log.d("DATA", data.toString())
        when (which) {
            0 -> {
                (0 until data.length()).map { data.get(it) as JSONObject }
                        .forEach {
                            val type = if (it.getString("RoleName").contains("助教")) MemberType.TA else MemberType.TEA
                            memberItems!!.add(MemberItem(
                                    it.getString("Name"),
                                    it.getString("DepartId"),
                                    try {
                                        it.getString("EMail")
                                    } catch (e: JSONException) {
                                        ""
                                    }, type
                            ))
                        }
            }
            1, 2 -> {
                (0 until data.length()).map { data.get(it) as JSONObject }
                        .forEach {
                            memberItems!!.add(MemberItem(
                                    it.getString("Name"),
                                    it.getString("DepartId"),
                                    try {
                                        it.getString("EMail")
                                    } catch (e: JSONException) {
                                        ""
                                    }, if (which == 1) MemberType.STU else MemberType.AUDIT
                            ))
                        }
            }
        }
        if (getMemberListStatus[0] && getMemberListStatus[1] && getMemberListStatus[2]) {
            memberItems!!.sortBy { it.type }
            completionHandler(OldE3Interface.Status.SUCCESS, memberItems)
            memberItems = null
        }
    }

    override fun getScoreData(courseId: String,
                              completionHandler: (status: OldE3Interface.Status,
                                                  response: ArrayList<ScoreItem>?) -> Unit) {
        post("/GetScoreData", hashMapOf(
                "courseId" to courseId
        )) { status, response ->
            if (status == OldE3Interface.Status.SUCCESS) {
                Log.d("RESP", response.toString())
                val data = response!!.getJSONObject("ScoreData")
                val types = arrayOf("Office", "Exam", "Ques", "Hwk", "Discuss", "OneSelf",
                        "Score", "AdjustToScoreForAll", "Absence", "AdjustToScore", "Attendance", "FinalScore")
                val scoreItems = ArrayList<ScoreItem>()
                types.forEach {
                    if (data.has(it)) {
                        val scoreData = data.getJSONObject(it).forceGetJsonArray("ScoreItemData")
                        (0 until scoreData.length()).map { scoreData.get(it) as JSONObject }
                                .forEach {
                                    scoreItems.add(ScoreItem(it.getString("DisplayName"),
                                            it.getString("Score3")))
                                }
                    }
                }
                completionHandler(status, scoreItems)
            } else completionHandler(status, null)
        }

    }

    override fun cancelPendingRequests() {
        client.dispatcher().cancelAll()
    }
}

