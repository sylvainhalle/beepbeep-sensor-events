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
import ca.uqac.lif.cep.functions.IdentityFunction;
import ca.uqac.lif.cep.functions.IfThenElse;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.mtnp.PrintGnuPlot;
import ca.uqac.lif.cep.mtnp.UpdateTableStream;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.mtnp.plot.Plot.ImageType;
import ca.uqac.lif.mtnp.plot.gnuplot.Scatterplot;
import sensors.EventFormat;
import sensors.Integrate;
import sensors.LogRepository;
import sensors.MultiDaySource;
import sensors.ToConstant;
import sensors.nears.NearsJsonFormat;
import sensors.nears.NearsMultiDaySource;

import static ca.uqac.lif.cep.Connector.connect;
import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;

/**
 * Upon reception of each event, determines the time since the last update
 * received from a specific sensor. This can be used to identify unusually long
 * periods of slience from a sensor.
 * <p>
 * The pipeline corresponding to this calculation is illustrated below:
 * <p>
 * <img src="{@docRoot}/doc-files/LastSensorUpdate.png" alt="Pipeline" />
 * 
 * @author Sylvain Hallé
 */
public class LastSensorUpdate
{
	/* The adapter for the event format. */
	protected static EventFormat format = new NearsJsonFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* The ID of the sensor on wishes to examine. */
		//Object sensor_id = format.createPlacement("bedroom", "bedhead", "fgms001");
		Object sensor_id = format.createPlacement("kitchen", "coffeemaker", "dmof1");
		
		/* Define the range of days to process. */
		int first_day = 1, last_day = -1;
		
		/* Define the input and output file. */
		FileSystem fs = new LogRepository("0102").open();
		OutputStream os = fs.writeTo("LastSensorUpdate.txt");
		MultiDaySource feeder = new NearsMultiDaySource(fs, first_day, last_day);
		
		/* Create the pipeline. */
		Pump p = new Pump();
		connect(feeder, p);
		Fork f1 = new Fork();
		connect(p, f1);
		ApplyFunction get_ts = new ApplyFunction(format.timestamp());
		connect(f1, TOP, get_ts, INPUT);
		Fork f2 = new Fork(3);
		connect(get_ts, f2);
		ApplyFunction to_f = new ApplyFunction(ToConstant.instance);
		connect(f2, BOTTOM, to_f, INPUT);
		Fork f3 = new Fork();
		connect(f1, BOTTOM, f3, INPUT);
		ApplyFunction is_x = new ApplyFunction(new FunctionTree(
				Equals.instance,
				format.sensorPlacement(),
				new Constant(sensor_id)));
		connect(f3, TOP, is_x, INPUT);
		TurnInto to_id = new TurnInto(new IdentityFunction());
		connect(f3, BOTTOM, to_id, INPUT);
		ApplyFunction ite = new ApplyFunction(IfThenElse.instance);
		connect(is_x, OUTPUT, ite, 0);
		connect(to_f, OUTPUT, ite, 1);
		connect(to_id, OUTPUT, ite, 2);
		Integrate in = new Integrate(0);
		connect(ite, in);
		ApplyFunction minus = new ApplyFunction(Numbers.subtraction);
		connect(f2, TOP, minus, TOP);
		connect(in, OUTPUT, minus, BOTTOM);
		
		/* Plot. This part of the pipeline is not present in the diagram.
		 * It is added so that the output of the calculation can be plotted
		 * and visualized. */
		UpdateTableStream table = new UpdateTableStream("Timestamp", "Time (ms)");
		connect(f2, 2, table, TOP);
		connect(minus, OUTPUT, table, BOTTOM);
		KeepLast last = new KeepLast();
		connect(table, last);
		PrintGnuPlot to_plot = new PrintGnuPlot(new Scatterplot(), ImageType.PDF);
		connect(last, to_plot);
		
		/* Connect the pipeline to an output and run. */
		connect(to_plot, new Print(new PrintStream(os)));
		p.run();
		
		/* Clean up. */
		feeder.stop();
		os.close();
		fs.close();
	}

}
