package experiments.orange4home.WithoutCoOccurrence;


/**
 * Property: Using the office should involve three sensors: office_luminosity, desk_plug_consumption, and presence_office.
 * The pipeline without co-occurrence assumes that the three relevant sensors emit their events at the same time stamp 
 * and in this order: office_luminosity followed by presence_office followed by desk_plug_consumption.
 * */
import static ca.uqac.lif.cep.Connector.connect;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Iterator;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ContextAssignment;
import ca.uqac.lif.cep.functions.ContextVariable;
import ca.uqac.lif.cep.fsm.FunctionTransition;
import ca.uqac.lif.cep.fsm.MooreMachine;
import ca.uqac.lif.cep.fsm.TransitionOtherwise;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.BinaryFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.IfThenElse;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tmf.Window;
import ca.uqac.lif.cep.util.Booleans.And;
import ca.uqac.lif.cep.util.Booleans.Not;
import ca.uqac.lif.cep.util.Booleans.Or;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Lists;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.util.Numbers.IsGreaterThan;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.cep.util.Strings;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class UsingOffice_WithoutCoOccurrence {

	  protected static final LogRepository fs = new Orange4HomeLogRepository();
	  protected static final EventFormat format = new Orange4HomeFormat();

	  public static void main(String[] args) throws FileSystemException, IOException {
	 
	        fs.open();
	        
	        // Input & feeder
	        InputStream is = fs.readFrom("o4h_all_events.csv");
	        OutputStream os = fs.writeTo("verdicts_withoutCoOccurrence.txt");
	        Processor feeder = format.getFeeder(is);
	        CheckOrderMachine machine = new CheckOrderMachine();
	        Pump p = new Pump();
            connect(feeder, p);                 
            connect(p, machine);

	         // Print verdicts directly to file
	         PrintStream ps = new PrintStream(os);
	         Print printer = new Print(ps).setSeparator("\n");
	         connect(machine, printer);
	
	         // Run the pipeline
	         p.run();
	
	         // Clean up
	         ps.close();
	         feeder.stop();
	         fs.close();
	         System.out.println("Done!");
     }
	  
	  
	  
	  protected static class CheckOrderMachine extends MooreMachine
		{
		  
			public CheckOrderMachine()
			{
				super(1, 1);
				final int STATE0= 0, STATE1 = 1, STATE2 = 2 , STATE3 = 3;
				addSymbol(STATE0, new Constant("true"));
            	addSymbol(STATE1, new Constant("true")); 
            	addSymbol(STATE2, new Constant("true"));
            	addSymbol(STATE3, new Constant("false"));
            	
				
				//variable to save the timestamp
				setContext("t", null);
				
				//transitions from state 0				
				FunctionTree cond0 = new FunctionTree(And.instance,
                        new FunctionTree(Equals.instance, format.locationString(), new Constant("office")),
                        new FunctionTree(And.instance,
                                         new FunctionTree(Equals.instance, format.sensorString(), new Constant("luminosity")),
                                         new FunctionTree(Numbers.isGreaterThan, new FunctionTree(SafeNumberCast.instance, format.stateString()), new Constant(0))
                                        )
                       );
				//save the timestamp of the transition
				ContextAssignment asg0 = new ContextAssignment("t", format.timestamp());
				FunctionTree condNon0 = new FunctionTree(Not.instance, cond0);
				
				addTransition(0, new FunctionTransition(cond0, 1, asg0));
				addTransition(0, new FunctionTransition(condNon0, 0));
				
				//transitions from state 1
				FunctionTree cond1 = new FunctionTree(And.instance,
                        new FunctionTree(Equals.instance, format.locationString(), new Constant("office")),
                        new FunctionTree(And.instance,
                                         new FunctionTree(Equals.instance, format.sensorString(), new Constant("presence")),
                                         new FunctionTree(And.instance,
		                                                   new FunctionTree(Equals.instance, format.stateString(), new Constant("ON")),
		                                                   new FunctionTree(Equals.instance, format.timestamp(), new ContextVariable("t")))
		                                                    )
                       );
				
				addTransition(1, new FunctionTransition(cond1, 2));
				addTransition(1, new TransitionOtherwise(3));
				
				//transitions from state 2
				
				FunctionTree cond2 = new FunctionTree(And.instance,
                        new FunctionTree(Equals.instance, format.locationString(), new Constant("office")),
                        new FunctionTree(And.instance,
                                         new FunctionTree(Equals.instance, format.sensorString(), new Constant("consumption")),
                                         new FunctionTree(And.instance,
		                                                    new FunctionTree(Equals.instance, format.subjectString(), new Constant("deskplug")),
		                                                    new FunctionTree(And.instance,
		                                                    		         new FunctionTree(Numbers.isGreaterOrEqual, new FunctionTree(SafeNumberCast.instance, format.stateString()), new Constant(0)),
		                                                    		         new FunctionTree(Equals.instance, format.timestamp(), new ContextVariable("t")))
                                        ))
                       );

				
				addTransition(2, new FunctionTransition(cond2, 1));
				addTransition(2, new TransitionOtherwise(3));
				addTransition(3, new TransitionOtherwise(0));
			}
		}	  
	  
	
	  public static class SafeNumberCast extends ca.uqac.lif.cep.functions.UnaryFunction<Object,Number> {
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

