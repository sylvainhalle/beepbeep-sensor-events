/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023 Sylvain Hallé

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
package nears.examples;

import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;
import static ca.uqac.lif.cep.Connector.connect;
import static nears.SensorEvent.JP_LOCATION;
import static nears.SensorEvent.JP_SENSOR;
import static nears.SensorEvent.JP_TIMESTAMP;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.complex.RangeCep;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.json.JPathFunction;
import ca.uqac.lif.cep.json.StringValue;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Freeze;
import ca.uqac.lif.cep.tmf.Insert;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.json.JsonString;
import nears.DateToTimestamp;
import nears.LogRepository;
import nears.MultiDaySource;

public class PresenceHeatMap
{

	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		FileSystem fs = new LogRepository("0105").open();
		MultiDaySource feeder = new MultiDaySource(fs);
		OutputStream os = fs.writeTo("PresenceHeatMap.txt");
		
		/* Keep only motion sensor events */
		Pump p = new Pump();
		connect(feeder, p);
		Fork f0 = new Fork();
		connect(p, f0);
		ApplyFunction is_motion = new ApplyFunction(new FunctionTree(Equals.instance,
				new JPathFunction(JP_SENSOR),
				new Constant(new JsonString("motion"))));
		connect(f0, BOTTOM, is_motion, INPUT);
		Filter f_is_motion = new Filter();
		connect(is_motion, OUTPUT, f_is_motion, BOTTOM);
		connect(f0, TOP, f_is_motion, TOP);
		
		RangeCep rc = new RangeCep(new GroupProcessor(1, 1) {{
				ApplyFunction loc = new ApplyFunction(new JPathFunction(JP_LOCATION));
				Fork f = new Fork();
				connect(loc, f);
				Trim t = new Trim(1);
				connect(f, TOP, t, INPUT);
				ApplyFunction eq = new ApplyFunction(Equals.instance);
				connect(t, OUTPUT, eq, TOP);
				connect(f, BOTTOM, eq, BOTTOM);
				Insert ins = new Insert(1, true);
				connect(eq, ins);
				addProcessors(loc, f, t, eq, ins);
				associateInput(loc).associateOutput(ins);
			}},
				new Processor[] {
					new ApplyFunction(new JPathFunction(JP_LOCATION)),
					new GroupProcessor(1, 1) {{
						ApplyFunction ts = new ApplyFunction(new FunctionTree(DateToTimestamp.instance, new FunctionTree(StringValue.instance, new JPathFunction(JP_TIMESTAMP))));
						Fork f = new Fork();
						connect(ts, f);
						ApplyFunction minus = new ApplyFunction(Numbers.subtraction);
						connect(f, TOP, minus, TOP);
						Freeze fr = new Freeze();
						connect(f, BOTTOM, fr, INPUT);
						connect(fr, OUTPUT, minus, BOTTOM);
						addProcessors(ts, f, minus, fr);
						associateInput(ts).associateOutput(minus);
					}}},
				new MergeScalars("location", "duration")).allowRestarts(true).includesLast(true).isContiguous(true);
		connect(f_is_motion, rc);
		
		/* Connect the pipeline to an output and run. */
		connect(rc, new Print(new PrintStream(os)).setSeparator("\n"));
		p.run();
		
		/* Clean up. */
		os.close();
		fs.close();
		
	}

}
