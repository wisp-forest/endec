package io.wispforest.endec;

import io.wispforest.endec.format.edm.EdmDeserializer;
import io.wispforest.endec.format.edm.EdmIo;
import io.wispforest.endec.format.edm.EdmSerializer;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;

import java.io.File;
import java.io.IOException;

public class Test {

    public static void main(String[] args) {
        var endec = ReflectiveEndecBuilder.INSTANCE.get(TestRecord.class);

        TestRecord data;

        var file = new File("test.txt");

        if(file.exists()) {
            try {
                data = endec.decodeFile(file.toPath(), EdmIo::fileReader);

                System.out.println("Data file loaded!");
                System.out.println(data);
            } catch (IOException e) {
                System.out.println("Unable to read the test file!");
                e.printStackTrace();

                return;
            }
        } else {
            data = new TestRecord(0, 0, "0");
        }

        try {
            var newData = new TestRecord(data.int1 + 1, data.int2 + 1, data.string1 + "1");

            endec.encodeFile(file.toPath(), EdmIo::fileWriter, newData);
        } catch (IOException e) {
            System.out.println("Unable to write the test file!");
            e.printStackTrace();
        }
    }

    public record TestRecord(int int1, int int2, String string1){}
}
