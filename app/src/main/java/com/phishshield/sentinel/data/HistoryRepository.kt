package com.phishshield.sentinel.data

import android.content.Context
import com.phishshield.sentinel.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class HistoryState(val loaded: Boolean = false, val enabled: Boolean = false,
    val entries: List<HistoryRecord> = emptyList(), val working: Boolean = false, val error: String? = null,
    internal val epoch: Long = 0)
data class HistoryTicket internal constructor(val generation: Long)

/** One serialized consent+ledger store shared by scans and the listener in this app process. */
class HistoryRepository internal constructor(private val storage: HistoryStorage, private val clock: () -> Long = System::currentTimeMillis) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(HistoryState())
    val state = mutable.asStateFlow()
    @Volatile private var generation = 0L
    private val ready = scope.async {
        mutex.withLock {
            try {
                val saved = storage.read()
                val kept = HistoryPolicy.retain(saved.entries, clock())
                if (kept != saved.entries) storage.write(saved.copy(entries = kept))
                mutable.value = HistoryState(true, saved.enabled, kept)
            } catch (_: Exception) { unavailable() }
        }
    }
    private fun unavailable() {
        generation++
        mutable.value = HistoryState(loaded = true, error = "History storage unavailable. Saving is paused. Reset local history to recover.", epoch = generation)
    }
    suspend fun awaitReady() { ready.await() }
    fun ticket(): HistoryTicket? {
        val snapshot = mutable.value
        return if (snapshot.loaded && snapshot.enabled && snapshot.error == null && !snapshot.working) HistoryTicket(snapshot.epoch) else null
    }
    suspend fun record(ticket: HistoryTicket?, source: HistorySource, result: AnalysisResult): Boolean {
        if (ticket == null) return false
        ready.await()
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val before = mutable.value
                if (!before.enabled || before.error != null || ticket.generation != before.epoch) return@withLock false
                val now = clock()
                val entry = HistoryPolicy.metadata(UUID.randomUUID().toString(), now, source, result) ?: return@withLock false
                try {
                    val entries = HistoryPolicy.retain(listOf(entry) + before.entries, now)
                    storage.write(HistorySnapshot(true, entries))
                    mutable.value = before.copy(entries = entries)
                    true
                } catch (_: Exception) { unavailable(); false }
            }
        }
    }
    private suspend fun change(action: (HistoryState) -> HistorySnapshot?, reset: Boolean = false): Boolean {
        ready.await()
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val before = mutable.value
                generation++
                mutable.value = before.copy(working = true, epoch = generation)
                try {
                    val next = if (reset) { storage.reset(); HistorySnapshot() }
                        else { check(before.error == null); action(before)!!.also(storage::write) }
                    mutable.value = HistoryState(true, next.enabled, next.entries, epoch = generation)
                    true
                } catch (_: Exception) { unavailable(); false }
            }
        }
    }
    suspend fun setEnabled(enabled: Boolean): Boolean = if (enabled) change({ HistorySnapshot(true, it.entries) }) else change({ null }, reset = true)
    suspend fun deleteAll(): Boolean = change({ HistorySnapshot(it.enabled) })
    suspend fun delete(id: String): Boolean = change({ HistorySnapshot(it.enabled, it.entries.filterNot { entry -> entry.id == id }) })
    suspend fun refresh() {
        ready.await()
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val before = mutable.value
                if (before.error != null) return@withLock
                val kept = HistoryPolicy.retain(before.entries, clock())
                if (kept != before.entries) try {
                    storage.write(HistorySnapshot(before.enabled, kept)); mutable.value = before.copy(entries = kept)
                } catch (_: Exception) { unavailable() }
            }
        }
    }
    companion object {
        @Volatile private var instance: HistoryRepository? = null
        fun get(context: Context): HistoryRepository = instance ?: synchronized(this) {
            instance ?: HistoryRepository(EncryptedHistoryStorage(context.applicationContext)).also { instance = it }
        }
    }
}
