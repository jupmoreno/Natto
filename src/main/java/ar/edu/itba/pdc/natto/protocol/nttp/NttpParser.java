package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.Parser;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by user on 05/11/16.
 */
public class NttpParser implements Parser<StringBuilder>{

    private ByteBuffer retBuffer = ByteBuffer.allocate(10000);
    private StringBuilder ret = new StringBuilder();

    private static final int MAX_SIZE = 30;

    boolean foundCommand = false;
    private StringBuilder commands = new StringBuilder();


    @Override
    public StringBuilder fromByteBuffer(ByteBuffer buffer) {
        System.out.println("Esto es lo que recibo en el fromByteBuffer: " + new String(buffer.array(), buffer.position(), buffer.limit(), Charset.defaultCharset()));

        if(foundCommand){
            ret.setLength(0);
            foundCommand = false;
        }

        String bufferStr = new String(buffer.array(), buffer.position(), buffer.limit(), Charset.forName("UTF-8"));
        buffer.clear();

        if(commands.length() + bufferStr.length() > MAX_SIZE){
            ret.setLength(0);
            commands.setLength(0);
            ret.append("\nerror\n");
            foundCommand = true;
            return ret;
        }

        commands.append(bufferStr);

        for(int i = 0; i < commands.length() && !foundCommand; i++){
            if(commands.charAt(i) == '\n'){
                foundCommand = true;
                commands.delete(0,i + 1);
            }else{
                ret.append(commands.charAt(i));
            }
        }

        if(foundCommand){
            System.out.println("Desde el fromByteBuffer voy a retornar " + ret);
            return ret;
        }

        return null;


    }

    @Override
    public ByteBuffer toByteBuffer(StringBuilder message) {
        retBuffer.compact();
        retBuffer = ByteBuffer.wrap(message.toString().getBytes());
        return retBuffer;
    }
}
