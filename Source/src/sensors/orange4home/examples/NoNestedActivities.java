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

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.UnaryFunction;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.FixedTupleBuilder;
import ca.uqac.lif.cep.tuples.TupleFeeder;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Strings;
import ca.uqac.lif.fs.FileSystemException;
import sensors.LogRepository;
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;

/**
 * Checks whether overlapping or nested activity labels are present in the
 * Orange4Home dataset.
 */
public class NoNestedActivities
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new Orange4HomeLogRepository();
	
	/* The adapter for the event format. */
	protected static final Orange4HomeFormat format = new Orange4HomeFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("o4h_all_events.csv");
		OutputStream os = fs.writeTo("nested.txt");
		Processor read = new ReadLines(is);
		TupleFeeder feeder = new TupleFeeder(new FixedTupleBuilder("Time", "ItemName", "Value"));
		connect(read, feeder);
		
		FilterOn fo = new FilterOn(new FunctionTree(Equals.instance, new FetchAttribute("ItemName"), new Constant("label")));
		connect(feeder, fo);
		Fork f0 = new Fork();
		connect(fo, f0);
		Fork f = new Fork();
		connect(f0, 1, f, 0);
		Trim trim = new Trim(1);
		connect(f, 1, trim, 0);
		ApplyFunction eq = new ApplyFunction(new FunctionTree(Booleans.or,
				new FunctionTree(Booleans.and,
						new FunctionTree(Strings.startsWith, new FunctionTree(new FetchAttribute("Value"), StreamVariable.X), new Constant("START")),
						new FunctionTree(Strings.startsWith, new FunctionTree(new FetchAttribute("Value"), StreamVariable.Y), new Constant("START"))
						),
				new FunctionTree(Booleans.and,
						new FunctionTree(Strings.startsWith, new FunctionTree(new FetchAttribute("Value"), StreamVariable.X), new Constant("STOP")),
						new FunctionTree(Strings.startsWith, new FunctionTree(new FetchAttribute("Value"), StreamVariable.Y), new Constant("STOP"))
						)));
		connect(f, 0, eq, 0);
		connect(trim, 0, eq, 1);
		Filter fil = new Filter();
		connect(f0, 0, fil, 0);
		connect(eq, 0, fil, 1);
		//Cumulate all = new Cumulate(Booleans.or);
		//connect(eq, all);
		

		Pump p = new Pump();
		connect(fil, p);
		//KeepLast last = new KeepLast();
		//connect(p, last);
		Print print = new Print(new PrintStream(os)).setSeparator("\n");
		connect(p, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		is.close();
		os.close();
		fs.close();
	}
	
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
				if (o.toString().startsWith("M"))
				{
					return false;
				}
			}
			return true;
		}
		
		
	}

}
