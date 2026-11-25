package com.gymtrainer.app.data

import android.content.Context
import com.gymtrainer.app.domain.DeloadKind

/** Coaching state that doesn't belong in Room: deload windows and which sessions were deloads. */
interface TrainingStore {
    /** Deload window (fatigue or manual) for a plan: runs while completed sessions < until. */
    fun deloadWindow(planId: Long): Pair<Int, DeloadKind>?
    fun setDeloadWindow(planId: Long, until: Int?, kind: DeloadKind?)
    /** Planned deload week the user skipped. */
    fun skippedWeek(planId: Long): Int?
    fun setSkippedWeek(planId: Long, week: Int?)
    /** Completed-session count when the last fatigue check started a deload (cool-down). */
    fun lastFatigueAt(planId: Long): Int?
    fun setLastFatigueAt(planId: Long, done: Int)
    fun deloadSessions(): Set<Long>
    fun addDeloadSession(sessionId: Long)
}

class MemoryTrainingStore : TrainingStore {
    private val windows = mutableMapOf<Long, Pair<Int, DeloadKind>>()
    private val skipped = mutableMapOf<Long, Int>()
    private val fatigue = mutableMapOf<Long, Int>()
    private val sessions = mutableSetOf<Long>()
    override fun deloadWindow(planId: Long) = windows[planId]
    override fun setDeloadWindow(planId: Long, until: Int?, kind: DeloadKind?) {
        if (until == null || kind == null) windows.remove(planId) else windows[planId] = until to kind
    }
    override fun skippedWeek(planId: Long) = skipped[planId]
    override fun setSkippedWeek(planId: Long, week: Int?) { if (week == null) skipped.remove(planId) else skipped[planId] = week }
    override fun lastFatigueAt(planId: Long) = fatigue[planId]
    override fun setLastFatigueAt(planId: Long, done: Int) { fatigue[planId] = done }
    override fun deloadSessions(): Set<Long> = sessions.toSet()
    override fun addDeloadSession(sessionId: Long) { sessions += sessionId }
}

class PrefsTrainingStore(context: Context) : TrainingStore {
    private val p = context.applicationContext.getSharedPreferences("gym_trainer_coaching", Context.MODE_PRIVATE)

    override fun deloadWindow(planId: Long): Pair<Int, DeloadKind>? {
        val until = p.getInt("dl_until_$planId", -1)
        val kind = p.getString("dl_kind_$planId", null)?.let { k -> DeloadKind.entries.firstOrNull { it.name == k } }
        return if (until < 0 || kind == null) null else until to kind
    }
    override fun setDeloadWindow(planId: Long, until: Int?, kind: DeloadKind?) {
        p.edit().apply {
            if (until == null || kind == null) { remove("dl_until_$planId"); remove("dl_kind_$planId") }
            else { putInt("dl_until_$planId", until); putString("dl_kind_$planId", kind.name) }
        }.apply()
    }
    override fun skippedWeek(planId: Long) = p.getInt("dl_skip_$planId", -1).takeIf { it >= 0 }
    override fun setSkippedWeek(planId: Long, week: Int?) {
        p.edit().apply { if (week == null) remove("dl_skip_$planId") else putInt("dl_skip_$planId", week) }.apply()
    }
    override fun lastFatigueAt(planId: Long) = p.getInt("dl_fat_$planId", -1).takeIf { it >= 0 }
    override fun setLastFatigueAt(planId: Long, done: Int) { p.edit().putInt("dl_fat_$planId", done).apply() }
    override fun deloadSessions(): Set<Long> =
        p.getStringSet("dl_sessions", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
    override fun addDeloadSession(sessionId: Long) {
        p.edit().putStringSet("dl_sessions", p.getStringSet("dl_sessions", emptySet()).orEmpty() + sessionId.toString()).apply()
    }
}
