package com.github.schaka.janitorr.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

@ConfigurationProperties(prefix = "file-system")
data class FileSystemProperties(
        val access: Boolean = false,
        val leavingSoonDir: String = Path.of(System.getProperty("user.home"), "janitorr", "leaving-soon").toString(),
        val mediaServerLeavingSoonDir: String? = null,
        val validateSeeding: Boolean = true,
        val fromScratch: Boolean = true,
        val freeSpaceCheckDir: String = Path.of("").toAbsolutePath().root?.toString() ?: "."
)
