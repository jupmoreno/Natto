package ar.edu.itba.pdc.natto.protocol.xmpp.models;

import java.util.*;

/**
 * Created by user on 25/10/16.
 */
public class Tag {

    String name;
    boolean isEmptyTag;
    Map<String,String> attributes = new HashMap<>();
    List<Tag> tags = new ArrayList<>();
    String value;

    public Tag(String name, boolean isEmptyTag){
        this.name = name;
        this.isEmptyTag = isEmptyTag;
    }

    public boolean isMessage(){
        return false;
    }

    public boolean isIq(){
        return false;
    }

    public boolean isPresence(){
        return false;
    }

    public void addAttribute(String name, String value){
        attributes.put(name, value);
    }

    public void addTag(Tag tag){
        tags.add(tag);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public String getName() {
        return name;
    }

    public boolean isEmptyTag() {
        return isEmptyTag;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String toString(){
        StringBuilder ret = new StringBuilder();

        ret.append("<").append(name);
        for(Map.Entry<String, String> entry : attributes.entrySet()){
            ret.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }

        if(isEmptyTag){
            ret.append("/>");
            return ret.toString();
        }

        ret.append(">");

        if(value != null)
            ret.append(value);

        for(Tag t : tags){
            ret.append(t);
        }

        ret.append("</").append(name).append(">");

        return ret.toString();
    }


}
