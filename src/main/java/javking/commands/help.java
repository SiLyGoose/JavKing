package javking.commands;

import javking.JavKing;
import javking.commands.manager.CommandManager;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.templates.EmbedTemplate;
import javking.util.PropertiesLoadingService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.SelfUser;

import java.util.Calendar;

public class help extends AbstractCommand {

    @Override
    public String getDescription() {
        return "displays the full list of commands";
    }

    @Override
    public String[] getAlias() {
        return new String[0];
    }

    @Override
    public String[] getUsage() {
        return new String[]{"[command]"};
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        String[] args = context.getMessage().getContentRaw().substring(context.getGuildContext().getPrefix().length()).trim().split("\\s+");
        CommandManager commandManager = JavKing.get().getCommandManager();

        if (args.length > 1) {
            args[1] = args[1].toLowerCase();

            AbstractCommand command = commandManager.getCommands().get(args[1]);
            if (command == null) command = commandManager.getAliases().get(args[1]);

            command.setContext(context);
            command.setIdentifier(args[1]);
            getMessageService().send(command.buildHelpEmbed(), context.getChannel());
        } else {
            StringBuilder builder = new StringBuilder();
            for (String command : commandManager.getCommands().keySet()) {
                if (commandManager.getCommands().get(command).requiresExecAdmin() || command.equalsIgnoreCase("seekbeginning")) continue;
                builder.append("[»] ").append(command).append("\n");
            }

            SelfUser JavKing = context.getJda().getSelfUser();

            EmbedBuilder embedBuilder = new EmbedTemplate()
                    .setAuthor(String.format("%s Help { JDA v%s - JVM v%s }",
                            JavKing.getName(), JDAInfo.VERSION_MAJOR, System.getProperty("java.version")),
                            JavKing.getEffectiveAvatarUrl())
                    .setTitle("\uD83D\uDD20 Commands")
                    .setDescription(String.format("[JavKing Site](%s)```ini\n%s```", PropertiesLoadingService.requireProperty("BOT_SITE"), builder))
                    .setThumbnail(JavKing.getEffectiveAvatarUrl())
                    .setFooter(String.format("%s©️ from 2020 - %s", JavKing.getName(), Calendar.getInstance().get(Calendar.YEAR)));

            getMessageService().send(embedBuilder, context.getChannel());
        }
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
