package javking.models.guild.property.properties;

import javking.models.guild.IGuildPropertyType;
import javking.util.PropertiesLoadingService;
import net.dv8tion.jda.api.entities.Guild;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VolumeProperty implements IGuildPropertyType {
    private final List<Integer> acceptedVolume;

    public VolumeProperty() {
        int maxVolume = Integer.parseInt(PropertiesLoadingService.requireProperty("MAX_VOLUME"));
        int minVolume = Integer.parseInt(PropertiesLoadingService.requireProperty("MIN_VOLUME"));

        acceptedVolume = IntStream.rangeClosed(minVolume, maxVolume).boxed().collect(Collectors.toList());
    }

    @Override
    public String typeName() {
        return "volume";
    }

    @Override
    public boolean validate(Guild guild, String value) {
        return acceptedVolume.stream().anyMatch(volume -> volume.equals(Integer.parseInt(value)));
    }

    @Override
    public String fromInput(Guild guild, String value) {
        return validate(guild, value) ? value : "0";
    }

    @Override
    public String toDisplay(Guild guild, String value) {
        return fromInput(guild, value);
    }
}
