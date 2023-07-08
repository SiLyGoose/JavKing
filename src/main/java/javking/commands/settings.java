package javking.commands;

import javking.JavKing;
import javking.discord.listeners.ConfirmationListener;
import javking.exceptions.UserException;
import javking.models.command.CommandContext;
import javking.models.guild.GuildContext;
import javking.models.guild.property.GuildProperties;
import javking.models.guild.property.GuildSpecification;
import javking.models.meta.AbstractCommand;
import javking.models.meta.AbstractSettingsCommand;
import javking.templates.EmbedTemplate;
import javking.templates.Templates;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import java.util.concurrent.CompletableFuture;

public class settings extends AbstractSettingsCommand {

    public settings() {
        super();
    }

    @Override
    public String[] getAlias() {
        return new String[]{"s"};
    }

    @Override
    public String[] getUsage() {
        return new String[]{"[property]"};
    }

    @Override
    public void execute(CommandContext context) throws Exception {
        String[] args = getCommandBody();
        GuildContext guildContext = context.getGuildContext();
        String identifier = getClass().getSimpleName();

        try {
            handleGuildSpecifications(args, identifier, guildContext);
        } catch (IndexOutOfBoundsException ignored) {
            handleDefault(identifier, guildContext);
        }
    }

    @Override
    public void onFailed() {

    }

    @Override
    public void onSuccess() {

    }
}
