//package ar.edu.itba.pdc.natto.proxy;
//
//import static com.google.common.base.Preconditions.checkNotNull;
//
//import ar.edu.itba.pdc.natto.protocol.Parser;
//import ar.edu.itba.pdc.natto.protocol.Protocol;
//import ar.edu.itba.pdc.natto.proxy.handlers.Connection;
//import com.google.common.collect.Maps;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.util.Queue;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
//public class ProtocolTask<T> implements Runnable { // TODO: Callable?
//    private final Parser<T> parser;
//    private final Protocol<T> protocol;
//
//    private final Connection readConnection;
//    private final Connection writeConnection;
//
//    private Queue<ByteBuffer> buffers;
//
//    public ProtocolTask(Connection readConnection, Connection writeConnection, Parser<T> parser,
//                        Protocol<T> protocol) {
//        this.parser = checkNotNull(parser, "Parser can't be null");
//        this.protocol = checkNotNull(protocol, "Protocol can't be null");
//        this.readConnection = checkNotNull(readConnection, "Connection can't be null");
//        this.writeConnection = checkNotNull(writeConnection, "Connection can't be null");
//        this.buffers = new ConcurrentLinkedQueue<>();
//    }
//
//    @Override
//    public void run() { // TODO: Sacar String
//        while (!buffers.isEmpty()) {
//            T request = parser.fromByteBuffer(buffers.remove());
//            if (request != null) {
//                T response = protocol.process(request);
//                if (response != null) {
//                    writeConnection.requestWrite(parser.toByteBuffer(response));
//                } else {
//                    // TODO:
//                }
//            } else {
//                readConnection.requestRead();
//            }
//        }
//    }
//
//    public boolean add(ByteBuffer buffer) {
//        return buffers.offer(buffer);
//    }
//}
