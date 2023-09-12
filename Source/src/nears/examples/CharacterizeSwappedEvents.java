package nears.examples;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;

import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.IfThenElse;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.json.JPathFunction;
import ca.uqac.lif.cep.json.StringValue;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.fs.HardDisk;
import nears.DateToTimestamp;
import nears.JsonFeeder;

public class CharacterizeSwappedEvents
{

	public static void main(String[] args) throws FileSystemException, IOException
	{
		FileSystem fs = new HardDisk("/home/sylvain/domus-capteurs").open();
		InputStream is = fs.readFrom("nears-hub-0032.json");
		
		JsonFeeder feeder = new JsonFeeder(is);
		
		ApplyFunction get_ts = new ApplyFunction(new FunctionTree(DateToTimestamp.instance, new FunctionTree(StringValue.instance, new JPathFunction("sentAt/$date"))));
		connect(feeder, get_ts);
		Fork f1 = new Fork(2);
		connect(get_ts, f1);
		ApplyFunction min = new ApplyFunction(Numbers.subtraction);
		connect(f1, 0, min, 0);
		Trim t = new Trim(1);
		connect(f1, 1, t, 0);
		connect(t, 0, min, 1);
		Fork f2 = new Fork();
		connect(min, f2);
		Filter fil = new Filter();
		connect(f2, 0, fil, 0);
		ApplyFunction lt0 = new ApplyFunction(new FunctionTree(Numbers.isLessThan, StreamVariable.X, new Constant(0)));
		connect(f2, 1, lt0, 0);
		connect(lt0, 0, fil, 1);
		Cumulate max = new Cumulate(Numbers.minimum);
		connect(fil, max);
		Pump p = new Pump();
		connect(max, p);
		KeepLast kl = new KeepLast();
		connect(p, kl);
		Print print = new Print();
		connect(kl, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		is.close();
		fs.close();
	}

}
