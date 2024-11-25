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
package sensors.nears.examples;

import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;
import static ca.uqac.lif.cep.Connector.connect;
import static ca.uqac.lif.cep.util.Equals.eq;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.fsm.FunctionTransition;
import ca.uqac.lif.cep.fsm.MooreMachine;
import ca.uqac.lif.cep.fsm.TransitionOtherwise;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.MultiDaySource;
import sensors.nears.NearsJsonFormat;
import sensors.nears.NearsLogRepository;
import sensors.nears.NearsMultiDaySource;

/**
 * Checks that each contact sensor follows its expected lifecycle, and reports
 * the number of times an unexpected transition is observed for each of them.
 * <p>
 * The pipeline corresponding to this calculation is illustrated below:
 * <p>
 * <img src="{@docRoot}/doc-files/ContactLifecycle.png" alt="Pipeline" />
 * 
 * @author Sylvain Hallé
 */
public class StoveLifecycle
{
	/* The adapter for the event format. */
	protected static final EventFormat format = new NearsJsonFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		LogRepository fs = new NearsLogRepository("0105").open();
		OutputStream os = fs.writeTo("StoveLifecycle.txt");
		MultiDaySource feeder = new NearsMultiDaySource(fs);

		/* Create the pipeline. */
		//JsonLineFeeder feeder = new JsonLineFeeder(fs.readFrom("../OnOffEpisodes_raw.txt"));
		Pump p = new Pump();
		connect(feeder, p);
		Fork f0 = new Fork();
		connect(p, f0);
		//ApplyFunction is_clap = new ApplyFunction(land(eq(new JPathFunction(SensorEvent.JP_SENSOR), new Constant(new JsonString(SensorEvent.V_CONTACT))),eq(new JPathFunction(SensorEvent.JP_SUBJECT), new Constant(new JsonString("vanity")))));
		ApplyFunction is_element = new ApplyFunction(eq(format.sensorString(), new Constant("burner_frontright_intensity")));
		Filter f_is_element = new Filter();
		connect(f0, TOP, f_is_element, TOP);
		connect(f0, BOTTOM, is_element, INPUT);
		connect(is_element, OUTPUT, f_is_element, BOTTOM);
		
		Slice per_sensor = new Slice(format.subjectString(),
				new GroupProcessor(1, 1) {{
					ApplyFunction get_state = new ApplyFunction(format.stateString());
					Fork f = new Fork();
					connect(get_state, f);
					ElementMooreMachine machine = new ElementMooreMachine();
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
		connect(f_is_element, per_sensor);
		KeepLast last = new KeepLast();
		connect(per_sensor, last);
		connect(last, new Print(new PrintStream(os)));
		p.run();

		/* Clean up. */
		feeder.stop();
		os.close();
		fs.close();
	}
	
	/**
	 * A {@link MooreMachine} representing the lifecycle of an oven element.
	 * It is made into a class so it can be reused in other examples.
	 */
	public static class ElementMooreMachine extends MooreMachine
	{
		protected static final Constant MED = new Constant("medium");
		protected static final Constant OFF = new Constant("off");
		protected static final Constant MIN = new Constant("low");
		protected static final Constant MAX = new Constant("high");
		
		public ElementMooreMachine()
		{
			super(1, 1);
			addSymbol(0, new Constant(0));
			addSymbol(1, new Constant(0));
			addSymbol(2, new Constant(0));
			addSymbol(3, new Constant(0));
			addSymbol(4, new Constant(1));
			addTransition(0, new FunctionTransition(eq(StreamVariable.X, MIN), 1));
			addTransition(0, new FunctionTransition(eq(StreamVariable.X, MAX), 2));
			addTransition(0, new FunctionTransition(eq(StreamVariable.X, OFF), 0));
			addTransition(0, new TransitionOtherwise(4));
			addTransition(1, new FunctionTransition(eq(StreamVariable.X, OFF), 0));
			addTransition(1, new FunctionTransition(eq(StreamVariable.X, MED), 3));
			addTransition(1, new FunctionTransition(eq(StreamVariable.X, MIN), 1));
			addTransition(1, new TransitionOtherwise(4));
			addTransition(2, new FunctionTransition(eq(StreamVariable.X, OFF), 0));
			addTransition(2, new FunctionTransition(eq(StreamVariable.X, MED), 3));
			addTransition(2, new FunctionTransition(eq(StreamVariable.X, MAX), 2));
			addTransition(2, new TransitionOtherwise(4));
			addTransition(3, new FunctionTransition(eq(StreamVariable.X, MIN), 0));
			addTransition(3, new FunctionTransition(eq(StreamVariable.X, MAX), 2));
			addTransition(3, new FunctionTransition(eq(StreamVariable.X, MAX), 3));
			addTransition(3, new TransitionOtherwise(4));
			addTransition(4, new FunctionTransition(eq(StreamVariable.X, OFF), 0));
			addTransition(4, new FunctionTransition(eq(StreamVariable.X, MIN), 1));
			addTransition(4, new FunctionTransition(eq(StreamVariable.X, MAX), 2));
			addTransition(4, new FunctionTransition(eq(StreamVariable.X, MED), 3));
		}
		
		@Override
		public ElementMooreMachine duplicate(boolean with_state)
		{
			return new ElementMooreMachine();
		}
	}

}
