package com.rmal30.auweatherbom.models;

import java.util.List;

public class Observation{
    public String currentTemp, apparentTemp, humidity, wind, rainfall, maxTemp, minTemp;
    public Observation(){
        this.currentTemp = "";
        this.apparentTemp = "";
        this.humidity = "";
        this.wind = "";
        this.rainfall = "";
        this.maxTemp = "";
        this.minTemp = "";
    }

    public Observation(List<Tree> info) {
        String units;
        for (Tree m : info) {
            units = m.properties.get("units");
            if (units == null) {
                units = "";
            }
            switch (m.properties.get("type")) {
                case "apparent_temp":
                    this.apparentTemp = "Feels like " + m.value + "°C";
                    break;
                case "air_temperature":
                    this.currentTemp = m.value;
                    break;
                case "rel-humidity":
                    this.humidity = m.value + units + " humidity\n";
                    break;
                case "wind_dir":
                    this.wind = "Wind: " + m.value + " " + units;
                    break;
                case "wind_spd_kmh":
                    if (!m.value.equals("0")) {
                        this.wind += m.value + " " + units;
                    }
                    break;
                case "rainfall":
                    if (!m.value.equals("0.0")) {
                        this.rainfall = m.value + " " + units + " rain\n";
                    }
                    break;
                case "maximum_air_temperature":
                    this.maxTemp = m.value;
                    break;
                case "minimum_air_temperature":
                    this.minTemp = m.value;
                    break;
            }
            if (this.minTemp == null) {
                this.minTemp = "--";
            }
            if (this.maxTemp == null) {
                this.maxTemp = "--";
            }
        }
    }
}