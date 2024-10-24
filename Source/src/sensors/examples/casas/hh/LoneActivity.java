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
package sensors.examples.casas.hh;

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
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tmf.Window;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.util.Bags;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Maps;
import ca.uqac.lif.cep.util.NthElement;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.cep.functions.IdentityFunction;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.PrettyPrint;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;

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
	protected static final HHFormat format = new HHFormat();
	
	protected static final LogRepository fs = new HHLogRepository("hh130");
	
	protected static final int s_windowWidth = 50;
	
	protected static final int s_threshold = 1;
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		fs.open();
		InputStream is = fs.readFrom("hh130.ann.txt");
		Processor feeder = format.getFeeder(is);
		OutputStream os = fs.writeTo("LoneActivities.txt");
		
		/* Create the pipeline. */
		Pump p = (Pump) connect(feeder,
				new GroupProcessor(1,1) {{
					ApplyFunction f = new ApplyFunction(new FetchAttribute(HHFormat.TXT_ACTIVITY));
					Window win = new Window(new GroupProcessor(1, 1) {{
						Slice s = new Slice(new IdentityFunction(1), new GroupProcessor(1, 1) {{
							TurnInto one = new TurnInto(1);
							Cumulate sum = new Cumulate(Numbers.addition);
							connect(one, sum);
							addProcessors(one, sum);
							associateInput(one);
							associateOutput(sum);
						}});
						addProcessors(s);
						associateInput(s);
						associateOutput(s);
          }}, s_windowWidth);
					connect(f, win);
					ApplyFunction filter = new ApplyFunction(new Maps.FilterMap(new FunctionTree(Numbers.isLessOrEqual, StreamVariable.Y, new Constant(s_threshold))));
					connect(win, filter);
					ApplyFunction not_empty = new ApplyFunction(new FunctionTree(Equals.instance, new FunctionTree(Bags.getSize, Maps.values), new Constant(1)));
					connect(filter, not_empty);
					addProcessors(f, win, filter); //, not_empty);
					associateInput(f);
					associateOutput(filter);
				}},
				new Pump());
		connect(p, new Print.Println(new PrintStream(os)));
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		os.close();
		fs.close();
		System.out.print("Code runs successfully.");
	}
}
