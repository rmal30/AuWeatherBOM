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

    public static Observation fromXML(List<XMLTree> info) {
        Observation observation = new Observation();
        String units;
        for (XMLTree m : info) {
            units = m.getAttributes().get("units");
            if (units == null) {
                units = "";
            }
            switch (m.getAttributes().get("type")) {
                case "apparent_temp":
                    observation.apparentTemp = "Feels like " + m.getValue() + "°C";
                    break;
                case "air_temperature":
                    observation.currentTemp = m.getValue();
                    break;
                case "rel-humidity":
                    observation.humidity = m.getValue() + units + " humidity\n";
                    break;
                case "wind_dir":
                    observation.wind = "Wind: " + m.getValue() + " " + units;
                    break;
                case "wind_spd_kmh":
                    if (!m.getValue().equals("0")) {
                        observation.wind += m.getValue() + " " + units;
                    }
                    break;
                case "rainfall":
                    if (!m.getValue().equals("0.0")) {
                        observation.rainfall = m.getValue() + " " + units + " rain\n";
                    }
                    break;
                case "maximum_air_temperature":
                    observation.maxTemp = m.getValue();
                    break;
                case "minimum_air_temperature":
                    observation.minTemp = m.getValue();
                    break;
            }
            if (observation.minTemp == null) {
                observation.minTemp = "--";
            }
            if (observation.maxTemp == null) {
                observation.maxTemp = "--";
            }
        }
        return observation;
    }
}