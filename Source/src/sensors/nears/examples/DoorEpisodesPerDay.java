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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;
import static ca.uqac.lif.cep.Connector.connect;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.complex.RangeCep;
import ca.uqac.lif.cep.fsm.FunctionTransition;
import ca.uqac.lif.cep.fsm.MooreMachine;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.IdentityFunction;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.cep.util.Booleans.And;
import ca.uqac.lif.fs.FileSystemException;
import sensors.DateFunction;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.MultiDaySource;
import sensors.nears.NearsJsonFormat;
import sensors.nears.NearsLogRepository;
import sensors.nears.NearsMultiDaySource;

/**
 * Observes the entrance door contact sensor, and creates composite
 * "door opening" events out of lower-level OPEN/CLOSE events.
 * <p>
 * The pipeline considers as a "door event" a pattern of two successive
 * events of the entrance door sensor, the first having the state OPEN and
 * the second having the state CLOSE. Out of these two events is produced
 * a tuple of the form:
 * <blockquote>
 * {start &mapsto; X, end &mapsto; Y}
 * </blockquote>
 * where X is the timestamp of the OPEN event and Y is the timestamp of the
 * corresponding CLOSE. However, these timestamps are <em>relative to the day
 * where they occur</em>: X and Y are the number of seconds elapsed since
 * midnight of the current day. This makes it possible to compare the time
 * at which the door is opened across multiple days.
 * <p>
 * To this end, the input stream is sliced according to each day. The end
 * result is an associative map where a key is the number of a day in the
 * year, and its associated value is the set of door events having occurred
 * on that day. For example:
 * <p>
 * <table border="1">
 * <tr><th>141</th><td>{start &mapsto; 64589, end &mapsto; 64592}</td></tr>
 * <tr><th>142</th><td>{start &mapsto; 36120, end &mapsto; 36167}, {start &mapsto; 43193, end &mapsto; 43197}</td></tr>
 * <tr><th>143</th><td>{start &mapsto; 50390, end &mapsto; 50392}</td></tr>
 * <tr><td colspan="2">&hellip;</td></tr>
 * </table>
 * <p>
 * The pipeline corresponding to this calculation is illustrated below:
 * <p>
 * <img src="{@docRoot}/doc-files/DoorEpisodesPerDay.png" alt="Pipeline" />
 * 
 * @author Sylvain Hallé
 */
public class DoorEpisodesPerDay
{
	/**
	 *  The adapter for the event format.
	 */
	protected static final EventFormat format = new NearsJsonFormat();
	
	protected static final Constant CLOSE = new Constant(format.getClosedConstant());
	protected static final Constant OPEN = new Constant(format.getOpenConstant());

	public static void main(String[] args) throws FileSystemException, IOException
	{

		/* Define the range of days to process. */
		int first_day = 1, last_day = 126;

		/* Define the input and output file. */
		LogRepository fs = new NearsLogRepository("0102").open();
		OutputStream os = fs.writeTo("DoorEpisodesPerDay.txt");
		MultiDaySource feeder = new NearsMultiDaySource(fs, first_day, last_day);

		/* Create the pipeline. */
		//JsonLineFeeder feeder = new JsonLineFeeder(fs.readFrom("../OnOffEpisodes_raw.txt"));
		Pump p = new Pump();
		connect(feeder, p);
		Fork f0 = new Fork();
		connect(p, f0);
		ApplyFunction is_clap = new ApplyFunction(new FunctionTree(And.instance,
				new FunctionTree(Equals.instance,
						format.locationString(),
						new Constant("entrance")),
				new FunctionTree(Equals.instance,
						format.sensorString(),
						new Constant(NearsJsonFormat.V_CONTACT))));
		Filter f_is_clap = new Filter();
		connect(f0, TOP, f_is_clap, TOP);
		connect(f0, BOTTOM, is_clap, INPUT);
		connect(is_clap, OUTPUT, f_is_clap, BOTTOM);
		
		Slice per_day = new Slice(new FunctionTree(DateFunction.dayOfYear,
				format.timestamp()),
				new GroupProcessor(1, 1) {{
					FindDoorEpisodes fe = new FindDoorEpisodes();
					Sets.PutInto put = new Sets.PutInto();
					connect(fe, put);
					addProcessors(fe, put);
					associateInput(fe).associateOutput(put);
				}});

		connect(f_is_clap, per_day);
		KeepLast last = new KeepLast();
		connect(per_day, last);
		connect(last, new Print(new PrintStream(os)));
		p.run();

		/* Clean up. */
		feeder.stop();
		os.close();
		fs.close();
	}

	/**
	 * The {@link RangeCep} processor that creates "door events". It is
	 * encapsulated into a {@link GroupProcessor} so it can be reused in other
	 * examples.
	 */
	public static class FindDoorEpisodes extends RangeCep
	{
		/**
		 * The Moore machine defining the lifecycle of the door sensor.
		 */
		protected static final MooreMachine s_lifecycle;

		static
		{
			/* Beware: this is not the same Moore machine as in ContractLifecycle. */
			s_lifecycle = new MooreMachine(1, 1);
			s_lifecycle.addSymbol(0, new IdentityFunction());
			s_lifecycle.addSymbol(1, new Constant(Boolean.TRUE));
			s_lifecycle.addSymbol(2, new Constant(Boolean.FALSE));
			s_lifecycle.addTransition(0, new FunctionTransition(new FunctionTree(Equals.instance, StreamVariable.X, OPEN), 1));
			s_lifecycle.addTransition(1, new FunctionTransition(new FunctionTree(Equals.instance, StreamVariable.X, OPEN), 1));
			s_lifecycle.addTransition(1, new FunctionTransition(new FunctionTree(Equals.instance, StreamVariable.X, CLOSE), 2));
			s_lifecycle.addTransition(2, new FunctionTransition(new FunctionTree(Equals.instance, StreamVariable.X, CLOSE), 2));
			s_lifecycle.addTransition(2, new FunctionTransition(new FunctionTree(Equals.instance, StreamVariable.X, CLOSE), 2));
		}

		/**
		 * Creates a new instance of the processor.
		 * @param format The specific event format used to extract values
		 */
		public FindDoorEpisodes()
		{
			super(new GroupProcessor(1, 1) {{ 
				ApplyFunction get_state = new ApplyFunction(format.stateString());
				connect(get_state, s_lifecycle);
				addProcessors(get_state, s_lifecycle);
				associateInput(INPUT, get_state, INPUT);
				associateOutput(OUTPUT, s_lifecycle, OUTPUT);
			}},
					new Processor[] {
							new GroupProcessor(1, 1) {{
								ApplyFunction hr = new ApplyFunction(new FunctionTree(DateFunction.timeOfDay,
										format.timestamp()));
								Cumulate min = new Cumulate(Numbers.minimum);
								connect(hr, min);
								addProcessors(hr, min);
								associateInput(INPUT, hr, INPUT);
								associateOutput(OUTPUT, min, OUTPUT);
							}},
							new GroupProcessor(1, 1) {{
								ApplyFunction hr = new ApplyFunction(new FunctionTree(DateFunction.timeOfDay,
										format.timestamp()));
								Cumulate max = new Cumulate(Numbers.maximum);
								connect(hr, max);
								addProcessors(hr, max);
								associateInput(INPUT, hr, INPUT);
								associateOutput(OUTPUT, max, OUTPUT);
							}}
			}, new MergeScalars("start", "end"));
			includesLast(true);
			allowRestarts(true);
		}
		
		@Override
		public FindDoorEpisodes duplicate(boolean with_state)
		{
			return new FindDoorEpisodes();
		}
	}

}
