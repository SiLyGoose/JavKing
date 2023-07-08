package javking.commands;

import javking.JavKing;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import javking.templates.EmbedTemplate;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.Calendar;

public class aliases extends AbstractCommand {

    @Override
    public String[] getAlias() {
        return new String[]{"al"};
    }

    @Override
    public String[] getUsage() {
        return new String[0];
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();

        JavKing.get().getCommandManager().getCommands().forEach((key, value) -> {
            if (value.getAlias().length > 0) {
                stringBuilder.append("[»] ")
                        .append(key)
                        .append(" - ")
                        .append(String.join(", ", value.getAlias()))
                        .append("\n");
            }
        });

        SelfUser selfUser = getContext().getJda().getSelfUser();

        EmbedBuilder embedBuilder = new EmbedTemplate()
                .setAuthor(String.format("%s Help", selfUser.getName()), selfUser.getEffectiveAvatarUrl())
                .setTitle("\uD83D\uDD24 Aliases")
                .setDescription(String.format("```ini\n%s```", stringBuilder))
                .setFooter(String.format("%s©️ from 2020 - %s", selfUser.getName(), Calendar.getInstance().get(Calendar.YEAR)), selfUser.getEffectiveAvatarUrl());

        getMessageService().send(embedBuilder.build(), context.getUserContext());
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {
        getContext().getMessage().addReaction(Emoji.fromUnicode("\uD83D\uDCEC")).queue();
    }
}
