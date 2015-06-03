package com.busit.routing;

import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import java.util.*;
import com.busit.rest.*;

public class Step
{
	public String fromInstance = null;
	public String toInstance = null;
	public String fromInterface = null;
	public String toInterface = null;
	public long sentTime = 0;
	public long handledTime = 0;
	public long processingTime = 0;
	public int costPrice = 0;
	public int costTax = 0;
	public int costShare = 0;
	public int costCount = 0;
	public int costSize = 0;
	public int costQos = 0;
	public long inputSize = 0;
	public long outputSize = 0;
	public String inputHash = null;
	public String outputHash = null;
	public String transaction = null;
	public String shareTransaction = null;
	public String identifier = null;
	
	public Step()
	{
	}
	
	public Step(String json) throws Exception
	{
		this(Json.decode(json));
	}
	
	public Step(Any json)
	{
		// may be null if this is the first step element
		this.fromInstance = json.<String>value("fromInstance");
		// can never be null
		this.toInstance = json.<String>value("toInstance");
		// may be null if this is the first step element
		this.fromInterface = json.<String>value("fromInterface");
		// can never be null
		this.toInterface = json.<String>value("toInterface");
		// can never be null
		this.sentTime = json.<Double>value("sentTime").longValue();
		// may be null (0) if this is the one we process now
		this.handledTime = json.<Double>value("handledTime").longValue();
		// may be null (0) if this is the one we process now
		this.processingTime = json.<Double>value("processingTime").longValue();
		// can never be null but may be zero
		this.costPrice = json.<Double>value("costPrice").intValue();
		// can never be null but may be zero
		this.costTax = json.<Double>value("costTax").intValue();
		// can never be null but may be zero
		this.costShare = json.<Double>value("costShare").intValue();
		// can never be null but may be zero
		this.costCount = json.<Double>value("costCount").intValue();
		// can never be null but may be zero
		this.costSize = json.<Double>value("costSize").intValue();
		// can never be null but may be zero
		this.costQos = json.<Double>value("costQos").intValue();
		// can never be null
		this.inputSize = json.<Double>value("inputSize").longValue();
		// may be null (0) if this is the one we process now
		this.outputSize = json.<Double>value("outputSize").longValue();
		// can never be null
		this.inputHash = json.<String>value("inputHash");
		// may be null if this is the one we process now
		this.outputHash = json.<String>value("outputHash");
		// can never be null
		this.transaction = json.<String>value("transaction");
		// may be null if this is not a shared message
		this.shareTransaction = json.<String>value("shareTransaction");
		// can never be null
		this.identifier = json.<String>value("identifier");		
		
		if( this.toInstance == null || this.toInstance.length() == 0 )
			throw new IllegalArgumentException("'toInstance' is undefined");
		if( this.toInterface == null || this.toInterface.length() == 0 )
			throw new IllegalArgumentException("'toInterface' is undefined");
		if( this.sentTime == 0 )
			throw new IllegalArgumentException("'sentTime' is undefined");
		if( this.inputSize == 0 )
			throw new IllegalArgumentException("'inputSize' is undefined");
		if( this.inputHash == null || this.inputHash.length() == 0 )
			throw new IllegalArgumentException("'inputHash' is undefined");
		if( this.transaction == null || this.transaction.length() == 0 )
			throw new IllegalArgumentException("'transaction' is undefined");
		if( this.costShare > 0 && (this.shareTransaction == null || this.shareTransaction.length() == 0) )
			throw new IllegalArgumentException("'shareTransaction' is undefined");
		if( this.identifier == null || this.identifier.length() == 0 )
			throw new IllegalArgumentException("'identifier' is undefined");

		int check = (this.fromInstance == null || this.fromInstance.length() == 0 ? 0 : 1);
		check += (this.fromInterface == null || this.fromInterface.length() == 0 ? 0 : 1);
		check += (this.handledTime == 0 ? 0 : 1);
		check += (this.processingTime == 0 ? 0 : 1);
		check += (this.outputSize == 0 ? 0 : 1);
		check += (this.outputHash == null || this.outputHash.length() == 0 ? 0 : 1);
		
		if( check != 0 && check != 6 )
			throw new IllegalArgumentException("Missing input data");
	}
	
	public static Step uncheckedParse(String json) throws Exception
	{
		return uncheckedParse(Json.decode(json));
	}
	
	public static Step uncheckedParse(Any json)
	{
		Step s = new Step();

		if( json.containsKey("fromInstance") )
			s.fromInstance = json.<String>value("fromInstance");
		if( json.containsKey("toInstance") )
			s.toInstance = json.<String>value("toInstance");
		if( json.containsKey("fromInterface") )
			s.fromInterface = json.<String>value("fromInterface");
		if( json.containsKey("toInterface") )
			s.toInterface = json.<String>value("toInterface");
		if( json.containsKey("sentTime") )
			s.sentTime = json.<Double>value("sentTime").longValue();
		if( json.containsKey("handledTime") )
			s.handledTime = json.<Double>value("handledTime").longValue();
		if( json.containsKey("processingTime") )
			s.processingTime = json.<Double>value("processingTime").longValue();
		if( json.containsKey("costPrice") )
			s.costPrice = json.<Double>value("costPrice").intValue();
		if( json.containsKey("costTax") )
			s.costTax = json.<Double>value("costTax").intValue();
		if( json.containsKey("costShare") )
			s.costShare = json.<Double>value("costShare").intValue();
		if( json.containsKey("costCount") )
			s.costCount = json.<Double>value("costCount").intValue();
		if( json.containsKey("costSize") )
			s.costSize = json.<Double>value("costSize").intValue();
		if( json.containsKey("costQos") )
			s.costQos = json.<Double>value("costQos").intValue();
		if( json.containsKey("inputSize") )
			s.inputSize = json.<Double>value("inputSize").longValue();
		if( json.containsKey("outputSize") )
			s.outputSize = json.<Double>value("outputSize").longValue();
		if( json.containsKey("inputHash") )
			s.inputHash = json.<String>value("inputHash");
		if( json.containsKey("outputHash") )
			s.outputHash = json.<String>value("outputHash");
		if( json.containsKey("transaction") )
			s.transaction = json.<String>value("transaction");
		if( json.containsKey("shareTransaction") )
			s.shareTransaction = json.<String>value("shareTransaction");
		if( json.containsKey("identifier") )
			s.identifier = json.<String>value("identifier");
			
		return s;
	}
	
	public String toString()
	{
		return "{" + 
			"\"fromInstance\":" + 
				(fromInstance == null ? "null" : "\"" + fromInstance.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"") + "," + 
			"\"toInstance\":" +
				(toInstance == null ? "null" : "\"" + toInstance.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"") + "," + 
			"\"fromInterface\":" + 
				(fromInterface == null ? "null" : "\"" + fromInterface.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"") + "," + 
			"\"toInterface\":" + 
				(toInterface == null ? "null" : "\"" + toInterface.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"") + "," + 
			"\"sentTime\":" + sentTime + "," + 
			"\"handledTime\":" + handledTime + "," + 
			"\"processingTime\":" + processingTime + "," + 
			"\"costPrice\":" + costPrice + "," + 
			"\"costTax\":" + costTax + "," + 
			"\"costShare\":" + costShare + "," + 
			"\"costCount\":" + costCount + "," + 
			"\"costSize\":" + costSize + "," + 
			"\"costQos\":" + costQos + "," + 
			"\"inputSize\":" + inputSize + "," + 
			"\"outputSize\":" + outputSize + "," + 
			"\"inputHash\":" + 
				(inputHash == null ? "null" : "\"" + inputHash.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"") + "," + 
			"\"outputHash\":" + 
				(outputHash == null ? "null" : "\"" + outputHash.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"") + "," +
			"\"transaction\":" + 
				(transaction == null ? "null" : "\"" + transaction.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"") + "," +
			"\"shareTransaction\":" + 
				(shareTransaction == null ? "null" : "\"" + shareTransaction.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"") + "," + 
			"\"identifier\":" + 
				(identifier == null ? "null" : "\"" + identifier.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"") +
			"}";
	}
	
	public Step clone()
	{
		Step t = new Step();
		t.fromInstance = fromInstance;
		t.toInstance = toInstance;
		t.fromInterface = fromInterface;
		t.toInterface = toInterface;
		t.sentTime = sentTime;
		t.handledTime = handledTime;
		t.processingTime = processingTime;
		t.costPrice = costPrice;
		t.costTax = costTax;
		t.costShare = costShare;
		t.costCount = costCount;
		t.costSize = costSize;
		t.costQos = costQos;
		t.inputSize = inputSize;
		t.outputSize = outputSize;
		t.inputHash = inputHash;
		t.outputHash = outputHash;
		t.transaction = transaction;
		t.shareTransaction = shareTransaction;
		t.identifier = identifier;
		return t;
	}
}