package javking.rest.payload.uuid;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.UUID;

public class UUIDDeserializer extends JsonDeserializer<UUID> {
    @Override
    public UUID deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        String token = parser.getText();
        return UUID.fromString(token);
    }
}
