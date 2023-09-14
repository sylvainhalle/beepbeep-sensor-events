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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.json.JPathFunction;
import ca.uqac.lif.cep.json.StringValue;
import ca.uqac.lif.cep.mtnp.PrintGnuPlot;
import ca.uqac.lif.cep.mtnp.UpdateTableStream;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.mtnp.plot.Plot.ImageType;
import ca.uqac.lif.mtnp.plot.gnuplot.Scatterplot;
import nears.DateToTimestamp;
import nears.LogRepository;
import nears.MultiDaySource;

import static ca.uqac.lif.cep.Connector.connect;
import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;

/**
 * Calculates the time between successive updates of <em>any</em> sensor.
 * <p>
 * The pipeline corresponding to this calculation is illustrated below:
 * <p>
 * <img src="{@docRoot}/doc-files/LastSensorUpdateAny.png" alt="Pipeline" />
 * 
 * @author Sylvain Hallé
 */
public class LastSensorUpdateAny
{

	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the range of days to process. */
		int first_day = 1, last_day = 120;
		
		/* Define the input and output file. */
		FileSystem fs = new LogRepository().open();
		OutputStream os = fs.writeTo("LastSensorUpdateAny.gp");
		fs.chdir("0032");
		
		/* Create the pipeline. */
		MultiDaySource feeder = new MultiDaySource(fs, first_day, last_day);
		Pump p = new Pump();
		connect(feeder, p);
		ApplyFunction get_ts = new ApplyFunction(new FunctionTree(DateToTimestamp.instance, new FunctionTree(StringValue.instance, new JPathFunction("sentAt/$date"))));
		connect(p, TOP, get_ts, INPUT);
		Fork f1 = new Fork(3);
		connect(get_ts, f1);
		Trim trim = new Trim(1);
		connect(f1, TOP, trim, INPUT);
		ApplyFunction minus = new ApplyFunction(Numbers.subtraction);
		connect(trim, OUTPUT, minus, TOP);
		connect(f1, BOTTOM, minus, BOTTOM);
		
		/* Plot. */
		UpdateTableStream table = new UpdateTableStream("Timestamp", "Time (ms)");
		Trim trim2 = new Trim(1);
		connect(f1, 2, trim2, INPUT);
		connect(trim2, OUTPUT, table, TOP);
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
