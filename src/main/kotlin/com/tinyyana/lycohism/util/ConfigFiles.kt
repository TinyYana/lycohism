package com.tinyyana.lycohism.util

import com.tinyyana.lycohism.Lycohism
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/** Loads an editable data-folder YAML while filling missing keys from the bundled resource. */
object ConfigFiles {

    fun load(plugin: Lycohism, fileName: String): YamlConfiguration {
        val file = File(plugin.dataFolder, fileName)
        if (!file.exists()) plugin.saveResource(fileName, false)

        val yaml = YamlConfiguration.loadConfiguration(file)
        plugin.getResource(fileName)?.use { stream ->
            InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
                yaml.setDefaults(YamlConfiguration.loadConfiguration(reader))
                // Copy only into the in-memory view. Existing files and custom values
                // stay untouched, while newly bundled v0.2 sections remain available.
                yaml.options().copyDefaults(true)
            }
        }
        return yaml
    }
}
