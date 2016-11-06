package ar.edu.itba.pdc.natto.protocol.xmpp;

import ar.edu.itba.pdc.natto.protocol.Parser;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;

// TODO: Fijarse de siempre cerrar bien el parser anterior!
public class XmppParser implements Parser<ByteBuffer> {

    private final static int BUFFER_MAX_SIZE = 10000;

    private AsyncXMLInputFactory inputF = new InputFactoryImpl();
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> parser = inputF.createAsyncForByteBuffer();


    private XmppData xmppData;

    private boolean inMessage = false;
    private boolean inBody = false;


    ByteBuffer retBuffer = ByteBuffer.allocate(BUFFER_MAX_SIZE);
    StringBuilder sb = new StringBuilder();


    public XmppParser(XmppData data){
        this.xmppData = data;
    }

    @Override
    public ByteBuffer fromByteBuffer(ByteBuffer buffer) {

        if (buffer == null) {
            return null;
        }


        if(parser.getInputFeeder().needMoreInput()){
            try {
                parser.getInputFeeder().feedInput(buffer);
                retBuffer.clear();

            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
        }

        // Aca empieza la etapa de parseo
        try {
            while (parser.hasNext()) {
                switch (parser.next()) {
                    case AsyncXMLStreamReader.START_DOCUMENT:
                        System.out.println("start document");
                        handleStartDocument();
                        break;

                    case AsyncXMLStreamReader.START_ELEMENT:
                        System.out.println("start element " + parser.getName());
                        handleStartElement();
                        break;

                    case AsyncXMLStreamReader.CHARACTERS:
                        System.out.println("Character: " + parser.getText());
                        handleCharacters();
                        break;

                    case AsyncXMLStreamReader.END_ELEMENT:
                        System.out.println("End element: " + parser.getName());
                        handleEndElement();
                        break;

                    case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                        System.out.println("Incomplete!");
                        System.out.println("devuelvo sb " + sb);
                     //   buffer.clear();
                        ByteBuffer ret = ByteBuffer.wrap(sb.toString().getBytes());
                        sb.setLength(0);
                        return ret;

                    case AsyncXMLStreamReader.PROCESSING_INSTRUCTION:
                        System.out.println("PROCESSING instruction");
                        break;

                    default:
                        break;
                }
            }
        } catch (XMLStreamException e) {
            return handleWrongFormat();

        }
        System.out.println("porque devuelvo aca? ojo");
        ByteBuffer ret = ByteBuffer.wrap(sb.toString().getBytes());
        sb.setLength(0);
        return ret;
    }

    private void handleStartDocument() {

        sb.append("<?xml ");
        String version = parser.getVersion();
        String encoding = parser.getCharacterEncodingScheme();
        if(version != null)
            sb.append("version=\"" + version + "\" ");
        if(encoding != null)
            sb.append("encoding=\"" + encoding + "\"");
        sb.append("?>");

    }

    private void handleStartElement(){
        String name = parser.getName().getLocalPart().toString();
        if(name.equals("message")){
            inMessage = true;
        }else if(name.equals("body") && inMessage){
            inBody = true;
        }

        sb.append("<");
        if(parser.getPrefix().length() != 0){
            sb.append(parser.getPrefix()).append(":");
        }
        sb.append(name);


        for(int i = 0; i < parser.getAttributeCount(); i++){
            sb.append(" ");
            if (!parser.getAttributePrefix(i).isEmpty()) {
                sb.append(parser.getAttributePrefix(i)).append(":");
            }
            sb.append(parser.getAttributeLocalName(i)).append("=\"").append(parser.getAttributeValue(i)).append("\"");
        }


        for(int i = 0; i < parser.getNamespaceCount(); i++){
            sb.append(" ").append("xmlns");
            if (!parser.getNamespacePrefix(i).isEmpty()) {
                sb.append(":").append(parser.getNamespacePrefix(i));
            }

            sb.append("=\"").append(parser.getNamespaceURI(i)).append("\"");
        }


        sb.append(">");
    }

    public void handleCharacters(){

        if(inBody && xmppData.isTransformEnabled()){ //TODO: se leetea?
            for (char c: parser.getText().toCharArray()) {
                switch (c) {
                    case 'a':
                        sb.append("4");
                        break;
                    case 'e':
                        sb.append("3");
                        break;
                    case 'i':
                        sb.append("1");
                        break;
                    case 'o':
                        sb.append("0");
                        break;
                    case 'c':
                        sb.append("&lt;");
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
        }else{
            sb.append(parser.getText());
        }

    }

    private void handleEndElement(){
        sb.append("</");
        if(parser.getPrefix().length() != 0){
            sb.append(parser.getPrefix()).append(":");
        }
        sb.append(parser.getName().getLocalPart()).append(">");


        if(parser.getName().getLocalPart().equals("body"))
            inBody = false;
        if(parser.getName().getLocalPart().equals("message"))
            inMessage = false;

    }

    private ByteBuffer handleWrongFormat(){
        System.out.println("Mal formado");
        sb.setLength(0);
        sb.append("</stream:stream>");

        ByteBuffer ret = ByteBuffer.wrap(sb.toString().getBytes());
        sb.setLength(0);

        return ret;
    }


    @Override
    public ByteBuffer toByteBuffer(ByteBuffer message) {


        retBuffer = ByteBuffer.wrap(sb.toString().getBytes());
        sb.setLength(0);

        return retBuffer;
    }


}