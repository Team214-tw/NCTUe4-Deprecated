package com.team214.nctue4.utility


import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

fun JSONObject.forceGetJsonArray(name: String): JSONArray {
    return if (this.has(name)) {
        try {
            this.getJSONArray(name)
        } catch (e: JSONException) {
            val tmp = JSONArray()
            tmp.put(this.getJSONObject(name))
            tmp
        }
    } else JSONArray()
}
