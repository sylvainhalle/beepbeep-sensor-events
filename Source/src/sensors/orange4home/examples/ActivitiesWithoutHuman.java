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
package sensors.orange4home.examples;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.Integrate;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.UnaryFunction;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.MergeTuples;
import ca.uqac.lif.cep.tuples.ScalarIntoTuple;
import ca.uqac.lif.cep.tuples.Tuple;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystemException;
import sensors.CurrentActivity;
import sensors.LogRepository;
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;

/**
 * Fetches all labelled activities that do not involve "human activity" in the
 * Orange4Home dataset.
 */
public class ActivitiesWithoutHuman
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new Orange4HomeLogRepository();
	
	/* The adapter for the event format. */
	protected static final Orange4HomeFormat format = new Orange4HomeFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("o4h_all_events.csv");
		OutputStream os = fs.writeTo("acts-dup.txt");
		Processor feeder = format.getFeeder(is);
		
		/* Create a tuple with an extra attribute containing the currently ongoing
		 * activity, if any. */
		Fork f = new Fork();
		connect(feeder, f);
		ApplyFunction to_f = new ApplyFunction(format.new GetUpdateActivity());
		connect(f, 0, to_f, 0);
		Integrate integ = new Integrate(new CurrentActivity());
		connect(to_f, integ);
		ApplyFunction added_tup = new ApplyFunction(new FunctionTree(new MergeTuples(2), StreamVariable.X, new FunctionTree(new ScalarIntoTuple("current"), StreamVariable.Y)));
		connect(f, 1, added_tup, 0);
		connect(integ, 0, added_tup, 1);
		
		Slice slice = new Slice(new FetchAttribute("current"), new GroupProcessor(1, 1) {{
			ApplyFunction sensor = new ApplyFunction(format.sensorId());
			Sets.PutInto put = new Sets.PutInto();
			connect(sensor, put);
			associateInput(sensor);
			associateOutput(put);
			addProcessors(sensor, put);
		}});
		connect(feeder, slice);
		
		Pump p = new Pump();
		connect(slice, p);
		KeepLast last = new KeepLast();
		connect(p, last);
		ApplyFunction f_map = new ApplyFunction(new Maps.FilterMap(new FunctionTree(new HasNoMotion(), StreamVariable.Y)));
		connect(last, f_map);
		Print print = new Print(new PrintStream(os)).setSeparator("\n");
		connect(f_map, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		is.close();
		os.close();
		fs.close();
	}
	
	/**
	 * A custom defined function that determines if a set of strings does not
	 * contain any name for a motion sensor (these sensors start by "M").
	 */
	@SuppressWarnings("rawtypes")
	public static class HasNoMotion extends UnaryFunction<Collection,Boolean>
	{
		public HasNoMotion()
		{
			super(Collection.class, Boolean.class);
		}

		@Override
		public Boolean getValue(Collection x)
		{
			for (Object o : x)
			{
				Tuple t = (Tuple) o;
				String sensor = t.get(Orange4HomeFormat.SENSOR).toString();
				String subject = t.get(Orange4HomeFormat.SUBJECT).toString();
				if (sensor.compareTo("presence") == 0 || sensor.compareTo("door") == 0)
				{
					return false;
				}
				if (subject.compareTo("switch") == 0)
				{
					return false;
				}
			}
			return true;
		}
	}
}
