package app.simplecloud.plugin.sign.paper.sender

import app.simplecloud.plugin.sign.paper.PaperSignsPluginBootstrap
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.incendo.cloud.SenderMapper

@Suppress("UnstableApiUsage")
class PaperCommandSenderMapper(
    private val bootstrap: PaperSignsPluginBootstrap
) : SenderMapper<CommandSourceStack, PaperCommandSender> {

    override fun map(base: CommandSourceStack): PaperCommandSender =
        PaperCommandSender(base)

    override fun reverse(mapped: PaperCommandSender): CommandSourceStack =
        mapped.sourceStack


}