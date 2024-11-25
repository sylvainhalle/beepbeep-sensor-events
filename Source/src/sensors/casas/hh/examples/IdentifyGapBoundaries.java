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

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.util.Bags;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.NthElement;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.Flatten;
import sensors.LogRepository;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;

import static ca.uqac.lif.cep.Connector.connect;
import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;

/**
 * Identifies the indices of the events representing the start and end
 * boundaries of gaps in the input stream. A pair of two events
 * <i>e</i><sub>1</sub> and <i>e</i><sub>2</sub> is considered a gap if:
 * <ol>
 * <li><i>e</i><sub>1</sub> and <i>e</i><sub>2</sub> are emitted by the
 * same sensor</li>
 * <li>no other event of the same sensor lies between <i>e</i><sub>1</sub> and
 * <i>e</i><sub>2</sub></li>
 * <li>the elapsed time between <i>e</i><sub>1</sub> and
 * <i>e</i><sub>2</sub> exceeds some predefined threshold <i>t</i></li>
 * </ol>
 * Intuitively, <i>e</i><sub>1</sub> and <i>e</i><sub>2</sub> mark the
 * boundaries of an interval of time during which the sensor is unusually
 * silent, and where there <em>may</em> be missing data.
 * <p>
 * <img src="{@docRoot}/doc-files/IdentifyGapBoundaries.png" alt="Pipeline" />
 */
public class IdentifyGapBoundaries
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new HHLogRepository("hh115");
	
	/* The adapter for the event format. */
	protected static final EventFormat format = new HHFormat();
	
	/* The minimum number of milliseconds of silence for an interval to be
	 * considered as a gap. */
	protected static final long gapLength = 24 * 3600 * 1000;

	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("casas-rawdata.txt");
		OutputStream os = fs.writeTo("gap-boundaries.txt");
		Processor feeder = format.getFeeder(is);
		
		Slice slice = new Slice(format.sensorId(), new GroupProcessor(1, 1) {{
			Fork f0 = new Fork();
			ApplyFunction to_list = new ApplyFunction(new Bags.ToList(2));
			connect(f0, TOP, to_list, TOP);
			Trim trim = new Trim(1);
			connect(f0, BOTTOM, trim, INPUT);
			connect(trim, OUTPUT, to_list, BOTTOM);
			Fork f = new Fork();
			connect(to_list, f);
			Filter fil = new Filter();
			connect(f, TOP, fil, TOP);
			ApplyFunction gt = new ApplyFunction(new FunctionTree(Numbers.isGreaterOrEqual,
					new FunctionTree(Numbers.subtraction,
							new FunctionTree(format.timestamp(), new NthElement(1)),
							new FunctionTree(format.timestamp(), new NthElement(0))),
					new Constant(gapLength)));
			connect(f, BOTTOM, gt, INPUT);
			connect(gt, OUTPUT, fil, BOTTOM);
			Sets.PutInto set = new Sets.PutInto();
			connect(fil, set);
			addProcessors(f0, to_list, trim, f, fil, gt, set);
			associateInput(f0);
			associateOutput(set);
		}});
		connect(feeder, slice);
		KeepLast last = new KeepLast();
		connect(slice, last);
		ApplyFunction indices = new ApplyFunction(
				new FunctionTree(new Bags.ApplyToAll(format.index()),
						new FunctionTree(Flatten.instance, Maps.values)));
		connect(last, indices);
		Pump p = new Pump();
		connect(indices, p);
		Print print = new Print(new PrintStream(os)).setSeparator("\n");
		connect(p, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		is.close();
		os.close();
		fs.close();
		System.out.println();
		System.out.println("Code runs successfully.");

	}

}
