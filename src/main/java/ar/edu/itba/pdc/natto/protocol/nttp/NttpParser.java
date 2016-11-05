package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.Parser;

import java.nio.ByteBuffer;

/**
 * Created by user on 05/11/16.
 */
public class NttpParser implements Parser<StringBuilder>{


    @Override
    public StringBuilder fromByteBuffer(ByteBuffer buffer) {
        if(buffer == null)
            return null;

        return null;
    }

    @Override
    public ByteBuffer toByteBuffer(StringBuilder message) {
        return null;
    }
}
