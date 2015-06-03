import com.busit.*;

public class connector
{
	Connector c2;
	ConnectorHelper ch;
	Consumer cc;
	Factory f2;
	IConnector c;
	IContent c3;
	IFactory f;
	IMessage m;
	IMessageList ml;
	Interface ci;
	Producer cp;
	Transformer ct;
	
	private class test extends Connector implements Transformer, Consumer, Producer
	{
		public boolean test() throws Exception { return true; }
		public IMessage transform(IMessage message, Interface in, Interface out) { return message; }
		public IMessage produce(Interface out) { return Factory.message(); }
		public IMessage sample(Interface out) { return Factory.message(); }
		public void consume(IMessage message, Interface in) { }
	}
}