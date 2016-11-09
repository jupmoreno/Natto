package ar.edu.itba.pdc.natto.protocol.xmpp;

import java.nio.ByteBuffer;

/**
 * Created by user on 26/10/16.
 */
public class XmppProtocol implements Protocol<ByteBuffer>{

    //variable booleana para saber si ya ha tomado lugar la negociacion, si ir al xmpp parser or al negotiator
    private boolean isVerified = false;

    @Override
    public ByteBuffer process(ByteBuffer message) {
        //TODO: ver si el usuario esta silenciado


        if(isVerified){
         //   return parser(message);
        }else{
       //     negotiator message
        }

        return message;
    }


}


