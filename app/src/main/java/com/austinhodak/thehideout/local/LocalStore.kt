package com.austinhodak.thehideout.local

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The app's only user-data store: one JSON tree in `filesDir/local_store.json`, no account and no network.
 *
 * Top-level nodes: `user` (progress, keys, player level), `tracker` (needed items, cart), `loadouts/<id>`
 * and `markers/<id>`. Paths use `/` (see [dotted] for Firestore-style `a.b.c`). Listeners always run on the
 * main thread. shortcut: the whole file is rewritten (once per burst of writes) and listeners are not diffed, which is
 * fine for a few hundred small entries; move to Room if the tree grows large.
 */
object LocalStore {
    /** Marker value: removes the node it is written to. */
    object Delete

    /** Marker value: adds [by] to the number at the node it is written to. */
    class Increment(
        val by: Long,
    )

    private class Listener(
        val path: String,
        val onChange: (Any?) -> Unit,
    )

    private val gson: Gson = GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create()
    private val mapType = object : TypeToken<LinkedHashMap<String, Any?>>() {}.type
    private val main = Handler(Looper.getMainLooper())
    private val writer = Executors.newSingleThreadExecutor()
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val savePending = AtomicBoolean(false)

    // Paths written since listeners last ran; guarded by this.
    private val dirtyPaths = HashSet<String>()
    private var root = LinkedHashMap<String, Any?>()
    private lateinit var file: File

    @Synchronized
    fun init(context: Context) {
        if (::file.isInitialized) return
        file = File(context.filesDir, "local_store.json")
        root =
            try {
                if (file.exists()) gson.fromJson(file.readText(), mapType) ?: LinkedHashMap() else LinkedHashMap()
            } catch (e: Exception) {
                // Keep the unreadable file for recovery instead of overwriting it with an empty tree.
                Timber.e(e, "local_store.json unreadable")
                file.renameTo(File(context.filesDir, "local_store.corrupt.${System.currentTimeMillis()}.json"))
                LinkedHashMap()
            }
    }

    @Synchronized
    fun get(path: String): Any? {
        var node: Any? = root
        for (key in segments(path)) node = (node as? Map<*, *>)?.get(key) ?: return null
        return node
    }

    inline fun <reified T> getAs(path: String): T? = convert(get(path), T::class.java)

    fun set(
        path: String,
        value: Any?,
    ) {
        val keys = segments(path)
        if (keys.isEmpty()) return
        // The JSON round trip runs before the lock so a big value does not block readers on the main thread.
        val plain = if (value === Delete || value == null || value is Increment) value else normalize(value)
        synchronized(this) {
            when {
                plain === Delete || plain == null -> remove(keys)
                plain is Increment -> write(keys, ((read(keys) as? Number)?.toLong() ?: 0L) + plain.by)
                else -> write(keys, plain)
            }
            changed(path)
        }
    }

    /** Deep-merges [values] into the node at [path]; [Delete] and [Increment] values apply to their key. */
    @Synchronized
    fun merge(
        path: String,
        values: Map<String, Any?>,
    ) {
        mergeInto(segments(path), values)
        changed(path)
    }

    private fun mergeInto(
        base: List<String>,
        values: Map<*, *>,
    ) {
        for ((key, value) in values) {
            if (key == null) continue
            val keys = base + key.toString()
            when {
                value === Delete || value == null -> remove(keys)
                value is Increment -> write(keys, ((read(keys) as? Number)?.toLong() ?: 0L) + value.by)
                value is Map<*, *> -> mergeInto(keys, value)
                else -> write(keys, normalize(value))
            }
        }
    }

    fun remove(path: String) = set(path, Delete)

    /** Calls [onChange] now and after every change to the store, with the value at [path]. */
    fun listen(
        path: String,
        onChange: (Any?) -> Unit,
    ): Closeable {
        val listener = Listener(path, onChange)
        listeners.add(listener)
        main.post { if (listener in listeners) onChange(get(path)) }
        return Closeable { listeners.remove(listener) }
    }

    inline fun <reified T> listenAs(
        path: String,
        crossinline onChange: (T?) -> Unit,
    ): Closeable = listen(path) { onChange(convert(it, T::class.java)) }

    // Locked because [value] is a live node that writers on other threads mutate; a node of the wrong shape reads as empty.
    @Synchronized
    fun <T> convert(
        value: Any?,
        type: Class<T>,
    ): T? =
        if (value == null) {
            null
        } else {
            try {
                gson.fromJson(gson.toJsonTree(value), type)
            } catch (e: JsonParseException) {
                Timber.e(e, "Ignoring malformed %s data", type.simpleName)
                null
            }
        }

    @Synchronized
    fun exportJson(): String = gson.toJson(root)

    /** Replaces everything with [json]. Throws if it is not a JSON object, leaving the current data untouched. */
    @Synchronized
    fun importJson(json: String) {
        val parsed: LinkedHashMap<String, Any?> = gson.fromJson(json, mapType) ?: throw IllegalArgumentException("empty")
        root = parsed
        changed("")
    }

    /** Firestore-style `a.b.c` field path to a store path. */
    fun dotted(field: String) = field.replace('.', '/')

    private fun segments(path: String) = path.split('/').filter { it.isNotEmpty() }

    private fun read(keys: List<String>): Any? {
        var node: Any? = root
        for (key in keys) node = (node as? Map<*, *>)?.get(key) ?: return null
        return node
    }

    @Suppress("UNCHECKED_CAST")
    private fun write(
        keys: List<String>,
        value: Any?,
    ) {
        var node = root as MutableMap<String, Any?>
        for (key in keys.dropLast(1)) {
            node = (node[key] as? MutableMap<String, Any?>) ?: LinkedHashMap<String, Any?>().also { node[key] = it }
        }
        node[keys.last()] = value
    }

    /** Removes the node and any parents left empty, like the Realtime Database did. */
    @Suppress("UNCHECKED_CAST")
    private fun remove(keys: List<String>) = prune(root, keys, 0)

    @Suppress("UNCHECKED_CAST")
    private fun prune(
        node: MutableMap<String, Any?>,
        keys: List<String>,
        i: Int,
    ) {
        if (i == keys.lastIndex) {
            node.remove(keys[i])
            return
        }
        val child = node[keys[i]] as? MutableMap<String, Any?> ?: return
        prune(child, keys, i + 1)
        if (child.isEmpty()) node.remove(keys[i])
    }

    // Round-trips through JSON so stored values are plain maps, lists, strings, numbers and booleans.
    private fun normalize(value: Any?): Any? = gson.fromJson(gson.toJson(value), Any::class.java)

    /** Saves once and wakes only the listeners near [path], however many writes land in a burst. Call while holding the lock. */
    private fun changed(path: String) {
        if (savePending.compareAndSet(false, true)) writer.execute { save() }
        val first = dirtyPaths.isEmpty()
        dirtyPaths.add(segments(path).joinToString("/"))
        if (first) main.post { notifyListeners() }
    }

    private fun save() {
        savePending.set(false)
        val json = synchronized(this) { gson.toJson(root) }
        try {
            val tmp = File(file.parentFile, "local_store.json.tmp")
            tmp.writeText(json)
            tmp.renameTo(file)
        } catch (e: Exception) {
            Timber.e(e, "Could not save local_store.json")
        }
    }

    private fun notifyListeners() {
        val paths = synchronized(this) { dirtyPaths.toList().also { dirtyPaths.clear() } }
        for (listener in listeners) {
            if (paths.any { overlaps(it, listener.path) }) listener.onChange(get(listener.path))
        }
    }

    // A write at "user/a" changes a listener on "user" and one on "user/a/b", not one on "tracker".
    private fun overlaps(
        changed: String,
        listening: String,
    ): Boolean {
        val l = segments(listening).joinToString("/")
        return changed.isEmpty() || l.isEmpty() || changed == l || changed.startsWith("$l/") || l.startsWith("$changed/")
    }
}
