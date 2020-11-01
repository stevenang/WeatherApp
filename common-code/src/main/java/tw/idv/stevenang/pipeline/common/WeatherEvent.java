package tw.idv.stevenang.pipeline.common;

public class WeatherEvent {

    public String locationName;
    public Double temperature;
    public Long timestamp;
    public Double longitute;
    public Double latitute;

    public WeatherEvent(){
    }

    public WeatherEvent(String locationName, Double temperature,
                        Long timestamp, Double longitute, Double latitute) {
        this.locationName = locationName;
        this.temperature = temperature;
        this.timestamp = timestamp;
        this.longitute = longitute;
        this.latitute = latitute;
    }
}
