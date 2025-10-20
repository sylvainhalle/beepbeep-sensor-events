package experiments.orange4home.WithoutCoOccurrence;


/**
 * Property: Using the couch should involve three sensors: couch_noise, couch_plug_consumption, and presence_couch.
 * The pipeline without co-occurrence assumes that the three relevant sensors emit their events at the same time stamp 
 * and in this order: couch_noise followed by couch_plug_consumption followed by couch_presence.
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
import ca.uqac.lif.cep.functions.UnaryFunction;
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

public class UsingCouch_WithoutCoOccurrence {

	  protected static final LogRepository fs = new Orange4HomeLogRepository();
	  protected static final EventFormat format = new Orange4HomeFormat();

	  public static void main(String[] args) throws FileSystemException, IOException {
	 
	        fs.open();
	        
	        // Input & feeder
	        InputStream is = fs.readFrom("o4h_all_events.csv");
	        OutputStream os = fs.writeTo("verdicts_withoutCoOccurrence.txt");
	        Processor feeder = format.getFeeder(is);
	        
	        
	        
	     // Filter events leaving only the events located in the livingroom 
         /*  FilterOn filterLocation = new FilterOn(
            		new FunctionTree(Equals.instance, format.locationString(), new Constant("livingroom"))
            );
            connect(feeder, filterLocation);
            
         // Filter events leaving only the events with subject = table 
            FilterOn filterSubject = new FilterOn(
             		new FunctionTree(Or.instance,
             				         new FunctionTree(Equals.instance, format.subjectString(), new Constant("table")),
             				        new FunctionTree(Equals.instance, format.subjectString(), new Constant("tableplug"))));
             		
             connect(filterLocation, filterSubject);
	        
           /* FilterOn filterSensors = new FilterOn(
                    new FunctionTree(
                        Or.instance,
                        new FunctionTree(Equals.instance, format.sensorString(), new Constant("presence")),
                        new FunctionTree(
                            Or.instance,
                            new FunctionTree(Equals.instance, format.sensorString(), new Constant("noise")),
                            new FunctionTree(Equals.instance, format.sensorString(), new Constant("plug_consumption"))
                        )
                    )
                );  */
            
         //   connect(filterSubject, filterSensors);*/
            
         // Group processor to check the exact order inside each 3-event window
           /* GroupProcessor group = new GroupProcessor(1, 1);
            {
            	// Build a mooreMachine that emits a final verdict based on the order of the three events inside the window
            	CheckOrderMachine machine = new CheckOrderMachine();
            	//Sets.PutInto put = new Sets.PutInto();
            	//connect(machine,put);
            	group.addProcessors(machine);
                group.associateInput(0, machine, 0);
                group.associateOutput(0, machine, 0);
            	 // group.associateInput(machine).associateOutput(put);
	        }*/
            
            CheckOrderMachine machine = new CheckOrderMachine();
            
            /*ApplyFunction choose = new ApplyFunction(
            		  new FunctionTree(IfThenElse.instance, StreamVariable.X, new Constant(true), StreamVariable.Y)
            		);
            		Connector.connect(condition, 0, choose, 0); // cond boolean
            		Connector.connect(window,    0, choose, 1); // gp boolean (after fix #3)*/
        
         // --- wiring (replace your window/fork/printer/pump section) ---

         // Window of 3 events
       //  Window window = new Window(group, 3);

         // Drive the whole pipeline with a Pump placed *after* the feeder
         Pump p = new Pump();
         connect(feeder, p);                 // feeder -> pump
         //connect(p, filterLocation);         // pump -> filters
      //   connect(filterLocation, filterSensors);
        // connect(filterSensors, window);
         connect(p, machine);

         // Print verdicts directly to file
         PrintStream ps = new PrintStream(os);
         Print printer = new Print(ps).setSeparator("\n");
         connect(machine, printer);

         // Run the network
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
            	addSymbol(STATE1, new Constant("true")); //if the incoming event is couch-noise, then the two next events should be couchplugconsumption and couchpresence
            	addSymbol(STATE2, new Constant("true"));
            	addSymbol(STATE3, new Constant("false"));
            	
				
				//variable to save the timestamp
				setContext("t", null);
				
				//transitions from state 0				
				FunctionTree cond0 = new FunctionTree(And.instance,
                        new FunctionTree(Equals.instance, format.locationString(), new Constant("livingroom")),
                        new FunctionTree(And.instance,
                                         new FunctionTree(Equals.instance, format.sensorString(), new Constant("noise")),
                                         new FunctionTree(And.instance,
		                                                    new FunctionTree(Equals.instance, format.subjectString(), new Constant("couch")),
		                                                    new FunctionTree(Numbers.isGreaterThan, new FunctionTree(SafeNumberCast.instance, format.stateString()), new Constant(0))
                                        ))
                       );
				//save the timestamp of the transition
				ContextAssignment asg0 = new ContextAssignment("t", format.timestamp());
				FunctionTree condNon0 = new FunctionTree(Not.instance, cond0);
				
				addTransition(0, new FunctionTransition(cond0, 1, asg0));
				addTransition(0, new FunctionTransition(condNon0, 0));
				
				//transitions from state 1
				FunctionTree cond1 = new FunctionTree(And.instance,
                        new FunctionTree(Equals.instance, format.locationString(), new Constant("livingroom")),
                        new FunctionTree(And.instance,
                                         new FunctionTree(Equals.instance, format.sensorString(), new Constant("consumption")),
                                         new FunctionTree(And.instance,
		                                                    new FunctionTree(Equals.instance, format.subjectString(), new Constant("couchplug")),
		                                                    new FunctionTree(And.instance,
		                                                    		         new FunctionTree(Numbers.isGreaterOrEqual, new FunctionTree(SafeNumberCast.instance, format.stateString()), new Constant(0)),
		                                                    		         new FunctionTree(Equals.instance, format.timestamp(), new ContextVariable("t")))
                                        ))
                       );
				
				addTransition(1, new FunctionTransition(cond1, 2));
			//	addTransition(1, new FunctionTransition(cond0, 1, asg0));
				//addTransition(1, new TransitionOtherwise(0));
				addTransition(1, new TransitionOtherwise(3));
				
				//transitions from state 2
				FunctionTree cond2 = new FunctionTree(And.instance,
                        new FunctionTree(Equals.instance, format.locationString(), new Constant("livingroom")),
                        new FunctionTree(And.instance,
                                         new FunctionTree(Equals.instance, format.sensorString(), new Constant("presence")),
                                         new FunctionTree(And.instance,
		                                                    new FunctionTree(Equals.instance, format.subjectString(), new Constant("couch")),
		                                                    new FunctionTree(And.instance,
		                                                    		new FunctionTree(Equals.instance, format.stateString(), new Constant("ON")),
		                                                    		new FunctionTree(Equals.instance, format.timestamp(), new ContextVariable("t")))
		                                                    )
                                        )
                       );

				
			//	addTransition(2, new FunctionTransition(cond0, 1, asg0));
			//	addTransition(2, new FunctionTransition(cond1, 2));
			//	addTransition(2, new FunctionTransition(cond2, 3));
				addTransition(2, new FunctionTransition(cond2, 1));
				//addTransition(2, new TransitionOtherwise(0));
				addTransition(2, new TransitionOtherwise(3));
				
				//transitions from state 3
			//	addTransition(3, new FunctionTransition(cond0, 1, asg0));
			//	addTransition(3, new FunctionTransition(cond1, 2));
			//	addTransition(3, new FunctionTransition(cond2, 3));
				addTransition(3, new TransitionOtherwise(0));
				         			
            	
			}
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

