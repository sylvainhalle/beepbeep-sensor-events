package experiments.orange4home.CoOccurrence;


/**
 * Property: Presence in office should involve three sensors: presence_office, office_luminosity, office_plug_consumption regardless the order.
 * 
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
import experiments.orange4home.CoOccurrence.SittingAtTable_WithCoOccurrence.SafeNumberCast;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;
import sensors.CoOccurs;
import sensors.CoOccurs.CheckCoOccurrence;

public class UsingOffice_WithCoOccurrence {

	protected static final LogRepository fs = new Orange4HomeLogRepository();
	protected static final EventFormat format = new Orange4HomeFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException {
		
		
		fs.open();
		 InputStream  is = fs.readFrom("o4h_all_events.csv");
		    OutputStream os = fs.writeTo("verdicts_coOccurrence.txt");
		    Processor    feeder = format.getFeeder(is);

		/* Timestamp extractor returns a number*/
		Function f_ts = format.timestamp(); 
		

		/* Trigger: luminosity >=0 */
		FunctionTree luminosity = new FunctionTree(And.instance,
                new FunctionTree(Equals.instance, format.locationString(), new Constant("office")),
                new FunctionTree(And.instance,
                                 new FunctionTree(Equals.instance, format.sensorString(), new Constant("luminosity")),
                                 new FunctionTree(Numbers.isGreaterOrEqual, new FunctionTree(SafeNumberCast.instance, format.stateString()), new Constant(0))
                                )
               );
	
		
		Processor w_cond = new ApplyFunction(luminosity);

		/* Condition #1: office_presence = ON */
		FunctionTree isPresenceSensor = new FunctionTree(And.instance,
                new FunctionTree(Equals.instance, format.locationString(), new Constant("office")),
                new FunctionTree(And.instance,
                                 new FunctionTree(Equals.instance, format.sensorString(), new Constant("presence")),
                                 new FunctionTree(Equals.instance, format.stateString(), new Constant("ON")))
               );
		
		Processor condPlug = new ApplyFunction( isPresenceSensor);

		/* Condition #2: desk_plug_consumption >= 0 */
		FunctionTree plugConsumption = new FunctionTree(And.instance,
                new FunctionTree(Equals.instance, format.locationString(), new Constant("office")),
                new FunctionTree(And.instance,
                                 new FunctionTree(Equals.instance, format.sensorString(), new Constant("consumption")),
                                 new FunctionTree(And.instance,
                                                    new FunctionTree(Equals.instance, format.subjectString(), new Constant("deskplug")),
                                                    new FunctionTree(Numbers.isGreaterOrEqual, new FunctionTree(SafeNumberCast.instance, format.stateString()), new Constant(0))
                                ))
               );
	
		
		Processor condNoise = new ApplyFunction(plugConsumption);

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
