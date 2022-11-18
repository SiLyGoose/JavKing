package javking.commands;

import javking.JavKing;
import javking.commands.manager.CommandManager;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.templates.Templates;
import net.dv8tion.jda.api.entities.Message;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class clean extends AbstractCommand {

    @Override
    public String getDescription() {
        return "clears bot's messages";
    }

    @Override
    public String[] getAlias() {
        return new String[0];
    }

    @Override
    public String[] getUsage() {
        return new String[0];
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        List<Message> messageList = context.getChannel().getHistory().retrievePast(100).complete();
        List<Message> toDelete = new ArrayList<>();
//        Iterator<Message> iterator = messageList.iterator();

        String prefix = context.getGuildContext().getPrefix();

        CommandManager commandManager = JavKing.get().getCommandManager();
//        Message nextMessage;
        messageList.parallelStream().forEach(message -> {
            OffsetDateTime dateTime = OffsetDateTime.now();
            dateTime = dateTime.minusWeeks(2);
            if (message.getTimeCreated().isBefore(dateTime)) {
                return;
            }

            String content = message.getContentDisplay().toLowerCase().trim();
            if (content.startsWith(prefix)) {
                String identifier = content.substring(prefix.length());
                identifier = identifier.contains(" ") ? identifier.split("\\s+")[0] : identifier;
                if (commandManager.getCommands().containsKey(identifier) || commandManager.getAliases().containsKey(identifier)) {
                    toDelete.add(message);
                }
            }

            if (message.getAuthor().getId().equals(context.getJda().getSelfUser().getId())) {
                toDelete.add(message);
            }
        });
//        while (iterator.hasNext()) {
//            nextMessage = iterator.next();
//
//            String nextMessageContent = nextMessage.getContentDisplay().toLowerCase().trim();
//            if (nextMessageContent.startsWith(prefix)) {
//                String identifier = nextMessageContent.substring(prefix.length());
//                identifier = identifier.contains("\\s+") ? identifier.split("\\s+")[0] : identifier;
//                if (!commandManager.getCommands().containsKey(identifier) && !commandManager.getAliases().containsKey(identifier)) {
//                    System.out.println("removeL: " + nextMessageContent);
//                    iterator.remove();
//                    continue;
//                }
//            }
//            if (!nextMessage.getAuthor().getId().equals(PropertiesLoadingService.requireProperty("ID"))) {
//                System.out.println("remove: " + nextMessage.getContentDisplay());
//                iterator.remove();
//            }
//        }

//        List<Message> prunedList = Lists.newArrayList(iterator);
        if (toDelete.size() > 1) {
            context.getChannel().purgeMessages(toDelete);
            getMessageService().sendBoldTemporary(Templates.command.check_mark.formatFull(String.format("Cleared `%d` messages!", toDelete.size())), context.getChannel());
        } else getMessageService().sendBoldTemporary(Templates.command.x_mark.formatFull("No messages to clear!"), context.getChannel());
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
