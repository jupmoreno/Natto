package ar.edu.itba.pdc.natto.protocol.xmpp.models;

import java.util.*;

/**
 * Created by user on 25/10/16.
 */
public class Tag {

    private StringBuilder name = new StringBuilder();
    private StringBuilder prefix = new StringBuilder();
    private StringBuilder namespace = new StringBuilder();
    private boolean isEmptyTag;
    private Map<StringBuilder, StringBuilder> attributes = new HashMap<>();
    private List<Tag> tags = new ArrayList<>();
    private StringBuilder value = new StringBuilder();
    private boolean modified = false;
    private boolean tooBig = false;

    public Tag(String name, boolean isEmptyTag){
        this.name.append(name);
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
        attributes.put(new StringBuilder(name), new StringBuilder(value));
    }

    public void addTag(Tag tag){
        tags.add(tag);
    }

    public void addNamespace(String namespace){
        this.namespace.append(namespace);
    }

    public Map<StringBuilder, StringBuilder> getAttributes() {
        return attributes;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public StringBuilder getName() {
        return name;
    }

    public boolean isEmptyTag() {
        return isEmptyTag;
    }

    public StringBuilder getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value.append(value);
    }

    public String toString(){
        StringBuilder ret = new StringBuilder();

        ret.append("<");

        if(prefix.length() != 0){
            ret.append(prefix).append(":");
        }

        ret.append(name);
        for(Map.Entry<StringBuilder, StringBuilder> entry : attributes.entrySet()){
            ret.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }

        if(namespace.length() != 0){
            ret.append(" xmlns:").append(prefix).append("=\"").append(namespace).append("\"");
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

        if(prefix.length() != 0){
            ret.append("</").append(prefix).append(":").append(name).append(">");
        }else{
            ret.append("</").append(name).append(">");
        }

        return ret.toString();
    }

    public void setPrefix(String prefix) {
        this.prefix.append(prefix);
    }

    public StringBuilder getPrefix() {
        return prefix;
    }

    public StringBuilder getNamespace() {
        return namespace;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isTooBig() {
        return tooBig;
    }

    public void setTooBig(boolean tooBig) {
        this.tooBig = tooBig;
    }

    public StringBuilder getAttribute(String attributeName){
        for(Map.Entry<StringBuilder, StringBuilder> e: getAttributes().entrySet()){
            if(e.getKey().toString().equals(attributeName)){
                return e.getValue();
            }
        }
        return null;
    }

    public StringBuilder getTagContent(String tagName){
        for(Tag t : this.getTags()){
            if(t.getName().equals(tagName))
                return t.getValue();
        }
        return null;
    }

    

}
