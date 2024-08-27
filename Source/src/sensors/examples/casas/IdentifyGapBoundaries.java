package sensors.examples.casas;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tmf.Window;
import ca.uqac.lif.cep.util.Bags;
import ca.uqac.lif.cep.util.Lists;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.NthElement;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.Flatten;
import sensors.LogRepository;
import sensors.casas.CasasLogRepository;
import sensors.casas.CasasTxtFormat;

import static ca.uqac.lif.cep.Connector.connect;
import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;

public class IdentifyGapBoundaries
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new CasasLogRepository();
	
	/* The adapter for the event format. */
	protected static final EventFormat format = new CasasTxtFormat();

	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("casas-rawdata.txt");
		OutputStream os = fs.writeTo("gap-boundaries.txt");
		Processor feeder = format.getFeeder(is);
		
		Slice slice = new Slice(format.sensorId(), new GroupProcessor(1, 1) {{
			Window win = new Window(new Lists.PutInto(), 2);
			Fork f = new Fork();
			connect(win, f);
			Filter fil = new Filter();
			connect(f, TOP, fil, TOP);
			ApplyFunction gt = new ApplyFunction(new FunctionTree(Numbers.isGreaterOrEqual,
					new FunctionTree(Numbers.subtraction,
							new FunctionTree(format.timestamp(), new NthElement(1)),
							new FunctionTree(format.timestamp(), new NthElement(0))),
					new Constant(24 * 3600 * 1000)));
			connect(f, BOTTOM, gt, INPUT);
			connect(gt, OUTPUT, fil, BOTTOM);
			Sets.PutInto set = new Sets.PutInto();
			connect(fil, set);
			addProcessors(win, f, fil, gt, set);
			associateInput(win);
			associateOutput(set);
		}});
		connect(feeder, slice);
		KeepLast last = new KeepLast();
		connect(slice, last);
		ApplyFunction indices = new ApplyFunction(
				new FunctionTree(new Bags.ApplyToAll(format.index()),
						new FunctionTree(Flatten.instance, Maps.values)));
		connect(last, indices);
		Pump p = new Pump();
		connect(indices, p);
		Print print = new Print(new PrintStream(os)).setSeparator("\n");
		connect(p, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		is.close();
		os.close();
		fs.close();
		System.out.println();
		System.out.println("Code runs successfully.");

	}

}
