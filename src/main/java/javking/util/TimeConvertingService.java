package javking.util;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TimeConvertingService {
    public static String millisecondsToHHMMSS(long milliseconds) {
        Duration duration = Duration.ofMillis(milliseconds);
        long seconds = duration.getSeconds();
        long HH = seconds / 3600;
        long MM = (seconds % 3600) / 60;
        long SS = seconds % 60;
        return HH > 0 ? String.format("%02d:%02d:%02d", HH, MM, SS) : String.format("%02d:%02d", MM, SS);
    }

    public static String secondsToHHMMSS(long seconds) {
        return millisecondsToHHMMSS(seconds * 1000L);
    }

    public static long HHMMSStoMilliseconds(String hms) {
        return HHMMSStoSeconds(hms) * 1000;
    }

    public static long HHMMSStoSeconds(String hms) {
        String[] split = hms.split(":");
        List<Integer> intSplit = Arrays.stream(split).map(Integer::parseInt).collect(Collectors.toList());
        if (split.length < 3) intSplit.add(0, 0);

        LocalTime localDateTime = LocalTime.of(intSplit.get(0), intSplit.get(1), intSplit.get(2));
        return localDateTime.toSecondOfDay();
    }
}
