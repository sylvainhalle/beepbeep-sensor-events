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
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.casas.CasasLogRepository;
import sensors.casas.CasasTxtFormat;

import static ca.uqac.lif.cep.Connector.connect;
import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;

public class TaintGapBoundaries
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
			Fork fork = new Fork();
			Trim trim = new Trim(1);
			connect(fork, BOTTOM, trim, INPUT);
			ApplyFunction af = new ApplyFunction(new FunctionTree(Numbers.isGreaterOrEqual,
					new FunctionTree(Numbers.subtraction,
							new FunctionTree(format.timestamp(), StreamVariable.Y),
							new FunctionTree(format.timestamp(), StreamVariable.X)
							),
					new Constant(3600 * 1000)));
			connect(fork, TOP, af, TOP);
			connect(trim, OUTPUT, af, BOTTOM);
			addProcessors(fork, trim, af);
			associateInput(fork);
			associateOutput(af);
		}});
		connect(feeder, slice);
		ApplyFunction any = new ApplyFunction(new FunctionTree(Booleans.bagOr, Maps.values));
		connect(slice, any);
		
		Pump p = new Pump();
		connect(any, p);
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
