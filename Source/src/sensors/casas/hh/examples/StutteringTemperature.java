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
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.IfThenElse;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Counts the number of times each sensor has emitted two successive identical
 * temperature readings. This may indicate an issue if temperature sensors are
 * expected to emit an event only when their reading <em>changes</em>. In that
 * case, identical readings may either indicate that an event is missing, or
 * that the sensor does not operate according to the documentation.
 * <p>
 * In the case of HH datasets, it is known that
 * <blockquote>
 * "[&hellip;] messages are sent when the sensor detects a change in the
 * measured temperature by 0.5 Celsius"
 * </blockquote>
 * Thus two successive identical readings should not occur.
 */
public class StutteringTemperature
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new HHLogRepository("hh130");
	
	/* The adapter for the event format. */
	protected static final EventFormat format = new HHFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("hh130.rawdata.txt");
		OutputStream os = fs.writeTo("stuttering-temp.txt");
		Processor feeder = format.getFeeder(is);
		
		/* Filter only temperature events. */
		FilterOn fil = new FilterOn(new FunctionTree(Equals.instance, format.modelString(), new Constant("T")));
		connect(feeder, fil);
		
		/* Determine successive events for each sensor that have the same value,
		 * and count these pairs. */
		Slice slice = new Slice(format.sensorString(),
				new GroupProcessor() {{
					Fork f = new Fork();
					ApplyFunction af = new ApplyFunction(new FunctionTree(IfThenElse.instance,
							new FunctionTree(Equals.instance,
									new FunctionTree(format.stateString(), StreamVariable.X),
									new FunctionTree(format.stateString(), StreamVariable.Y)),
							new Constant(1),
							new Constant(0)
							));
					connect(f, 0, af, 0);
					Trim trim = new Trim(1);
					connect(f, 1, trim, 0);
					connect(trim, 0, af, 1);
					Cumulate sum = new Cumulate(Numbers.addition);
					connect(af, sum);
					addProcessors(f, af, trim, sum);
					associateInput(f);
					associateOutput(sum);
		}});
		connect(fil, slice);
		
		/* Keep last state of the resulting map. */
		KeepLast last = new KeepLast();
		connect(slice, last);
		Pump p = new Pump();
		connect(last, p);
		Print print = new Print(new PrintStream(os));
		connect(p, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		is.close();
		os.close();
		fs.close();
	}
}
