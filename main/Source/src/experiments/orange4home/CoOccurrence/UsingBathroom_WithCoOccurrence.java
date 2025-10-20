package experiments.orange4home.CoOccurrence;


/**
 * Property: Using Bathroom should involve 3 sensors: light1, light2, presence_bathroom regardless the order.
 * The pipeline with co-occurrence ...
 
 */
import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.ContextVariable;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.UnaryFunction;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Booleans.And;
import ca.uqac.lif.cep.util.Booleans.Or;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Lists;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.util.Numbers.IsGreaterThan;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;
import sensors.CoOccurs;
import sensors.CoOccurs.CheckCoOccurrence;

public class UsingBathroom_WithCoOccurrence {

	protected static final LogRepository fs = new Orange4HomeLogRepository();
	protected static final EventFormat format = new Orange4HomeFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException {
		
		
		fs.open();
		 InputStream  is = fs.readFrom("o4h_all_events.csv");
		    OutputStream os = fs.writeTo("verdicts_cooccurrence.txt");
		    Processor    feeder = format.getFeeder(is);

		/* Timestamp extractor returns a number*/
		Function f_ts = format.timestamp(); 
		

		/* Trigger: presence_bathroom = ON */
		FunctionTree isPresenceSensor = new FunctionTree(And.instance,
                new FunctionTree(Equals.instance, format.locationString(), new Constant("bathroom")),
                new FunctionTree(And.instance,
                                 new FunctionTree(Equals.instance, format.sensorString(), new Constant("presence")),
                                 new FunctionTree(Equals.instance, format.stateString(), new Constant("ON"))
                                )
               );
		
		Processor w_cond = new ApplyFunction(isPresenceSensor);

		/* Condition #1: bathroom_light1 >= 0 */
		FunctionTree isPlugSensor = new FunctionTree(And.instance,
                new FunctionTree(Equals.instance, format.locationString(), new Constant("bathroom")),
                new FunctionTree(And.instance,
                                 new FunctionTree(Equals.instance, format.sensorString(), new Constant("light1")),
                                 new FunctionTree(Numbers.isGreaterOrEqual, new FunctionTree(SafeNumberCast.instance, format.stateString()), new Constant(0))
                                 )
               );
		
		Processor condPlug = new ApplyFunction( isPlugSensor);

		/* Condition #2: bathroom_light2 >= 0 */
		FunctionTree isNoiseSensor = new FunctionTree(And.instance,
                new FunctionTree(Equals.instance, format.locationString(), new Constant("bathroom")),
                new FunctionTree(And.instance,
                                 new FunctionTree(Equals.instance, format.sensorString(), new Constant("light2")),
                                 new FunctionTree(Numbers.isGreaterOrEqual, new FunctionTree(SafeNumberCast.instance, format.stateString()), new Constant(0))
                                )
               );
	
		
		Processor condNoise = new ApplyFunction(isNoiseSensor);

		/* Co-occurrence window == 5000 L means all three events must co-occur within 5 seconds*/
		long WINDOW_MS = 5000L;

		Processor coOccurs = new CoOccurs(WINDOW_MS, f_ts, w_cond, condPlug, condNoise);
		
	    Pump p = new Pump();
	    connect(feeder, p);
	    connect(p, coOccurs);

	    PrintStream ps = new PrintStream(os);
	    Print printer = new Print(ps).setSeparator("\n");
	    connect(coOccurs, printer);

	    // Run
	    p.run();

	    // Cleanup
	    ps.close();
	    feeder.stop();
	    fs.close();
	    System.out.println("Done!");
		
	}	
	
	public static class SafeNumberCast extends UnaryFunction<Object,Number> {
	    public static final SafeNumberCast instance = new SafeNumberCast();
	    private SafeNumberCast() { super(Object.class, Number.class); }
	    @Override
	    public Number getValue(Object o) {
	      if (o == null) return -1; 
	      if (o instanceof Number) return ((Number) o).doubleValue();
	      try { return Double.parseDouble(o.toString()); }
	      catch (Exception e) { return -1; }
	    }
	  }
}
