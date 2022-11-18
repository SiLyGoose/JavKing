package javking.models.meta;

import javking.JavKing;
import javking.discord.listeners.ConfirmationListener;
import javking.models.command.CommandContext;
import javking.models.guild.GuildContext;
import javking.models.guild.property.GuildProperties;
import javking.models.guild.property.GuildSpecification;
import javking.templates.EmbedTemplate;
import javking.templates.Templates;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractSettingsCommand extends AbstractCommand {
    private EmbedBuilder embedBuilder;

    public AbstractSettingsCommand() {
        super.setRequiresAdmin(true);
    }

//    causes NullPointerException because context not defined until command is used
    private void initEmbed(String identifier) {
        embedBuilder = new EmbedTemplate().setTitle(getContext().getJda().getSelfUser().getName() + " Settings")
                .setDescription(String.format("Use `%s%s %s` to view more information about an option",
                        getContext().getGuildContext().getPrefix(), identifier,
                        getUsage()[0]));
    }

    private void confirmReset(CommandContext context) {
        JavKing instance = JavKing.get();

        // check perms and then add event listener
        if (!context.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            getMessageService().sendBold(Templates.command.no_entry.formatFull("ADMIN required to reset settings."), context.getChannel());
            return;
        }
        CompletableFuture<Message> messageCompletableFuture = getMessageService()
                .sendBold(Templates.command.warning.formatFull("You are about to reset all " +
                                context.getJda().getSelfUser().getName() + "'s settings to default. Continue? (yes/no)"),
                        context.getChannel());

        messageCompletableFuture.whenComplete((message, throwable) -> {
            ConfirmationListener confirmationListener = new ConfirmationListener(instance.getExecutionQueueManager(),
                    instance.getGuildManager(), getMessageService(), context.getUserContext(), message);

            instance.getShardManager().addEventListener(confirmationListener);
        });
    }

    protected void handleDefault(String identifier, GuildContext guildContext) {
        initEmbed(identifier);

        buildSetup(identifier, embedBuilder, guildContext);
        getMessageService().send(embedBuilder, getContext().getChannel());
    }

    protected void handleGuildSpecifications(String[] args, String identifier, GuildContext guildContext) throws IndexOutOfBoundsException {
        if (args.length < 1) {
            handleDefault(identifier, guildContext);
            return;
        }

        CommandContext context = getContext();
        GuildSpecification guildSpecification = guildContext.getGuildSpecification();
        String propertyName = args[0];

//        identifier is passed to maintain 'settings'
        initEmbed(identifier);

        if (propertyName.equals("reset")) {
            confirmReset(context);
        } else {
            GuildProperties property = guildSpecification.getEnumProperty(propertyName);

            if (args.length > 1) {
                String value = args[1];
                guildSpecification.setGuildProperty(propertyName, value);

                getMessageService().sendBold(
                        Templates.command.blue_check_mark.formatFull(
                                String.format("%s updated to %s", smartCapitalize(propertyName), value)), context.getChannel());
            } else {
                boolean exception = propertyName.equals("setdj");

                String title = String.format("%s Settings - %s %s",
                        context.getJda().getSelfUser().getName(), property.getIcon(),
                        smartCapitalize(property.getPropertyType().typeName()));
                String update = String.format("%s%s %s [valid setting]",
                        guildContext.getPrefix(), identifier,
                        String.join("", property.getPropertyType().typeName().split("-")));

                String validProperty = property.getValidProperty();
                String currentProperty;
                if (exception) {
                    try {
                        currentProperty = guildContext.getDjRole().getAsMention();
                    } catch (IndexOutOfBoundsException ignored) {
                        currentProperty = lineCodeBlock("None Set");
                    }
                } else currentProperty = lineCodeBlock(guildSpecification.getOrDefault(propertyName));

                embedBuilder.setTitle(title)
                        .setDescription(property.getDescription())
                        .addField(Templates.command.check_mark.formatFull("Current Setting:"),
                                currentProperty, false)
                        .addField(Templates.command.pencil.formatFull("Update:"),
                                lineCodeBlock(update), false)
                        .addField(Templates.command.blue_check_mark.formatFull("Valid Settings:"),
                                lineCodeBlock(validProperty), false);

                getMessageService().send(embedBuilder, context.getChannel());
            }
        }
    }

    private void buildSetup(String identifier, EmbedBuilder embedBuilder, GuildContext guildContext) {
        for (GuildProperties guildProperty : GuildProperties.values()) {
            String[] split = guildProperty.name().split("_");

            String name = String.format("%s %s", guildProperty.getIcon(),
                    smartCapitalize(String.join("-", split)));
            String value = lineCodeBlock(String.format("%s%s %s", guildContext.getPrefix(), identifier,
                    String.join("", split).toLowerCase()));

            embedBuilder.addField(name, value, true);
        }
    }

    private String smartCapitalize(String text) {
        StringBuilder builder = new StringBuilder();
        for (String s : text.split("-")) {
            builder.append(s.substring(0, 1).toUpperCase()).append(s.substring(1).toLowerCase()).append(" ");
        }
        return builder.toString().replaceAll("[DdJj]{2}", "DJ").trim();
    }

    private String lineCodeBlock(String text) {
        return "`" + text + "`";
    }
}
