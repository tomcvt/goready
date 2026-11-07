package com.tomcvt.goready.data

import com.tomcvt.goready.constants.TaskType
import org.json.JSONArray
import org.json.JSONObject

data class TaskChainRecord(
    val taskType: TaskType,
    val taskData: String
)

fun encodeTaskChain(records: List<TaskChainRecord>): String {
    val array = JSONArray()
    records.forEach {
        array.put(
            JSONObject()
                .put("taskType", it.taskType.name)
                .put("taskData", it.taskData)
        )
    }
    return array.toString()
}

/** Returns an empty list for blank or malformed input; unknown task types are skipped. */
fun decodeTaskChain(json: String?): List<TaskChainRecord> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val type = runCatching { TaskType.valueOf(obj.getString("taskType")) }.getOrNull()
                ?: return@mapNotNull null
            TaskChainRecord(type, obj.optString("taskData", ""))
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/** Singular tasks become a one-element list; a task chain is decoded from its json. */
fun taskToRecords(taskType: TaskType, taskData: String?): List<TaskChainRecord> =
    if (taskType == TaskType.TASK_CHAIN) decodeTaskChain(taskData)
    else listOf(TaskChainRecord(taskType, taskData ?: ""))

