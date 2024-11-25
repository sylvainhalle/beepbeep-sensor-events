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

import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.util.Bags;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.MultiDaySource;
import sensors.nears.NearsJsonFormat;
import sensors.nears.NearsLogRepository;
import sensors.nears.NearsMultiDaySource;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Keeps the count of how many motion sensors are simultaneously ON at any
 * moment, and displays the result in a scatterplot.
 */
public class CountSimultaneousPresenceSensors
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new NearsLogRepository("0034");
	
	/* The adapter for the event format. */
	protected static final EventFormat format = new NearsJsonFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		OutputStream os = fs.writeTo("SimultaneousPresence.txt");
		MultiDaySource feeder = new NearsMultiDaySource(fs);
		
		/* Filter presence sensors */
		FilterOn fil = new FilterOn(new FunctionTree(Equals.instance, format.sensorString(), new Constant("motion")));
		connect(feeder, fil);

		Slice slice = new Slice(format.sensorId(), new ApplyFunction(
			new FunctionTree(Equals.instance, new Constant(format.getOnConstant()),
					format.stateString())));
		connect(fil, slice);
		ApplyFunction values = new ApplyFunction(new FunctionTree(new Bags.ApplyToAll(new FetchAttribute("location")), new FunctionTree(Maps.Keys.instance,
					new Maps.FilterMap(new FunctionTree(Equals.instance, StreamVariable.Y, new Constant(Boolean.TRUE))))));
		connect(slice, values);
		
		Pump p = new Pump();
		connect(values, p);
		/*KeepLast last = new KeepLast();
		connect(p, last);*/
		Print print = new Print(new PrintStream(os)).setSeparator("\n");
		connect(p, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		os.close();
		fs.close();
	}
	
	
}
