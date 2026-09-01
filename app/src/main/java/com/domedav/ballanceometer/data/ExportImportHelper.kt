package com.domedav.ballanceometer.data

import org.json.JSONArray
import org.json.JSONObject

object ExportImportHelper {

    const val VERSION = 1

    fun buildExportJson(
        config: AppConfig?,
        groups: List<TaskGroup>,
        subtasks: List<Subtask>,
        completions: List<Completion>,
        spendings: List<Spending>
    ): JSONObject {
        val root = JSONObject()
        root.put("version", VERSION)

        if (config != null) {
            val cfg = JSONObject()
            cfg.put("id", config.id)
            cfg.put("totalBalance", config.totalBalance)
            cfg.put("minimalSpend", config.minimalSpend)
            cfg.put("currency", config.currency)
            root.put("appConfig", cfg)
        } else {
            root.put("appConfig", JSONObject.NULL)
        }

        val groupsArr = JSONArray()
        for (g in groups) {
            val o = JSONObject()
            o.put("id", g.id)
            o.put("name", g.name)
            o.put("weight", g.weight.toDouble())
            o.put("createdAt", g.createdAt)
            groupsArr.put(o)
        }
        root.put("taskGroups", groupsArr)

        val subtasksArr = JSONArray()
        for (s in subtasks) {
            val o = JSONObject()
            o.put("id", s.id)
            o.put("groupId", s.groupId)
            o.put("title", s.title)
            o.put("recurrence", s.recurrence)
            o.put("createdAt", s.createdAt)
            subtasksArr.put(o)
        }
        root.put("subtasks", subtasksArr)

        val completionsArr = JSONArray()
        for (c in completions) {
            val o = JSONObject()
            o.put("id", c.id)
            o.put("subtaskId", c.subtaskId)
            o.put("date", c.date)
            o.put("completedAt", c.completedAt)
            o.put("valueUnlocked", c.valueUnlocked)
            completionsArr.put(o)
        }
        root.put("completions", completionsArr)

        val spendingsArr = JSONArray()
        for (s in spendings) {
            val o = JSONObject()
            o.put("id", s.id)
            o.put("amount", s.amount)
            o.put("note", s.note)
            o.put("timestamp", s.timestamp)
            spendingsArr.put(o)
        }
        root.put("spendings", spendingsArr)

        return root
    }

    data class ParsedData(
        val config: AppConfig?,
        val groups: List<TaskGroup>,
        val subtasks: List<Subtask>,
        val completions: List<Completion>,
        val spendings: List<Spending>
    )

    fun parseImportJson(jsonString: String): ParsedData {
        val root = JSONObject(jsonString)

        val config: AppConfig? = if (root.has("appConfig") && !root.isNull("appConfig")) {
            val cfg = root.getJSONObject("appConfig")
            AppConfig(
                id = cfg.optInt("id", 1),
                totalBalance = cfg.optDouble("totalBalance", 0.0),
                minimalSpend = cfg.optDouble("minimalSpend", 0.0),
                currency = cfg.optString("currency", "HUF")
            )
        } else {
            null
        }

        val groups = mutableListOf<TaskGroup>()
        if (root.has("taskGroups") && !root.isNull("taskGroups")) {
            val arr = root.getJSONArray("taskGroups")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                groups.add(
                    TaskGroup(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        weight = o.optDouble("weight", 0.0).toFloat(),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val subtasks = mutableListOf<Subtask>()
        if (root.has("subtasks") && !root.isNull("subtasks")) {
            val arr = root.getJSONArray("subtasks")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                subtasks.add(
                    Subtask(
                        id = o.getString("id"),
                        groupId = o.getString("groupId"),
                        title = o.getString("title"),
                        recurrence = o.optString("recurrence", "ONCE"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val completions = mutableListOf<Completion>()
        if (root.has("completions") && !root.isNull("completions")) {
            val arr = root.getJSONArray("completions")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                completions.add(
                    Completion(
                        id = o.getString("id"),
                        subtaskId = o.getString("subtaskId"),
                        date = o.getString("date"),
                        completedAt = o.optLong("completedAt", System.currentTimeMillis()),
                        valueUnlocked = o.optDouble("valueUnlocked", 0.0)
                    )
                )
            }
        }

        val spendings = mutableListOf<Spending>()
        if (root.has("spendings") && !root.isNull("spendings")) {
            val arr = root.getJSONArray("spendings")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                spendings.add(
                    Spending(
                        id = o.getString("id"),
                        amount = o.optDouble("amount", 0.0),
                        note = o.optString("note", ""),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        return ParsedData(config, groups, subtasks, completions, spendings)
    }
}
