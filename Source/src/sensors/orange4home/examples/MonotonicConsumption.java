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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Determines whether each sensor reporting "total energy" produces
 * monotonically increasing numerical values.
 */
public class MonotonicConsumption
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new Orange4HomeLogRepository();
	
	/* The adapter for the event format. */
	protected static final EventFormat format = new Orange4HomeFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("o4h_all_events.csv");
		OutputStream os = fs.writeTo("consumption.txt");
		Processor feeder = format.getFeeder(is);
		
		/* Filter presence sensors */
		FilterOn fil = new FilterOn(new FunctionTree(Equals.instance, format.sensorString(), new Constant("totalenergy")));
		connect(feeder, fil);

		Slice slice = new Slice(format.sensorId(), new GroupProcessor() {{
			ApplyFunction number = new ApplyFunction(new FunctionTree(Numbers.numberCast, format.stateString()));
			Fork f = new Fork();
			connect(number, f);
			Trim trim = new Trim(1);
			connect(f, 0, trim, 0);
			ApplyFunction lt = new ApplyFunction(Numbers.isGreaterOrEqual);
			connect(trim, 0, lt, 0);
			connect(f, 1, lt, 1);
			Cumulate and = new Cumulate(Booleans.and);
			connect(lt, and);
			addProcessors(number, f, trim, lt, and);
			associateInput(number);
			associateOutput(and);
		}});
		connect(fil, slice);

		Pump p = new Pump();
		connect(slice, p);
		KeepLast last = new KeepLast();
		connect(p, last);
		Print print = new Print(new PrintStream(os)).setSeparator("\n");
		connect(last, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		is.close();
		os.close();
		fs.close();
	}
	
	
}
