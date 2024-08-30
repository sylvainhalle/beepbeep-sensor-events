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
package sensors.casas.aruba;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.mtnp.PrintGnuPlot;
import ca.uqac.lif.cep.mtnp.UpdateTableStream;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Bags;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Lists;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.cep.util.Size;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.mtnp.plot.gnuplot.Scatterplot;
import sensors.EventFormat;
import sensors.LogRepository;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Finds pairs of motion sensors that are activated at the same time. Each
 * motion sensor cycles through the ON and OFF states, and the processor chain
 * finds intervals of time where two sensors are ON simultaneously.
 */
public class CountSimultaneousMotionSensors
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new ArubaLogRepository();
	
	/* The adapter for the event format. */
	protected static final EventFormat format = new ArubaFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("data");
		OutputStream os = fs.writeTo("pairs-num.txt");
		Processor feeder = format.getFeeder(is);
		//Processor feeder = format.getFeeder("/home/sylvain/Workspaces/BeepBeep/beepbeep-sensor-events/Source/data/aruba/data", new PrintStream(os_err));
		
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
		ApplyFunction values = new ApplyFunction(new FunctionTree(Size.instance, new FunctionTree(Maps.Keys.instance,
					new Maps.FilterMap(new FunctionTree(Equals.instance, StreamVariable.Y, new Constant(Boolean.TRUE))))));
		connect(slice, values);
		/*ApplyFunction pairs = new ApplyFunction(new FunctionTree(new Lists.Product(), StreamVariable.X, StreamVariable.X));
		connect(values, pairs);*/
		/*Sets.Union in_set = new Sets.Union();
		connect(pairs, in_set);
		*/
		Fork f = new Fork();
		connect(values, f);
		TurnInto one = new TurnInto(1);
		connect(f, 0, one, 0);
		Cumulate sum = new Cumulate(Numbers.addition);
		connect(one, sum);
		UpdateTableStream ts = new UpdateTableStream("t", "n");
		connect(sum, 0, ts, 0);
		connect(f, 1, ts, 1);
		Pump p = new Pump();
		connect(ts, p);
		KeepLast last = new KeepLast();
		connect(p, last);
		PrintGnuPlot plot = new PrintGnuPlot(new Scatterplot().withPoints(false));
		connect(last, plot);
		Print print = new Print(new PrintStream(os)).setSeparator("\n");
		connect(plot, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		is.close();
		os.close();
		fs.close();
	}
	
	
}