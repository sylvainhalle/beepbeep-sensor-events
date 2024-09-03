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
package sensors.examples.casas.aruba;

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
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Bags;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Lists;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.casas.aruba.ArubaFormat;
import sensors.casas.aruba.ArubaLogRepository;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Keeps the count of motion sensors that are activated at the same time. Each
 * motion sensor cycles through the ON and OFF states, and the processor chain
 * finds intervals of time where two sensors are ON simultaneously.
 */
public class SimultaneousMotionSensorsPairs
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new ArubaLogRepository();
	
	/* The adapter for the event format. */
	protected static final EventFormat format = new ArubaFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("data");
		OutputStream os = fs.writeTo("pairs.txt");
		OutputStream os_err = fs.writeTo("err.txt");
		Processor feeder = format.getFeeder(is);
		
		/* Keep only motion sensors */
		/*Fork fork = new Fork();
		connect(feeder, fork);
		Filter fil = new Filter();
		connect(fork, 0, fil, 0);
		ApplyFunction is_motion = new ApplyFunction(new FunctionTree(Equals.instance, format.modelString(), new Constant("M")));
		connect(fork, 1, is_motion, 0);
		connect(is_motion, 0, fil, 1);
		*/
		Slice slice = new Slice(format.sensorId(), new ApplyFunction(
			new FunctionTree(Equals.instance, new Constant(format.getOnConstant()),
					format.stateString())));
		connect(feeder, slice);
		ApplyFunction values = new ApplyFunction(new FunctionTree(Maps.Keys.instance,
					new Maps.FilterMap(new FunctionTree(Equals.instance, StreamVariable.Y, new Constant(Boolean.TRUE)))));
		connect(slice, values);
		/*ApplyFunction pairs = new ApplyFunction(new FunctionTree(new Lists.Product(), StreamVariable.X, StreamVariable.X));
		connect(values, pairs);*/
		/*Sets.Union in_set = new Sets.Union();
		connect(pairs, in_set);
		*/
		
		Pump p = new Pump();
		connect(values, p);
		//KeepLast last = new KeepLast();
		//connect(p, last);
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