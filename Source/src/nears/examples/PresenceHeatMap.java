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
import static nears.SensorEvent.JP_STATE;
import static nears.SensorEvent.JP_TIMESTAMP;
import static nears.SensorEvent.V_ON;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.complex.RangeCep;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.IdentityFunction;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.json.JPathFunction;
import ca.uqac.lif.cep.json.StringValue;
import ca.uqac.lif.cep.mtnp.UpdateTableMap;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Freeze;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.MapToTuple;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.json.JsonString;
import nears.DateFunction;
import nears.DateToTimestamp;
import nears.LogRepository;
import nears.MultiDaySource;
import nears.StutterFirst;

public class PresenceHeatMap
{

	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		FileSystem fs = new LogRepository("0102").open();
		MultiDaySource feeder = new MultiDaySource(fs);
		OutputStream os = fs.writeTo("PresenceHeatMap.txt");
		
		/* Keep only motion sensor events */
		Pump p = new Pump();
		connect(feeder, p);
		Fork f0 = new Fork();
		connect(p, f0);
		ApplyFunction is_motion_on = new ApplyFunction(new FunctionTree(Booleans.and,
				new FunctionTree(Equals.instance,
						new JPathFunction(JP_SENSOR),
						new Constant(new JsonString("motion"))),
				new FunctionTree(Equals.instance,
						new JPathFunction(JP_STATE),
						new Constant(new JsonString(V_ON)))));
		connect(f0, BOTTOM, is_motion_on, INPUT);
		Filter f_is_motion = new Filter();
		connect(is_motion_on, OUTPUT, f_is_motion, BOTTOM);
		connect(f0, TOP, f_is_motion, TOP);
		
		RangeCep rc = new RangeCep(new GroupProcessor(1, 1) {{
				ApplyFunction loc = new ApplyFunction(new JPathFunction(JP_LOCATION));
				StutterFirst sf = new StutterFirst(2);
				connect(loc, sf);
				Fork f = new Fork();
				connect(sf, f);
				Trim t = new Trim(1);
				connect(f, TOP, t, INPUT);
				ApplyFunction eq = new ApplyFunction(Equals.instance);
				connect(t, OUTPUT, eq, TOP);
				connect(f, BOTTOM, eq, BOTTOM);
				addProcessors(loc, f, t, eq, sf);
				associateInput(loc).associateOutput(eq);
			}},
				new Processor[] {
					new GroupProcessor(1, 1) {{
						Freeze f = new Freeze();
						ApplyFunction ts = new ApplyFunction(new FunctionTree(DateToTimestamp.instance, new FunctionTree(StringValue.instance, new JPathFunction(JP_TIMESTAMP))));
						connect(f, ts);
						addProcessors(f, ts);
						associateInput(f).associateOutput(ts);
					}},
					new GroupProcessor(1, 1) {{
						Freeze f = new Freeze();
						ApplyFunction l = new ApplyFunction(new JPathFunction(JP_LOCATION));
						connect(f, l);
						addProcessors(f, l);
						associateInput(f).associateOutput(l);
					}},
					new GroupProcessor(1, 1) {{
						TurnInto one = new TurnInto(1);
						Cumulate sum = new Cumulate(Numbers.addition);
						connect(one, sum);
						addProcessors(one, sum);
						associateInput(one).associateOutput(sum);
					}},
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
				new MergeScalars("start", "location", "events", "duration")).allowRestarts(true).includesLast(true).isContiguous(true);
		connect(f_is_motion, rc);
		RangeCep week_summary = new RangeCep(
				new GroupProcessor(1, 1) {{
					ApplyFunction week = new ApplyFunction(new FunctionTree(DateFunction.weekOfYear, new FunctionTree(Numbers.numberCast, new FetchAttribute("start"))));
					StutterFirst sf = new StutterFirst(2);
					connect(week, sf);
					Fork f = new Fork();
					connect(sf, f);
					Trim t = new Trim(1);
					connect(f, TOP, t, INPUT);
					ApplyFunction eq = new ApplyFunction(Equals.instance);
					connect(t, OUTPUT, eq, TOP);
					connect(f, BOTTOM, eq, BOTTOM);
					addProcessors(week, f, t, eq, sf);
					associateInput(week).associateOutput(eq);
				}},
				new Processor[] {new Slice(new FetchAttribute("location"),
						new GroupProcessor(1, 1) {{
							ApplyFunction dur = new ApplyFunction(new FunctionTree(Numbers.numberCast, new FetchAttribute("duration")));
							Cumulate sum = new Cumulate(Numbers.addition);
							connect(dur, sum);
							addProcessors(dur, sum);
							associateInput(dur).associateOutput(sum);
						}})},
				new IdentityFunction(1)).allowRestarts(true).includesLast(true).isContiguous(true);
		connect(rc, week_summary);
		ApplyFunction tuple = new ApplyFunction(MapToTuple.instance);
		connect(week_summary, tuple);
		UpdateTableMap table = new UpdateTableMap("\"living\"", "\"bedroom\"", "\"kitchen\"", "\"bathroom\"", "\"entrance\"");
		connect(tuple, table);
		KeepLast last = new KeepLast();
		connect(table, last);
		
		/* Connect the pipeline to an output and run. */
		connect(last, new Print(new PrintStream(os)).setSeparator("\n"));
		p.run();
		
		/* Clean up. */
		os.close();
		fs.close();
		
	}

}
