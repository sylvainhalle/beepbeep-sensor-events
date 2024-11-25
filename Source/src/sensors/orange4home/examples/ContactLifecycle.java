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
package sensors.orange4home.examples;

import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;
import static ca.uqac.lif.cep.Connector.connect;
import static ca.uqac.lif.cep.util.Booleans.land;
import static ca.uqac.lif.cep.util.Booleans.lnot;
import static ca.uqac.lif.cep.util.Booleans.lor;
import static ca.uqac.lif.cep.util.Equals.eq;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
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
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;

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
public class ContactLifecycle
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new Orange4HomeLogRepository();

	/* The adapter for the event format. */
	protected static final EventFormat format = new Orange4HomeFormat();

	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		fs.open();
		InputStream is = fs.readFrom("o4h_all_events.csv");
		OutputStream os = fs.writeTo("ContactLifecycle.txt");
		Processor feeder = format.getFeeder(is);

		/* Create the pipeline. */
		//JsonLineFeeder feeder = new JsonLineFeeder(fs.readFrom("../OnOffEpisodes_raw.txt"));
		Pump p = new Pump();
		connect(feeder, p);
		Fork f0 = new Fork();
		connect(p, f0);
		//ApplyFunction is_clap = new ApplyFunction(land(eq(new JPathFunction(SensorEvent.JP_SENSOR), new Constant(new JsonString(SensorEvent.V_CONTACT))),eq(new JPathFunction(SensorEvent.JP_SUBJECT), new Constant(new JsonString("vanity")))));
		ApplyFunction is_clap = new ApplyFunction(
				land(
						lor(
								lor(
										eq(format.stateString(), new Constant(format.getOnConstant())),
										eq(format.stateString(), new Constant(format.getOffConstant()))
										),
								lor(
										eq(format.stateString(), new Constant(format.getOpenConstant())),
										eq(format.stateString(), new Constant(format.getClosedConstant()))
										)),
						lnot(eq(format.sensorString(), new Constant("status"))) // "status" sensors do not follow this lifecycle
						)
				);
		Filter f_is_clap = new Filter();
		connect(f0, TOP, f_is_clap, TOP);
		connect(f0, BOTTOM, is_clap, INPUT);
		connect(is_clap, OUTPUT, f_is_clap, BOTTOM);

		Slice per_sensor = new Slice(format.sensorId(),
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
		connect(f_is_clap, per_sensor);
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
