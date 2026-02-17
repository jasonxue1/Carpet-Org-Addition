package boat.carpetorgaddition.rule.value;

public enum IgnoreChannelingConditions {
    FALSE,
    IGNORE_WEATHER,
    IGNORE_WEATHER_AND_SKY;

    public boolean isIgnoreWeather() {
        return this == IGNORE_WEATHER || this == IGNORE_WEATHER_AND_SKY;
    }

    public boolean isIgnoreSky() {
        return this == IGNORE_WEATHER_AND_SKY;
    }
}
