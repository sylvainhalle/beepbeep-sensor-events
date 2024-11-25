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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.Size;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Keeps the count of how many motion sensors are simultaneously ON at any
 * moment, and displays the result in a scatterplot.
 */
public class CountSimultaneousPresenceSensors
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new HHLogRepository("hh130");

	/* The adapter for the event format. */
	protected static final EventFormat format = new HHFormat();

	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("hh130.rawdata.txt");
		OutputStream os = fs.writeTo("SimultaneousSensors.txt");
		Processor feeder = format.getFeeder(is);

		/* Filter presence sensors */
		FilterOn fil = new FilterOn(new FunctionTree(Equals.instance, format.modelString(), new Constant("M")));
		connect(feeder, fil);

		Slice slice = new Slice(format.sensorId(), new ApplyFunction(
				new FunctionTree(Equals.instance, new Constant(format.getOnConstant()),
						format.stateString())));
		connect(feeder, slice);
		ApplyFunction values = new ApplyFunction(new FunctionTree(Size.instance, new FunctionTree(Maps.Keys.instance,
				new Maps.FilterMap(new FunctionTree(Equals.instance, StreamVariable.Y, new Constant(Boolean.TRUE))))));
		connect(slice, values);

		/*Fork f = new Fork();
			connect(values, f);
			TurnInto one = new TurnInto(1);
			connect(f, 0, one, 0);
			Cumulate sum = new Cumulate(Numbers.addition);
			connect(one, sum);
			UpdateTableStream ts = new UpdateTableStream("t", "n");
			connect(sum, 0, ts, 0);
			connect(f, 1, ts, 1);
		 */
		Pump p = new Pump();
		connect(values, p);
		/*KeepLast last = new KeepLast();
			connect(p, last);
			PrintGnuPlot plot = new PrintGnuPlot(new Scatterplot().withPoints(false));
			connect(last, plot);*/
		Print print = new Print(new PrintStream(os)).setSeparator("\n");
		connect(p, print);

		/* Run the pipeline. */
		p.run();

		/* Close the resources. */
		is.close();
		os.close();
		fs.close();
	}


}
