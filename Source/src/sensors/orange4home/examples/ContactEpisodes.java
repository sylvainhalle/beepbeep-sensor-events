package sensors.orange4home.examples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.fsm.FunctionTransition;
import ca.uqac.lif.cep.fsm.MooreMachine;
import ca.uqac.lif.cep.fsm.TransitionOtherwise;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.ContextAssignment;
import ca.uqac.lif.cep.functions.ContextVariable;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;

import static ca.uqac.lif.cep.Connector.connect;

public class ContactEpisodes
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new Orange4HomeLogRepository();

	/* The adapter for the event format. */
	protected static final EventFormat format = new Orange4HomeFormat();
	
	protected static final Constant CLOSE = new Constant(format.getClosedConstant());
	protected static final Constant OPEN = new Constant(format.getOpenConstant());
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		fs.open();
		InputStream is = fs.readFrom("o4h_all_events.csv");
		OutputStream os = fs.writeTo("DoorEpisodesPerDay.txt");
		Processor feeder = format.getFeeder(is);
		
		/* Filter only the "contact" events. */
		FilterOn filter = new FilterOn(new FunctionTree(Booleans.or,
				new FunctionTree(Equals.instance, format.stateString(), CLOSE),
				new FunctionTree(Equals.instance, format.stateString(), OPEN)));
		connect(feeder, filter);
		
		/* Create the pipeline. */
		Slice slice = new Slice(format.sensorId(),
				new GroupProcessor(1, 1) {{
					EpisodeMachine em = new EpisodeMachine();
					Sets.PutInto put = new Sets.PutInto();
					connect(em, put);
					addProcessors(em, put);
					associateInput(em).associateOutput(put);
				}});
		connect(filter, slice);
		
		/* Print the results. */
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
	
	protected static class EpisodeMachine extends MooreMachine
	{
		public EpisodeMachine()
		{
			super(1, 1);
			addSymbol(0, new Constant(null));
			addSymbol(1, new Constant(null));
			addSymbol(2, new FunctionTree(Numbers.subtraction, format.timestamp(), new ContextVariable("s")));
			addTransition(0, new FunctionTransition(new FunctionTree(Equals.instance, format.stateString(), OPEN), 1, new ContextAssignment("s", format.timestamp())));
			addTransition(1, new FunctionTransition(new FunctionTree(Equals.instance, format.stateString(), CLOSE), 2));
			addTransition(2, new FunctionTransition(new FunctionTree(Equals.instance, format.stateString(), CLOSE), 0));
			addTransition(2, new FunctionTransition(new FunctionTree(Equals.instance, format.stateString(), OPEN), 1, new ContextAssignment("s", format.timestamp())));
			addTransition(0, new TransitionOtherwise(0));
			addTransition(1, new TransitionOtherwise(1));
		}
	}
}
