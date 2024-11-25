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
package sensors.nears.examples;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Queue;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionException;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.Integrate;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.UnaryFunction;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.mtnp.PrintGnuPlot;
import ca.uqac.lif.cep.mtnp.UpdateTableStream;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.mtnp.plot.gnuplot.Scatterplot;
import sensors.BoxAndWhiskers;
import sensors.EventFormat;
import sensors.MultiDaySource;
import sensors.PrettyPrint;
import sensors.house.PowerState;
import sensors.nears.NearsJsonFormat;
import sensors.nears.NearsLogRepository;
import sensors.nears.NearsMultiDaySource;
import sensors.nears.examples.PowerLaw.ExcursionEpisode;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Checks that current (I), power (P) and voltage (V) in every energy sensor
 * roughly follow the equation P = VI. The output of the pipeline is a map
 * where every sensor is associated with a box-and-whiskers "plot" of the ratio
 * P / (IV).
 */
public class PowerLawPlot
{
	/* The adapter for the event format. */
	protected static EventFormat format = new NearsJsonFormat();

	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		FileSystem fs = new NearsLogRepository("0104").open();
		MultiDaySource feeder = new NearsMultiDaySource(fs, 1, 36);
		OutputStream os = fs.writeTo("PVI-dmof1.gp");

		// Filter only energy events
		FilterOn fil1 = new FilterOn(new FunctionTree(Equals.instance, format.sensorPlacement(), new Constant(format.createPlacement(
				//"kitchen", "microoven", "zw096"
				"living", "tv", "dmof1"
				//"kitchen", "stove_power", "zw078"
		))));
		connect(feeder, fil1);
		// Filter only energy events
		FilterOn fil2 = new FilterOn(new FunctionTree(Booleans.or,
				new FunctionTree(Booleans.or,
						new FunctionTree(Equals.instance, format.sensorString(), new Constant("instant_current")),
						new FunctionTree(Equals.instance, format.sensorString(), new Constant("instant_voltage"))),
				new FunctionTree(Equals.instance, format.sensorString(), new Constant("instant_power"))));
		connect(fil1, fil2);
		Fork f = new Fork();
		connect(fil2, f);
		// Branch 1: timestamp
		//ApplyFunction get_ts = new ApplyFunction(new FunctionTree(Numbers.division, format.timestamp(), new Constant(1)));
		ApplyFunction get_ts = new ApplyFunction(format.timestamp());
		connect(f, 0, get_ts, 0);
		// Branch 2: P/VI ratio
		ApplyFunction to_f = new ApplyFunction(new PowerState.EventAsFunction(format));
		connect(f, 1, to_f, 0);
		Integrate in = new Integrate(new PowerState());
		connect(to_f, in);
		ApplyFunction pvi = new ApplyFunction(new PowerLaw.PVIRatio());
		connect(in, pvi);
		ExcursionEpisode ee = new PowerLaw.ExcursionEpisode();
		connect(get_ts, 0, ee, 0);
		connect(pvi, 0, ee, 1);
		FilterOn last_fil = new FilterOn(new FunctionTree(Numbers.isGreaterOrEqual, StreamVariable.X, new Constant(30)));
		connect(ee, last_fil);
		Pump p = new Pump();
		connect(last_fil, p);
		connect(p, new Print.Println(new PrintStream(os)));

		/* Run the pipeline. */
		p.run();

		/* Close the resources. */
		os.close();
		fs.close();
	}
}
