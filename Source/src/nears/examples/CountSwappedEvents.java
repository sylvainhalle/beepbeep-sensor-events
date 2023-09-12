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

public class CountSwappedEvents
{

	public static void main(String[] args) throws FileSystemException, IOException
	{
		FileSystem fs = new HardDisk("/home/sylvain/domus-capteurs").open();
		InputStream is = fs.readFrom("nears-hub-0034.json");
		
		JsonFeeder feeder = new JsonFeeder(is);
		
		ApplyFunction get_ts = new ApplyFunction(new FunctionTree(DateToTimestamp.instance, new FunctionTree(StringValue.instance, new JPathFunction("sentAt/$date"))));
		connect(feeder, get_ts);
		Fork f1 = new Fork(2);
		connect(get_ts, f1);
		ApplyFunction gt = new ApplyFunction(Numbers.isLessOrEqual);
		connect(f1, 0, gt, 0);
		Trim t = new Trim(1);
		connect(f1, 1, t, 0);
		connect(t, 0, gt, 1);
		ApplyFunction ite = new ApplyFunction(new FunctionTree(IfThenElse.instance, StreamVariable.X, new Constant(0), new Constant(1)));
		connect(gt, ite);
		Cumulate sum = new Cumulate(Numbers.addition);
		connect(ite, sum);
		Pump p = new Pump();
		connect(sum, p);
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
