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
        String ret = "<" + name;
        for(Map.Entry<String, String> entry : attributes.entrySet()){
            ret = ret + " " + entry.getKey() + "=\"" + entry.getValue() +"\"";
        }

        if(isEmptyTag){
            ret += "/>";
            return ret;
        }

        ret += ">";

        ret += value;

        for(Tag t : tags){
            ret += t;
        }

        ret = ret + "</" + name + ">";

        return ret;
    }
}
