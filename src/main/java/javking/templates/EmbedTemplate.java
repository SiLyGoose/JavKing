package javking.templates;

import javking.util.PropertiesLoadingService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Calendar;

public class EmbedTemplate extends EmbedBuilder {
    private final EmbedBuilder embedBuilder;

    public EmbedTemplate() {
        embedBuilder = new EmbedBuilder().setColor(Color.decode(PropertiesLoadingService.requireProperty("HEX")));
    }

    public MessageEmbed getEmbedBuilder() {
        return embedBuilder.build();
    }

    public EmbedBuilder clearEmbed() {
        embedBuilder.clear();
        return setColor(Color.decode(PropertiesLoadingService.requireProperty("HEX")))
                .setFooter("JavKing© from 2020 - " + Calendar.getInstance().get(Calendar.YEAR));
    }

    public @NotNull EmbedBuilder addField(String name, String value, boolean inline) {
        return embedBuilder.addField(name, value, inline);
    }

    public EmbedBuilder addField(String name, String value) {
        return addField(name, value, false);
    }

    public @NotNull EmbedBuilder setTitle(String title, String url) {
        return embedBuilder.setTitle(title, url);
    }

    public @NotNull EmbedBuilder setTitle(String title) {
        return embedBuilder.setTitle(title);
    }

    public EmbedBuilder setDescription(@Nullable String description) {
        return embedBuilder.setDescription(description);
    }

    public @NotNull EmbedBuilder setAuthor(@Nullable String name) {
        return setAuthor(name, null, null);
    }

    public @NotNull EmbedBuilder setAuthor(@Nullable String name, @Nullable String iconURI) {
        return setAuthor(name, null, iconURI);
    }

    public @NotNull EmbedBuilder setAuthor(@Nullable String name, @Nullable String uri, @Nullable String iconURI) {
        return embedBuilder.setAuthor(name, uri, iconURI);
    }

    public @NotNull EmbedBuilder setThumbnail(@Nullable String uri) {
        return embedBuilder.setThumbnail(uri);
    }

    public @NotNull EmbedBuilder setColor(int color) {
        return embedBuilder.setColor(color);
    }

    public @NotNull EmbedBuilder setColor(@Nullable Color color) {
        return embedBuilder.setColor(color);
    }

    public EmbedBuilder setColor(String hexColor) {
        return setColor(Color.decode(hexColor));
    }

    public @NotNull EmbedBuilder setFooter(@Nullable String text) {
        return setFooter(text, null);
    }

    public @NotNull EmbedBuilder setFooter(@Nullable String text, @Nullable String iconURI) {
        return embedBuilder.setFooter(text, iconURI);
    }
}
