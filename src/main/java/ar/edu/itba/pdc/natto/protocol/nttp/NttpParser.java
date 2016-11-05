package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.Parser;

import java.nio.ByteBuffer;
import java.util.Queue;

/**
 * Created by user on 05/11/16.
 */
public class NttpParser implements Parser<StringBuilder>{

    ByteBuffer currBuffer = ByteBuffer.allocate(10000);
    StringBuilder ret = new StringBuilder();


    @Override
    public StringBuilder fromByteBuffer(ByteBuffer buffer) {

        ret.setLength(0);
        currBuffer.put(buffer);
        char curr;

        while(currBuffer.hasRemaining()){
            if((curr = currBuffer.getChar()) == '\n'){
                currBuffer.compact();
                return ret;
            }else{
                ret.append(curr);
            }
        }
        currBuffer.compact();
        return null;
    }

    @Override
    public ByteBuffer toByteBuffer(StringBuilder message) {
        return ByteBuffer.wrap(message.toString().getBytes());
    }
}
