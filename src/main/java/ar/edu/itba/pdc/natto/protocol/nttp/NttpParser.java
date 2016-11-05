package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.Parser;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Queue;

/**
 * Created by user on 05/11/16.
 */
public class NttpParser implements Parser<StringBuilder>{

    ByteBuffer currBuffer = ByteBuffer.allocate(10000);
    ByteBuffer retBuffer = ByteBuffer.allocate(10000);
    StringBuilder ret = new StringBuilder();


    @Override
    public StringBuilder fromByteBuffer(ByteBuffer buffer) {

        ret.setLength(0);
        currBuffer.put(buffer);
        char curr;
        System.out.println(new String(buffer.array(), Charset.forName("UTF-8")));
        CharBuffer charBuffer = buffer.asCharBuffer();
        while(currBuffer.hasRemaining()){
            System.out.println("HOLA 1");
            if((curr = (char) buffer.get()) == '\n'){
                System.out.println("HOLA 2");
                currBuffer.compact();
                System.out.println("HOLA 3");
                return ret;
            }else{
              //  System.out.println("HOLA 4 " + curr);
                ret.append(curr);
            }
        }
        currBuffer.compact();
        return null;

    }

    @Override
    public ByteBuffer toByteBuffer(StringBuilder message) {
        retBuffer.clear();
        retBuffer = ByteBuffer.wrap(message.toString().getBytes());
        return retBuffer;
    }
}
