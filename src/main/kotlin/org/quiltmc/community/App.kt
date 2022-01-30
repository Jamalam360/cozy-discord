/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(PrivilegedIntent::class)

/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package org.quiltmc.community

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.modules.extra.mappings.extMappings
import com.kotlindiscord.kord.extensions.modules.extra.phishing.DetectionAction
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import com.kotlindiscord.kord.extensions.utils.envOrNull
import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.common.entity.Permission
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import org.quiltmc.community.cozy.modules.cleanup.userCleanup
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.modes.quilt.extensions.*
import org.quiltmc.community.modes.quilt.extensions.filtering.FilterExtension
import org.quiltmc.community.modes.quilt.extensions.github.GithubExtension
import org.quiltmc.community.modes.quilt.extensions.messagelog.MessageLogExtension
import org.quiltmc.community.modes.quilt.extensions.minecraft.MinecraftExtension
import org.quiltmc.community.modes.quilt.extensions.settings.SettingsExtension
import org.quiltmc.community.modes.quilt.extensions.suggestions.SuggestionsExtension
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

val MODE = envOrNull("MODE")?.lowercase() ?: "quilt"
val ENVIRONMENT = envOrNull("ENVIRONMENT")?.lowercase() ?: "production"

suspend fun setupCollab() = ExtensibleBot(DISCORD_TOKEN) {
    common()
    database()

    extensions {
        sentry {
            distribution = "collab"
        }
    }
}

suspend fun setupDev() = ExtensibleBot(DISCORD_TOKEN) {
    common()
    database()

    extensions {
        add(::SubteamsExtension)

        extMappings { }

        if (GITHUB_TOKEN != null) {
            add(::GithubExtension)
        }

        sentry {
            distribution = "dev"
        }
    }
}

suspend fun setupQuilt() = ExtensibleBot(DISCORD_TOKEN) {
    common()
    database(true)
    settings()

    chatCommands {
        enabled = true
    }

    intents {
        +Intents.all
    }

    members {
        all()

        fillPresences = true
    }

    extensions {
        add(::FilterExtension)
        add(::MessageLogExtension)
        add(::MinecraftExtension)
        add(::PKExtension)
        add(::SettingsExtension)
        add(::ShowcaseExtension)
        add(::SuggestionsExtension)
        add(::SyncExtension)
//        add(::UserCleanupExtension)
        add(::UtilityExtension)

        extPhishing {
            appName = "QuiltMC's Cozy Bot"
            detectionAction = DetectionAction.Kick
            logChannelName = "cozy-logs"
            requiredCommandPermission = null

            check { inQuiltGuild() }
            check { notHasBaseModeratorRole() }
        }

        userCleanup {
            maxPendingDuration = 3.days
            taskDelay = 1.hours
            loggingChannelName = "cozy-logs"

            runAutomatically = false

            guildPredicate {
                val servers = getKoin().get<ServerSettingsCollection>()
                val serverEntry = servers.get(it.id)

                serverEntry?.quiltServerType != null
            }

            commandCheck { hasPermission(Permission.Administrator) }
        }

        sentry {
            distribution = "community"
        }
    }
}

suspend fun setupShowcase() = ExtensibleBot(DISCORD_TOKEN) {
    common()
    database()
    settings()

    extensions {
        sentry {
            distribution = "showcase"
        }
    }
}

suspend fun main() {
    val bot = when (MODE) {
        "dev" -> setupDev()
        "collab" -> setupCollab()
        "quilt" -> setupQuilt()
        "showcase" -> setupShowcase()

        else -> error("Invalid mode: $MODE")
    }

    bot.start()
}
