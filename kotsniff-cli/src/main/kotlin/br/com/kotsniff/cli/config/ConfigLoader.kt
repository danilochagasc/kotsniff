package br.com.kotsniff.cli.config

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Loads and persists [FileConfig] from/to disk. */
object ConfigLoader {
    private val json = Json { ignoreUnknownKeys = true }

    /** Pretty JSON that omits fields equal to their defaults. */
    private val writer =
        Json {
            prettyPrint = true
            encodeDefaults = false
        }

    /**
     * Loads the configuration from [file].
     *
     * @return the parsed config, or `null` when the file does not exist.
     * @throws IllegalArgumentException when the file exists but is malformed.
     */
    fun load(file: File): FileConfig? {
        if (!file.exists()) return null
        return try {
            // Tolerate a leading UTF-8 BOM (added by some editors on Windows).
            json.decodeFromString<FileConfig>(file.readText().removePrefix("\uFEFF"))
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid configuration file '${file.path}': ${e.message}", e)
        }
    }

    /** Writes [config] to [file], creating parent directories if needed. */
    fun save(
        file: File,
        config: FileConfig,
    ) {
        file.parentFile?.mkdirs()
        file.writeText(writer.encodeToString(config))
    }
}
