/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023-2024 Sylvain Hallé

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package sensors.casas.hh.examples;

import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;
import static ca.uqac.lif.cep.Connector.connect;
import static ca.uqac.lif.cep.util.Booleans.lor;
import static ca.uqac.lif.cep.util.Equals.eq;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.fsm.FunctionTransition;
import ca.uqac.lif.cep.fsm.MooreMachine;
import ca.uqac.lif.cep.fsm.TransitionOtherwise;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.tuples.TupleFeeder;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;


/**
 * We do not have contact sensor in the CASAS dataset, so we use the same example but for a motion sesnor
 * As in ContactLifecycle.java example, this example checks that a motion sensor follows its expected lifecycle, 
 * and reports the number of times an unexpected transition is observed.
 * 

 * @author Sylvain Hallé
 */
public class MotionSensorLifecycle
{
	/* The adapter for the event format. */
	protected static final EventFormat format = new HHFormat();
	protected static final LogRepository fs = new HHLogRepository("hh115");
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		fs.open();
		InputStream is = fs.readFrom("casas-rawdata.txt");
		ReadLines reader = new ReadLines(is);
		TupleFeeder feeder = new TupleFeeder();
		Connector.connect(reader, feeder);
		OutputStream os = fs.writeTo("MotionSensorLifecycle.txt");//casas-data.MotionSensorLifecycle.txt
		

		/* Create the pipeline. */
		Pump p = new Pump();
		connect(feeder, p);
		Fork f0 = new Fork();
		connect(p, f0);
		//ApplyFunction is_clap = new ApplyFunction(eq(format.sensorString(), new Constant("Control4-Motion")));
		
		/**
		 * In the CASAS dataset, we have the motion sensors: Control4-Motion and Control4-MotionArea producing ON and OFF states
		 * and the door sensor Control4-Door producing CLOSE and OPEN states
		 * The filter filters out any event that is not from any of these sensors.
		 * */
		
		ApplyFunction is_motion = new ApplyFunction(new FunctionTree(Booleans.or, 
				                                                     new FunctionTree(Booleans.or, 
				                                                    		          new FunctionTree (Equals.instance, format.sensorString(), new Constant("Control4-Motion")), 
				                                                    		          new FunctionTree(Equals.instance, format.sensorString(),new Constant("Control4-MotionArea"))),
                                                                     new FunctionTree(Equals.instance, format.sensorString(), new Constant("Control4-Door"))));
		
		Filter f_is_motion = new Filter();
		connect(f0, TOP, f_is_motion, TOP);
		connect(f0, BOTTOM, is_motion, INPUT);
		connect(is_motion, OUTPUT, f_is_motion, BOTTOM);
		
		Slice per_sensor = new Slice(format.subjectString(),
				new GroupProcessor(1, 1) {{
					ApplyFunction get_state = new ApplyFunction(format.stateString());
					Fork f = new Fork();
					connect(get_state, f);
					ContactMooreMachine machine = new ContactMooreMachine();
					connect(f, TOP, machine, INPUT);
					Cumulate add1 = new Cumulate(Numbers.addition);
					connect(machine, add1);
					TurnInto one = new TurnInto(1);
					connect(f, BOTTOM, one, INPUT);
					Cumulate add2 = new Cumulate(Numbers.addition);
					connect(one, add2);
					ApplyFunction merge = new ApplyFunction(new MergeScalars("invalid", "total"));
					connect(add1, OUTPUT, merge, TOP);
					connect(add2, OUTPUT, merge, BOTTOM);
					addProcessors(get_state, f, machine, one, add1, add2, merge);
					associateInput(get_state).associateOutput(merge);
				}});
		connect(f_is_motion, per_sensor);
		KeepLast last = new KeepLast();
		connect(per_sensor, last);
		connect(last, new Print(new PrintStream(os)));
		p.run();

		/* Clean up. */
		feeder.stop();
		os.close();
		fs.close();
		System.out.println("code runs successfully.");
	}
	
	/**
	 * A {@link MooreMachine} representing the lifecycle of a contact sensor.
	 * It is made into a class so it can be reused in other examples.
	 */
	public static class ContactMooreMachine extends MooreMachine
	{
		protected static final Constant CLOSE = new Constant(format.getClosedConstant());
		protected static final Constant OFF = new Constant(format.getOffConstant());
		protected static final Constant ON = new Constant(format.getOnConstant());
		protected static final Constant OPEN = new Constant(format.getOpenConstant());
		
		public ContactMooreMachine()
		{
			super(1, 1);
			addSymbol(0, new Constant(1));
			addSymbol(1, new Constant(0));
			addSymbol(2, new Constant(0));
			addTransition(0, new FunctionTransition(lor(eq(StreamVariable.X, OPEN), eq(StreamVariable.X, ON)), 1));
			addTransition(0, new FunctionTransition(lor(eq(StreamVariable.X, CLOSE), eq(StreamVariable.X, OFF)), 2));
			addTransition(0, new TransitionOtherwise(0));
			addTransition(1, new FunctionTransition(lor(eq(StreamVariable.X, CLOSE), eq(StreamVariable.X, OFF)), 2));
			addTransition(1, new TransitionOtherwise(0));
			addTransition(2, new FunctionTransition(lor(eq(StreamVariable.X, OPEN), eq(StreamVariable.X, ON)), 1));
			addTransition(2, new TransitionOtherwise(0));
		}
		
		@Override
		public ContactMooreMachine duplicate(boolean with_state)
		{
			return new ContactMooreMachine();
		}
	}

}
