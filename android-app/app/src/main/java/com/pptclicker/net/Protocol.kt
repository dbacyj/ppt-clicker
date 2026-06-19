package com.pptclicker.net

import org.json.JSONArray
import org.json.JSONObject

/**
 * 协议消息构造器，对应 protocol/spec.md。
 * 集中管理 JSON 结构，避免散落各处出错。
 */
object Protocol {
    const val VERSION = 1
    const val DEFAULT_PORT = 48721

    private fun envelope(type: String, data: JSONObject, id: String? = null): String =
        JSONObject().apply {
            put("v", VERSION)
            put("type", type)
            if (id != null) put("id", id)
            put("data", data)
        }.toString()

    fun hello(pairCode: String, clientName: String, appVersion: String): String =
        envelope("hello", JSONObject().apply {
            put("pair_code", pairCode)
            put("client", JSONObject().apply {
                put("name", clientName)
                put("platform", "android")
                put("app_version", appVersion)
            })
            put("caps", JSONArray().apply { put("key"); put("key_combo") })
        }, id = "hello")

    fun key(code: String, id: String): String =
        envelope("key", JSONObject().apply {
            put("code", code)
            put("hold_ms", 0)
        }, id)

    fun ping(id: String, ts: Long): String =
        envelope("ping", JSONObject().apply { put("ts", ts) }, id)
}
