package javking.exceptions.handlers;

import javking.JavKing;
import javking.discord.MessageService;
import javking.exceptions.UserException;
import javking.models.command.Command;
import javking.models.command.CommandContext;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class CommandExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Command command;

    public CommandExceptionHandler(Command command) {
        this.command = command;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        CommandContext context = command.getContext();
        MessageChannel messageChannel = context.getChannel();
        String commandName = command.getIdentifier();
        MessageService messageService = JavKing.get().getMessageService();

        if (e instanceof UserException) {
            messageService.send(((UserException) e).buildHelpEmbed(command), messageChannel);
        } else {
            messageService.sendBold(Templates.command.x_mark.formatFull("Exception while handling `CommandContext@" + context.getId() + "` "), messageChannel);
//            logger.error(String.format("Exception while handling command %s on guild %s", commandName, context.getGuild().getName()), e);
        }
    }
}
