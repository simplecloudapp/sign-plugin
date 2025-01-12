package app.simplecloud.plugin.sign.shared.utils

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.sign.shared.SignManagerProvider
import app.simplecloud.plugin.sign.shared.config.rule.RuleConfig
import app.simplecloud.plugin.sign.shared.matcher.MatcherConfigEntry
import app.simplecloud.plugin.sign.shared.matcher.MatcherType
import app.simplecloud.plugin.sign.shared.rule.RuleContext
import app.simplecloud.plugin.sign.shared.rule.impl.ServerRuleContext
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.minimessage.MiniMessage

object MatcherUtil {

    private val miniMessage = MiniMessage.miniMessage()

    fun matches(rule: RuleConfig, ruleContext: RuleContext): Boolean {
        return runBlocking {
            rule.matcher.all { (type, entries) ->
                evaluateMatcher(type, entries, ruleContext)
            }
        }
    }

    private suspend fun evaluateMatcher(
        type: MatcherType,
        entries: List<MatcherConfigEntry>,
        ruleContext: RuleContext
    ): Boolean {
        return when (type) {
            MatcherType.MATCH_ALL -> entries.all { entry ->
                entry.operation.matches(
                    resolvePlaceholder(entry.key, ruleContext),
                    resolvePlaceholder(entry.value, ruleContext),
                    entry.negate
                )
            }

            MatcherType.MATCH_ANY -> entries.any { entry ->
                entry.operation.matches(
                    resolvePlaceholder(entry.key, ruleContext),
                    resolvePlaceholder(entry.value, ruleContext),
                    entry.negate
                )
            }
        }
    }

    private suspend fun resolvePlaceholder(placeholder: String, ruleContext: RuleContext): String {
        val pattern = "<(server_|group_|env_)([^>]+)>".toRegex()

        return when {
            placeholder.startsWith("<server_") || placeholder.startsWith("<group_") -> {
                if (ruleContext !is ServerRuleContext) return placeholder
                if (placeholder == "<server_state>" && ruleContext.server == null) return "null"

                ruleContext.server?.let { server ->
                    when {
                        placeholder.startsWith("<server_") -> {
                            val innerKey = pattern.find(placeholder)?.groupValues?.get(2) ?: return placeholder
                            return miniMessage.serialize(
                                SignManagerProvider.get()
                                    .serverPlaceholderProvider
                                    .append(server, "<$innerKey>")
                            )
                        }

                        placeholder.startsWith("<group_") -> {
                            val innerKey = pattern.find(placeholder)?.groupValues?.get(2) ?: return placeholder
                            val group = SignManagerProvider.get()
                                .controllerApi
                                .getGroups()
                                .getGroupByName(server.group)
                            miniMessage.serialize(
                                SignManagerProvider.get()
                                    .groupPlaceholderProvider
                                    .append(group, "<$innerKey>")
                            )
                        }

                        else -> placeholder
                    }
                } ?: placeholder
            }

            placeholder.startsWith("<env_") -> {
                val innerKey = pattern.find(placeholder)?.groupValues?.get(2) ?: return placeholder
                System.getenv(innerKey) ?: placeholder
            }

            else -> placeholder
        }
    }

    fun resolveAllPlaceholders(line: String, server: Server?): String {
        val pattern = "<(server_|group_|env_)([^>]+)>".toRegex()
        val ruleContext = ServerRuleContext(server)

        return runBlocking {
            pattern.findAll(line).fold(line) { currentLine, matchResult ->
                val placeholder = matchResult.value
                val resolvedValue = resolvePlaceholder(placeholder, ruleContext)
                currentLine.replace(placeholder, resolvedValue)
            }
        }
    }
}