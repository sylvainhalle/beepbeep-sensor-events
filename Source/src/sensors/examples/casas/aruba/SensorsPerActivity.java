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

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.Integrate;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.MergeTuples;
import ca.uqac.lif.cep.tuples.ScalarIntoTuple;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.casas.aruba.ArubaFormat;
import sensors.casas.aruba.ArubaLogRepository;

/**
 * Lists all the sensors that produce an event in an activity.
 */
public class SensorsPerActivity
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new ArubaLogRepository();
	
	/* The adapter for the event format. */
	protected static final ArubaFormat format = new ArubaFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("data");
		OutputStream os = fs.writeTo("acts.txt");
		Processor feeder = format.getFeeder(is);
		
		/* Create a tuple with an extra attribute containing the currently ongoing
		 * activity, if any. */
		Fork f = new Fork();
		connect(feeder, f);
		ApplyFunction to_f = new ApplyFunction(format.new GetUpdateActivity());
		connect(f, 0, to_f, 0);
		Integrate integ = new Integrate(format.new CurrentActivity());
		connect(to_f, integ);
		ApplyFunction added_tup = new ApplyFunction(new FunctionTree(new MergeTuples(2), StreamVariable.X, new FunctionTree(new ScalarIntoTuple("current"), StreamVariable.Y)));
		connect(f, 1, added_tup, 0);
		connect(integ, 0, added_tup, 1);
		
		Slice slice = new Slice(new FetchAttribute("current"), new GroupProcessor(1, 1) {{
			ApplyFunction sensor = new ApplyFunction(format.sensorString());
			Sets.PutInto put = new Sets.PutInto();
			connect(sensor, put);
			associateInput(sensor);
			associateOutput(put);
			addProcessors(sensor, put);
		}});
		connect(added_tup, slice);
		
		Pump p = new Pump();
		connect(slice, p);
		KeepLast last = new KeepLast();
		connect(p, last);
		Print print = new Print(new PrintStream(os)).setSeparator("\n");
		connect(last, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		is.close();
		os.close();
		fs.close();
	}

}
