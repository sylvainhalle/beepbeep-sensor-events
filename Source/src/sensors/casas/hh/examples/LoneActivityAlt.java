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
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tmf.Window;
import ca.uqac.lif.cep.tmf.WindowFunction;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.util.Bags;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.NthElement;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.functions.IdentityFunction;
import ca.uqac.lif.cep.functions.RaiseArity;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.fs.FileSystemException;
import sensors.LogRepository;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;
import sensors.patterns.CounterPattern;
import sensors.patterns.SuccessivePattern;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Retrieves the list of all sensors, grouped by ID, and collects all the
 * locations for which the sensor is reported.
 * 
 * @author Sylvain Hallé
 */
public class LoneActivityAlt
{
	/* The adapter for the event format. */
	protected static final HHFormat format = new HHFormat();

	protected static final LogRepository fs = new HHLogRepository("hh115");

	protected static final int s_windowWidth = 50;

	protected static final int s_threshold = 1;

	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		fs.open();
		InputStream is = fs.readFrom("hh115.ann.txt");
		Processor feeder = format.getFeeder(is);
		OutputStream os = fs.writeTo("LoneActivities.txt");

		/* Create the pipeline. */
		Fork fk = new Fork();
		connect(feeder, fk);
		//ApplyFunction cnt = new ApplyFunction(format.timestamp());
		CounterPattern cnt = new CounterPattern();
		connect(fk, 0, cnt, 0);

		ApplyFunction f = new ApplyFunction(new FetchAttribute(HHFormat.TXT_ACTIVITY));
		connect(fk, 1, f, 0);
		WindowFunction win = new WindowFunction(new RaiseArity(3, new FunctionTree(Booleans.and,
				new FunctionTree(Booleans.not, new FunctionTree(Equals.instance, StreamVariable.X, StreamVariable.Y)),
				new FunctionTree(Booleans.not, new FunctionTree(Equals.instance, StreamVariable.Y, StreamVariable.Z))
				)));
		connect(f, win);
		/*
		SuccessivePattern succ = new SuccessivePattern(new FunctionTree(Booleans.and,
				new FunctionTree(Equals.instance, new FunctionTree(Bags.getSize, new FunctionTree(Maps.values, StreamVariable.X)), new Constant(1)),
				new FunctionTree(Equals.instance, new FunctionTree(Bags.getSize, new FunctionTree(Maps.values, StreamVariable.Y)), new Constant(1))
				));
		connect(filter, succ);
		*/
		ApplyFunction to_list = new ApplyFunction(new Bags.ToList(2));
		connect(cnt, 0, to_list, 0);
		connect(win, 0, to_list, 1);
		//FilterOn not_empty = new FilterOn(new NthElement(1));
		//connect(to_list, not_empty);

		Pump p = new Pump();
		connect(to_list, p);
		connect(p, new Print.Println(new PrintStream(os)));

		/* Run the pipeline. */
		p.run();

		/* Close the resources. */
		os.close();
		fs.close();
		System.out.print("Code runs successfully.");
	}
}
