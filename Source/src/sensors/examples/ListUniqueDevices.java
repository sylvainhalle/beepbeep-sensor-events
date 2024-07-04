package sensors.examples;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.json.JPathFunction;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import sensors.JsonFeeder;
import sensors.LogRepository;
import sensors.PrettyPrint;

public class ListUniqueDevices
{

	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		FileSystem fs = new LogRepository().open();
		InputStream is = fs.readFrom("nears-hub-0032.json");
		OutputStream os = fs.writeTo("ListUniqueDevices.txt");
		
		/* Create the pipeline. */
		JsonFeeder feeder = new JsonFeeder(is);
		Pump p = new Pump();
		connect(feeder, p);
		ApplyFunction scal = new ApplyFunction(new FunctionTree(new MergeScalars("location", "subject", "model"), new JPathFunction("location"), new JPathFunction("subject"), new JPathFunction("model")));
		connect(p, scal);
		Sets.PutInto pi = new Sets.PutInto();
		connect(scal, pi);
		KeepLast last = new KeepLast();
		connect(pi, last);
		
		/* Connect the pipeline to an output and run. */
		connect(last, new PrettyPrint(new PrintStream(os)));
		p.run();
		
		/* Clean up. */
		os.close();
		is.close();
		fs.close();
	}

}
