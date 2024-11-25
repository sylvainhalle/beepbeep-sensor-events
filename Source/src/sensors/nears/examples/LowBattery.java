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
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.IfThenElse;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.MultiDaySource;
import sensors.nears.NearsJsonFormat;
import sensors.nears.NearsLogRepository;
import sensors.nears.NearsMultiDaySource;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Counts the number of times each sensor has emitted a battery reading below
 * a critical level (typically 10%).
 */
public class LowBattery
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new NearsLogRepository("0032");

	/* The adapter for the event format. */
	protected static final EventFormat format = new NearsJsonFormat();
	
	/* The critical battery level to look for. */
	public static final int level = 10;

	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */		
		fs.open();
		OutputStream os = fs.writeTo("lowbat.txt");
		MultiDaySource feeder = new NearsMultiDaySource(fs);

		/* Filter only battery readings. */
		FilterOn fil = new FilterOn(new FunctionTree(Equals.instance, format.sensorString(), new Constant("battery_level")));
		connect(feeder, fil);

		/* Counts battery readings below a critical level. */
		Slice slice = new Slice(format.sensorId(),
				new GroupProcessor() {{
					ApplyFunction af = new ApplyFunction(new FunctionTree(IfThenElse.instance,
							new FunctionTree(Numbers.isLessOrEqual,
									new FunctionTree(Numbers.numberCast, format.stateString()),
									new Constant(level)),
								new Constant(1),
								new Constant(0)));
					Cumulate sum = new Cumulate(Numbers.addition);
					connect(af, sum);
					addProcessors(af, sum);
					associateInput(af);
					associateOutput(sum);
				}});
		connect(fil, slice);

		/* Keep last state of the resulting map. */
		KeepLast last = new KeepLast();
		connect(slice, last);
		Pump p = new Pump();
		connect(last, p);
		Print print = new Print(new PrintStream(os));
		connect(p, print);

		/* Run the pipeline. */
		p.run();

		/* Close the resources. */
		os.close();
		fs.close();
	}
}
