package com.busit.local;

import com.anotherservice.util.*;
import com.busit.*;
import java.util.*;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

public class Message implements IMessage
{
	// this is just to make sure the user doesnt create a message "by mistake" using the default constructor
	private Message() { }
	public Message(boolean dummy) { }
	
	private Hashtable<String, InputStream> attachments = new Hashtable<String, InputStream>();
	public void addAttachment(String name, String data) { attachments.put(name, new ByteArrayInputStream(Hex.getBytes(data))); }
	public void addAttachment(String name, byte[] data) { attachments.put(name, new ByteArrayInputStream(data)); }
	public void addAttachment(String name, InputStream data) { attachments.put(name, data); }
	public void removeAttachment(String name) { attachments.remove(name); }
	public void clearAttachments() { attachments.clear(); }
	public int countAttachments() { return attachments.size(); }
	public Collection<String> getAttachmentNames() { return this.attachments.keySet(); }
	public InputStream getAttachment(String name) { InputStream is = this.attachments.get(name); try { is.reset(); } catch(Exception e) {} return is; }
	
	private byte[] content = new byte[]{};
	public String getContentUTF8() { try { return new String(this.content, "UTF-8"); } catch(Exception e) { return new String(this.content); } }
	public byte[] getContentBinary() { return content; }
	public void setContentUTF8(String content) { this.content = Hex.getBytes(content); }
	public void setContentBinary(byte[] content) { this.content = content; }
	public void clearContent() { this.content = new byte[]{}; }
	
	private IIdentity identity = null;
	public IIdentity getInputOrigin() { return identity; }
	public IIdentity getInputDestination() { return identity; }
	public void setOutputOrigin(IIdentity identity) throws Exception { }
	public void setOutputDestination(IIdentity identity) { }
	public void hideOriginalAuthor() { }
	
	public String toString(IIdentity from) throws Exception { return "### ENCRYPTED MESSAGE ###"; }
	public String toString(IIdentity from, IIdentity to) throws Exception { return "### ENCRYPTED MESSAGE ###"; }
	public String toString() { return "### ENCRYPTED MESSAGE ###"; }
	
	public IMessage duplicate()
	{
		IMessage m = new Message();
		m.setContentBinary(this.content);
		
		for( String name : attachments.keySet() )
			m.addAttachment(name, getAttachment(name));
		
		return m;
	}
	
	public IType getKnownType() { try { return new KnownType(this.getContentUTF8()); } catch(Exception e) { return null; } }
	public void setKnownType(IType type) { if( type instanceof KnownType ) this.setContentUTF8(type.toJson()); else this.setContentUTF8(new KnownType(type).toJson()); }
}