package su.knst.moneysaver.utils.config;

import java.nio.ByteBuffer;

public interface BinaryObject {
    ByteBuffer save();
    void load(ByteBuffer savedData);
    int size();
}
