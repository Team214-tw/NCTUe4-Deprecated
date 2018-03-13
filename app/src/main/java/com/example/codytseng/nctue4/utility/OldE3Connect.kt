package com.example.codytseng.nctue4.utility

import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import org.json.JSONArray
import org.json.JSONObject


class OldE3Connect : OldE3Interface {
    private var loginTicket: String = ""
    private var accountId: String = ""
    private val tag = OldE3Connect::class.java.simpleName

    private fun post(path: String, params: HashMap<String, String>,
                     completionHandler: (status: OldE3Interface.Status, response: JSONObject?) -> Unit) {
        val url = "http://e3.nctu.edu.tw/mService/service.asmx" + path
        val stringRequest = object : StringRequest(Request.Method.POST, url,
                Response.Listener<String> { response ->
                    val xmlToJson = XmlToJson.Builder(response).build().toJson()
                    completionHandler(OldE3Interface.Status.SUCCESS, xmlToJson)
                },
                Response.ErrorListener { response ->
                    Log.d("jizz", response.toString())
                    completionHandler(OldE3Interface.Status.SERVICE_ERROR, null)
                }) {
            override fun getParams(): Map<String, String> {
                return params
            }
        }
        VolleyHandler.instance?.addToRequestQueue(stringRequest, tag)
    }


    override fun getLoginTicket(studentId: String, studentPassword: String,
                                completionHandler: (status: OldE3Interface.Status,
                                                    response: Pair<String, String>?) -> Unit) {
        val params = HashMap<String, String>()
        params.put("account", studentId)
        params.put("password", studentPassword)
        post("/Login", params) { status, response ->
            if (status == OldE3Interface.Status.SUCCESS) {
                val accountData = response!!.getJSONObject("AccountData")
                if (accountData.has("Name")) {
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

    override fun getCourseList(completionHandler: (status: OldE3Interface.Status, response: JSONArray?) -> Unit) {
        val params = HashMap<String, String>()
        params.put("loginTicket", loginTicket)
        params.put("accountId", accountId)
        params.put("role", "stu")
        post("/GetCourseList", params) { status, response ->
            if (status == OldE3Interface.Status.SUCCESS) {
                completionHandler(status, response!!.getJSONObject("ArrayOfCourseData")
                        .getJSONArray("CourseData"))
            } else {
                completionHandler(status, null)
            }
        }
    }

    override fun getAnnouncementList_Login(completionHandler: (status: OldE3Interface.Status, response: JSONArray?) -> Unit) {
        val params = HashMap<String, String>()
        params.put("loginTicket", loginTicket)
        params.put("studentId", accountId)
        post("/GetAnnouncementList_Login", params) { status, response ->
            if (status == OldE3Interface.Status.SUCCESS) {
                completionHandler(status, response!!.getJSONObject("ArrayOfBulletinData")
                        .getJSONArray("BulletinData"))
            } else {
                completionHandler(status, null)
            }
        }
    }
}

