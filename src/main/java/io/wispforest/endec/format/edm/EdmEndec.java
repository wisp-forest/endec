package io.wispforest.endec.format.edm;

import com.google.common.io.ByteStreams;
import io.wispforest.endec.*;

import java.io.IOException;

public class EdmEndec implements Endec<EdmElement<?>> {

    public static final EdmEndec INSTANCE = new EdmEndec();

    public static final Endec<EdmMap> MAP = INSTANCE.xmap(EdmElement::asMap, edmMap -> edmMap);
    
    private EdmEndec() {}

    @Override
    public void encode(Serializer<?> serializer, EdmElement<?> value) {
        if (serializer.has(DataToken.SELF_DESCRIBING)) {
            new EdmDeserializer(value).readAny(serializer);
            return;
        }

        try {
            var output = ByteStreams.newDataOutput();
            EdmIo.encode(output, value);

            serializer.writeBytes(output.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode EDM element in EdmEndec", e);
        }
    }

    @Override
    public EdmElement<?> decode(Deserializer<?> deserializer) {
        if (deserializer instanceof SelfDescribedDeserializer<?> selfDescribedDeserializer) {
            var nativeSerializer = new EdmSerializer();
            selfDescribedDeserializer.readAny(nativeSerializer);

            return nativeSerializer.result();
        }

        try {
            return EdmIo.decode(ByteStreams.newDataInput(deserializer.readBytes()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse EDM element in EdmEndec", e);
        }
    }

}
