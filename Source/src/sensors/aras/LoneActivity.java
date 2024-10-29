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
package sensors.aras;

import java.io.IOException;
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
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.util.Bags;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.NthElement;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.functions.IdentityFunction;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.fs.FileSystemException;
import sensors.patterns.CounterPattern;
import sensors.patterns.SuccessivePattern;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Retrieves the list of all sensors, grouped by ID, and collects all the
 * locations for which the sensor is reported.
 * 
 * @author Sylvain Hallé
 */
public class LoneActivity
{
	/* The adapter for the event format. */
	protected static final ArasFormat format = new ArasFormatHouseA();

	protected static final ArasLogRepository fs = new ArasLogRepository("House A");

	protected static final int s_windowWidth = 10;

	protected static final int s_threshold = 1;

	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		fs.open();
		Processor feeder = format.getRawFeeder(null, fs.getLogFiles());
		OutputStream os = fs.writeTo("LoneActivities.txt");

		/* Create the pipeline. */
		Fork fk = new Fork();
		connect(feeder, fk);
		CounterPattern cnt = new CounterPattern();
		connect(fk, 0, cnt, 0);

		ApplyFunction f = new ApplyFunction(new FetchAttribute(ArasFormat.P_ACTIVITY1));
		connect(fk, 1, f, 0);
		Window win = new Window(new Slice(new IdentityFunction(1), new GroupProcessor(1, 1) {{
				TurnInto one = new TurnInto(1);
				Cumulate sum = new Cumulate(Numbers.addition);
				connect(one, sum);
				addProcessors(one, sum);
				associateInput(one);
				associateOutput(sum);
		}}), s_windowWidth);
		connect(f, win);
		ApplyFunction filter = new ApplyFunction(new Maps.FilterMap(new FunctionTree(Numbers.isLessOrEqual, StreamVariable.Y, new Constant(s_threshold))));
		connect(win, filter);
		ApplyFunction to_list = new ApplyFunction(new Bags.ToList(2));
		connect(cnt, 0, to_list, 0);
		connect(filter, 0, to_list, 1);
		FilterOn not_empty = new FilterOn(new FunctionTree(Equals.instance, new FunctionTree(Bags.getSize, new FunctionTree(Maps.values, new NthElement(1))), new Constant(1)));
		connect(to_list, not_empty);
		SuccessivePattern succ = new SuccessivePattern(new FunctionTree(Booleans.and, new FunctionTree(new NthElement(1), StreamVariable.X), new FunctionTree(new NthElement(1), StreamVariable.Y)));
		connect(not_empty, succ);
		Pump p = new Pump();
		connect(succ, p);
		connect(p, new Print.Println(new PrintStream(os)));

		/* Run the pipeline. */
		p.run();

		/* Close the resources. */
		os.close();
		fs.close();
		System.out.print("Code runs successfully.");
	}
}
