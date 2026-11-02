package com.example.problemfinder.feature.builds.api

object BuildsDestination {
    const val ROUTE = "builds"
    const val PROJECT_ID = "projectId"
    const val ITEM_ID = "itemId"
    const val WORKSPACE = "builds/{$PROJECT_ID}"
    const val FEATURE = "builds/{$PROJECT_ID}/features/{$ITEM_ID}"
    const val TASK = "builds/{$PROJECT_ID}/tasks/{$ITEM_ID}"
    const val PLAN = "builds/{$PROJECT_ID}/plan"
    const val DOCUMENTS = "builds/{$PROJECT_ID}/documents"
    const val DOCUMENT = "builds/{$PROJECT_ID}/documents/{$ITEM_ID}"
    fun workspace(id: String) = "builds/" + encode(id)
    fun feature(id: String, item: String) = workspace(id) + "/features/" + encode(item)
    fun task(id: String, item: String) = workspace(id) + "/tasks/" + encode(item)
    fun plan(id: String) = workspace(id) + "/plan"
    fun documents(id: String) = workspace(id) + "/documents"
    fun document(id: String, item: String) = workspace(id) + "/documents/" + encode(item)
    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
