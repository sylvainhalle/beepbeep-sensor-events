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
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Identifies ZigBee heartbeats in a log that span a longer interval than
 * usual. This rule only applies to HH datasets after 2014. Their documentation
 * states that ZigBee sensors should send an "OK" message to confirm their
 * proper operating status every 30 minutes. This pipeline singles out all
 * successive heartbeats that span more than 60 minutes, for every sensor, and
 * prints their duration.
 */
public class ZigBeeHeartbeats
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new HHLogRepository("hh130");
	
	/* The adapter for the event format. */
	protected static final EventFormat format = new HHFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("hh130.rawdata.txt");
		OutputStream os = fs.writeTo("zigbee.txt");
		Processor feeder = format.getFeeder(is);
		
		/* Filter only ZigBee heartbeats. */
		FilterOn fil = new FilterOn(new FunctionTree(Equals.instance, format.modelString(), new Constant("ZB")));
		connect(feeder, fil);
		
		/* Determine intervals between heartbeats for each sensor, and filters out
		   those that span less than 60 minutes. */
		Slice slice = new Slice(format.sensorString(),
				new GroupProcessor() {{
					Fork f = new Fork();
					ApplyFunction af = new ApplyFunction(new FunctionTree(Numbers.division, 
							new FunctionTree(Numbers.subtraction,
									new FunctionTree(format.timestamp(), StreamVariable.Y),
									new FunctionTree(format.timestamp(), StreamVariable.X)),
							new Constant(1000 * 60))); // interval expressed in minutes
					connect(f, 0, af, 0);
					Trim trim = new Trim(1);
					connect(f, 1, trim, 0);
					connect(trim, 0, af, 1);
					FilterOn in_fil = new FilterOn(new FunctionTree(Numbers.isGreaterThan, StreamVariable.X, new Constant(60)));
					connect(af, in_fil);
					Sets.PutInto in_set = new Sets.PutInto();
					connect(in_fil, in_set);
					addProcessors(f, af, trim, in_fil, in_set);
					associateInput(f);
					associateOutput(in_set);
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
