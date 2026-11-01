package org.solidcraft.mobile

import org.json.JSONObject

/** Stores the durable relationship between an app-private CAD working copy and its SAF URI. */
internal object DocumentMapping {
    fun persist(path: String, uri: String, writable: Boolean, write: (String, String) -> Boolean) {
        require(path.isNotBlank()) { "The CAD working path is missing" }
        require(uri.isNotBlank()) { "The document provider URI is missing" }
        val encoded = JSONObject().put("uri", uri).put("writable", writable).toString()
        check(write(path, encoded)) { "Could not persist the Android document mapping" }
    }
}
