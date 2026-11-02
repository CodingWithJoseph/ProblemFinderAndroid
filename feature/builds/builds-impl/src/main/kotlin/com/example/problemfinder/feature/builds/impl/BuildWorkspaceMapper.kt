package com.example.problemfinder.feature.builds.impl

import com.example.problemfinder.core.model.workspace.BuildWorkspaceData

internal fun BuildWorkspaceData.toPresentation(): BuildWorkspace {
    val model = solution
    val tasksById = model.tasks.associateBy { it.id }
    val plan = buildList {
        model.architecture?.let { a ->
            add(BuildPlanSection("architecture", "", "", a.stack + a.components.map { it.name + ": " + it.responsibility }, BuildPlanKind.ARCHITECTURE))
            add(BuildPlanSection("data", "", "", a.entities.map { it.name + ": " + it.purpose + "\n" + it.fields.joinToString(", ") }, BuildPlanKind.DATA))
            add(BuildPlanSection("apis", "", "", a.apis.map { it.method + " " + it.path + ": " + it.purpose }, BuildPlanKind.APIS))
            add(BuildPlanSection("integrations", "", "", a.integrations, BuildPlanKind.INTEGRATIONS))
            add(BuildPlanSection("security", "", "", a.security, BuildPlanKind.SECURITY))
        }
        model.testPlan?.let { add(BuildPlanSection("tests", "", "", it.unit + it.integration + it.endToEnd, BuildPlanKind.TESTS)) }
        add(BuildPlanSection("delivery", "", "", model.delivery.map { it.label }, BuildPlanKind.DELIVERY))
    }.filter { it.items.isNotEmpty() }
    return BuildWorkspace(
        project.id, project.clusterId, project.name, model.goal.orEmpty(), model.version.toString(), "",
        model.users, model.constraints, model.nonGoals, model.primaryFlow,
        model.features.map { BuildFeature(it.id, it.title, it.summary, it.acceptanceCriteria) },
        model.tasks.map { BuildTask(it.id, it.title, it.description, BuildTaskStatus.valueOf(it.status.name),
            it.acceptanceChecks, it.dependencies.mapNotNull { id -> tasksById[id]?.title }) },
        plan, model.documents.map { BuildDocument(it.key, it.title, it.filename, it.content, it.editable) },
        model.version, model.approvalStatus == "approved" && model.approvedVersion == model.version,
    )
}
