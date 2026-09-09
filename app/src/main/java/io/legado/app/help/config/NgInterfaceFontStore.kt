package io.legado.app.help.config

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.util.LruCache
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefString
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** 界面字体与阅读正文的字体偏好分离；自定义字体使用私有副本。 */
internal object NgInterfaceFontStore {
    private const val CHOICE = "ngInterfaceFont.choice"
    private const val FILE = "ngInterfaceFont.file"
    private const val NAME = "ngInterfaceFont.name"
    const val DIRECTORY = "ngInterfaceFont.directory"
    private val cache = LruCache<String, Typeface>(4)
    private val preparedFiles = LruCache<String, File>(12)
    private val loader = Semaphore(2)
    suspend fun load(context: Context, choice: String): Pair<Typeface, File?> =
        withContext(Dispatchers.IO) { loader.withPermit { prepare(context, choice) } }
    val systemChoices = listOf("system", "serif", "monospace")

    fun choice(context: Context): String = context.getPrefString(CHOICE)
        ?: NgThemeLibraryStore.activeTheme(context)?.let { theme ->
            theme.resolvePackageAsset(theme.resourceProfile?.appFont)?.toURI()?.toString()
        } ?: "system"
    fun name(context: Context): String = context.getPrefString(NAME).orEmpty()
    fun hasOverride(context: Context): Boolean = context.getPrefString(CHOICE) != null
    fun systemTypeface(choice: String): Typeface = when (choice) {
        "serif" -> Typeface.SERIF
        "monospace" -> Typeface.MONOSPACE
        else -> Typeface.DEFAULT
    }
    private fun fromFile(file: File): Typeface = synchronized(cache) {
        cache.get(file.absolutePath) ?: Typeface.createFromFile(file).also { cache.put(file.absolutePath, it) }
    }
    fun typeface(context: Context): Typeface {
        val choice = choice(context)
        if (choice in systemChoices) return systemTypeface(choice)
        return runCatching { fromFile(File(context.getPrefString(FILE).orEmpty())) }.getOrDefault(Typeface.DEFAULT)
    }
    fun prepare(context: Context, choice: String): Pair<Typeface, File?> {
        if (choice in systemChoices) return systemTypeface(choice) to null
        val sourceKey = if (choice.startsWith("file:")) {
            val file = File(Uri.parse(choice).path.orEmpty())
            "$choice:${file.lastModified()}:${file.length()}"
        } else choice
        synchronized(preparedFiles) { preparedFiles.get(sourceKey) }?.takeIf { it.isFile }?.let {
            return fromFile(it) to it
        }
        if (choice == context.getPrefString(CHOICE)) {
            val active = File(context.getPrefString(FILE).orEmpty())
            if (active.isFile) return fromFile(active) to active
        }
        val root = File(context.cacheDir, "interface-font-preview").apply { mkdirs() }
        val temp = File.createTempFile("font-", ".tmp", root)
        try {
            val uri = Uri.parse(choice)
            val input = if (uri.scheme == "content") context.contentResolver.openInputStream(uri)
                else File(uri.path ?: choice).inputStream()
            requireNotNull(input) { "无法读取字体" }.use { source ->
                temp.outputStream().use { target ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 64L * 1024 * 1024) { "字体文件超过64MB" }
                        target.write(buffer, 0, count)
                    }
                }
            }
            val digest = MessageDigest.getInstance("SHA-256")
            temp.inputStream().use { source ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) { val n = source.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
            }
            val target = File(root, digest.digest().joinToString("") { "%02x".format(it) } + ".font")
            if (!target.exists()) check(temp.renameTo(target) || target.isFile) { "无法缓存字体" }
            synchronized(preparedFiles) { preparedFiles.put(sourceKey, target) }
            return fromFile(target) to target
        } finally { temp.delete() }
    }
    fun save(context: Context, choice: String, name: String, prepared: File?) {
        if (choice !in systemChoices) {
            val source = requireNotNull(prepared) { "请等待字体加载完成" }
            val root = File(context.filesDir, "interface-fonts").apply { mkdirs() }
            val target = File(root, source.name)
            if (source.canonicalPath != target.canonicalPath && !target.exists()) {
                val temporary = File.createTempFile("install-", ".tmp", root)
                try {
                    source.copyTo(temporary, overwrite = true)
                    Typeface.createFromFile(temporary)
                    check(temporary.renameTo(target) || target.isFile) { "无法保存字体" }
                } finally { temporary.delete() }
            }
            fromFile(target)
            context.putPrefString(FILE, target.absolutePath)
        }
        context.putPrefString(NAME, name)
        context.putPrefString(CHOICE, choice)
    }
}
