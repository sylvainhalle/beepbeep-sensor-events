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

import ca.uqac.lif.cep.GroupProcessor;
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
import sensors.MultiDaySource;
import sensors.nears.NearsJsonFormat;
import sensors.nears.NearsLogRepository;
import sensors.nears.NearsMultiDaySource;

import static ca.uqac.lif.cep.Connector.connect;

public class ContactEpisodes
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
		int first_day = 1, last_day = 36;

		/* Define the input and output file. */
		LogRepository fs = new NearsLogRepository("0105").open();
		OutputStream os = fs.writeTo("DoorEpisodesPerDay.txt");
		MultiDaySource feeder = new NearsMultiDaySource(fs, first_day, last_day);
		
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
