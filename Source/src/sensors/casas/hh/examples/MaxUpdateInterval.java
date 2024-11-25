/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023-2024 Sylvain Hallé, Rania Taleb

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

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.PrettyPrintStream;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * For every sensor, determines the minimum/maximum/average interval between
 * successive updates. This can be used to establish a "profile" of the
 * frequency at which a sensor is expected to send events.
 * <p>
 * The pipeline corresponding to this calculation is illustrated below:
 * <p>
 * <img src="{@docRoot}/doc-files/MaxUpdateInterval.png" alt="Pipeline" />
 * 
 * @author Sylvain Hallé
 *
 */
public class MaxUpdateInterval
{
	/* The adapter for the event format. */
	protected static final EventFormat format = new HHFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		LogRepository fs = new HHLogRepository("hh130").open();
		InputStream is = fs.readFrom("hh130.rawdata.txt");
		OutputStream os = fs.writeTo("MaxUpdateInterval.txt");
		Processor feeder = format.getFeeder(is);
		
		Slice s = new Slice(format.sensorId(),
				new GroupProcessor(1, 1) {{
					ApplyFunction get_ts = new ApplyFunction(new FunctionTree(Numbers.division,
							format.timestamp(), new Constant(1000f)));
					Fork f1 = new Fork(2);
					connect(get_ts, f1);
					Trim t = new Trim(1);
					connect(f1, 0, t, 0);
					ApplyFunction diff = new ApplyFunction(Numbers.subtraction);
					connect(t, 0, diff, 0);
					connect(f1, 1, diff, 1);
					Fork f2 = new Fork(3);
					connect(diff, f2);
					// Minimum interval
					Cumulate min = new Cumulate(Numbers.minimum);
					connect(f2, 0, min, 0);
					// Average interval
					Fork f3 = new Fork(2);
					connect(f2, 1, f3, 0);
					Cumulate sum = new Cumulate(Numbers.addition);
					connect(f3, 0, sum, 0);
					TurnInto one = new TurnInto(1);
					connect(f3, 1, one, 0);
					Cumulate cnt = new Cumulate(Numbers.addition);
					connect(one, cnt);
					ApplyFunction div = new ApplyFunction(Numbers.division);
					connect(sum, 0, div, 0);
					connect(cnt, 0, div, 1);
					// Maximum interval
					Cumulate max = new Cumulate(Numbers.maximum);
					connect(f2, 2, max, 0);
					ApplyFunction tuple = new ApplyFunction(new MergeScalars("min", "avg", "max"));
					connect(min, 0, tuple, 0);
					connect(div, 0, tuple, 1);
					connect(max, 0, tuple, 2);
					addProcessors(get_ts, f1, t, diff, f2, min, f3, sum, one, cnt, div, max, tuple).associateInput(get_ts).associateOutput(tuple);
				}});
		connect(feeder, s);
		Pump p = new Pump();
		connect(s, p);
		KeepLast kl = new KeepLast();
		connect(p, kl);
		Print print = new Print(new PrettyPrintStream(os));
		connect(kl, print);
		
		p.run();
		os.close();
		fs.close();
		System.out.print("Code runs successfully.");
	}
}
