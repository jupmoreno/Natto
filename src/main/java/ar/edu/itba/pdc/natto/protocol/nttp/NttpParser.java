package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.Parser;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

public class NttpParser implements Parser<StringBuilder> {

    private ByteBuffer retBuffer = ByteBuffer.allocate(10000);
    private StringBuilder ret = new StringBuilder();

    private static final int MAX_SIZE = 500;

    boolean foundCommand = false;
    boolean tooBig = false;
    private StringBuilder commands = new StringBuilder();


    public StringBuilder fromByteBuffer(ByteBuffer buffer) {
        int originalPosition = buffer.position();
        int moved = 0;
        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer);

        if (!charBuffer.hasRemaining()) {
            return null;
        }

        char current = '0';

        if (tooBig) {
            while (charBuffer.hasRemaining() && (current = charBuffer.get()) != '\n') {
                moved++;
            }

            if (!charBuffer.hasRemaining()) {
                if (current == '\n') {
                    foundCommand = true;
                    tooBig = false;
                }
                return null;
            }

            moved++;

            tooBig = false;
            foundCommand = true;
        }

        if (foundCommand) {
            ret.setLength(0);
            foundCommand = false;
        }


        while (charBuffer.hasRemaining() && (current = charBuffer.get()) != '\n') {
            ret.append(current);
            moved++;
            System.out.println(buffer);

            if (ret.length() > MAX_SIZE) {
                handleTooBig();
                System.out.println("Devuelvo " + ret);
                buffer.position(originalPosition + moved);
                return ret;
            }
        }

        if (current == '\n') {
            foundCommand = true;
            System.out.println("Devuelvo " + ret);
            buffer.position(originalPosition + moved + 1);
            return ret;
        }

        System.out.println("Devuelvooo null");
        buffer.position(originalPosition + moved);
        return null;

    }

    private void handleTooBig() {
        ret.setLength(0);
        tooBig = true;
        ret.append("\nerror\n");

    }

    public ByteBuffer toByteBuffer(StringBuilder message) {
        return ByteBuffer.wrap(message.toString().getBytes());
    }
}
