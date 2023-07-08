package javking.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiscordService {
    private static final Pattern mentionUserPattern = Pattern.compile("<@!?([0-9]{8,})>");
    private static final Pattern channelPattern = Pattern.compile("<#!?([0-9]{4,})>");
    private static final Pattern rolePattern = Pattern.compile("<@&([0-9]{4,})>");
    private static final Pattern anyMention = Pattern.compile("<[@#][&!]?([0-9]{4,})>");
    private static final Pattern discordId = Pattern.compile("(\\d{9,})");

    public static boolean isUserMention(String input) {
        return mentionUserPattern.matcher(input).find();
    }

    public static boolean isRoleMention(String input) {
        return rolePattern.matcher(input).find();
    }

    public static Role findRole(Guild guild, long id) {
        List<Role> roles = guild.getRoles();
        return roles.parallelStream().filter(role -> role.getIdLong() == id).collect(Collectors.toList()).get(0);
    }

    //    unadvised for duplicate role names
    public static Role findRole(Guild guild, String name) {
        List<Role> roles = guild.getRoles();
        return roles.parallelStream().filter(role -> role.getName().equals(name)).collect(Collectors.toList()).get(0);
    }

    /**
     * Converts any mention to an id
     *
     * @param mention the mention to strip
     * @return a stripped down version of the mention
     */
    public static long mentionToId(String mention) {
        Matcher matcher = anyMention.matcher(mention);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return 0L;
    }
}
