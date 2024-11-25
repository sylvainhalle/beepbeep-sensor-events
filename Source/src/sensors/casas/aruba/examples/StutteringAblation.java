package sensors.casas.aruba.examples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.BinaryFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.ParseNumber;
import sensors.RemoveStutterTail;
import sensors.casas.aruba.ArubaFormat;
import sensors.casas.aruba.ArubaLogRepository;
import sensors.patterns.CounterPattern;

import static ca.uqac.lif.cep.Connector.connect;

public class StutteringAblation
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new ArubaLogRepository();
	
	/* The adapter for the event format. */
	protected static final EventFormat format = new ArubaFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		LogRepository fs = new ArubaLogRepository().open();
		InputStream is = fs.readFrom("data");
		OutputStream os = fs.writeTo("TemperatureReadings.txt");
		Processor feeder = format.getFeeder(is);
		
		FilterOn filter = new FilterOn(format.isTemperature());
		connect(feeder, filter);
		
		// Removes any reading closer than 0.5 from the last emitted event
		Slice slice = new Slice(format.sensorId(), new GroupProcessor(1, 1) {{
			RemoveStutterTail rsh = new RemoveStutterTail(new TemperatureDistance());
			CounterPattern cnt = new CounterPattern();
			connect(rsh, cnt);
			addProcessors(rsh, cnt);
			associateInput(rsh);
			associateOutput(cnt);
		}});
		connect(filter, slice);
		Pump p = new Pump();
		connect(slice, p);
		KeepLast last = new KeepLast();
		connect(p, last);
		connect(last, new Print(new PrintStream(os)));
		
		/* Run the pipeline. */
		p.run();
		is.close();
		os.close();
		fs.close();
	}

	protected static class TemperatureDistance extends BinaryFunction<Object,Object,Boolean>
	{
		public TemperatureDistance()
		{
			super(Object.class, Object.class, Boolean.class);
		}

		@Override
		public Boolean getValue(Object x, Object y)
		{
			double t1, t2;
			{
				Object out[] = new Object[1];
				format.stateString().evaluate(new Object[] {x}, out);
				t1 = Double.parseDouble((String) out[0]);
			}
			{
				Object out[] = new Object[1];
				format.stateString().evaluate(new Object[] {y}, out);
				t2 = Double.parseDouble((String) out[0]);
			}
			return Math.abs(t2 - t1) <= 0.5;
			//return false;
		}
		
		
	}
}
