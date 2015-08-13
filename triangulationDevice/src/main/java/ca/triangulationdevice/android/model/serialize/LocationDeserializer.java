package ca.triangulationdevice.android.model.serialize;

import android.location.Location;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class LocationDeserializer extends JsonDeserializer<Location> {
    @Override
    public Location deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        String provider = node.get("provider").asText();
        double longitude = node.get("longitude").asDouble();
        double latitude = node.get("latitude").asDouble();
        float accuracy = (float) node.get("accuracy").asDouble();

        Location location = new Location(provider);
        location.setLongitude(longitude);
        location.setLatitude(latitude);
        location.setAccuracy(accuracy);

        return location;
    }
}
