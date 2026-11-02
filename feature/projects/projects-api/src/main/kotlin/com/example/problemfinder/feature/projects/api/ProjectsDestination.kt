package com.example.problemfinder.feature.projects.api

object ProjectsDestination {
    const val ROUTE = "projects"
    const val PROJECT_ID = "projectId"
    const val ITEM_ID = "itemId"
    const val WORKSPACE = "projects/{$PROJECT_ID}"
    const val PROBLEM = "projects/{$PROJECT_ID}/problems/{$ITEM_ID}"
    const val AUDIENCE = "projects/{$PROJECT_ID}/audiences/{$ITEM_ID}"
    const val ASSUMPTIONS = "projects/{$PROJECT_ID}/assumptions"
    const val CONVERSATION = "projects/{$PROJECT_ID}/conversation"
    fun workspace(id: String) = "projects/" + encode(id)
    fun problem(id: String, item: String) = workspace(id) + "/problems/" + encode(item)
    fun audience(id: String, item: String) = workspace(id) + "/audiences/" + encode(item)
    fun assumptions(id: String) = workspace(id) + "/assumptions"
    fun conversation(id: String) = workspace(id) + "/conversation"
    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
