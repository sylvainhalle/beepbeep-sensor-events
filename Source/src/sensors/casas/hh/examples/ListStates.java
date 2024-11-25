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

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.IdentityFunction;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.functions.UnaryFunction;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.tuples.TupleFeeder;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.PrettyPrint;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;
import sensors.nears.ReadTemperature;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Extracts all the states observed in a stream for each sensor, and counts the
 * number of times each one occurs. Optionally, numerical values can be
 * grouped into "bins" to produce data suitable for a graphical histogram.
 * <p>
 * The pipeline corresponding to this calculation is illustrated below:
 * <p>
 * <img src="{@docRoot}/doc-files/ListStates.png" alt="Pipeline" />
 *  
 * @author Sylvain Hallé
 */
public class ListStates
{
	/* The adapter for the event format. */
	protected static final EventFormat format = new HHFormat();
	protected static final LogRepository fs = new HHLogRepository("hh115");
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("casas-rawdata.txt");
		ReadLines reader = new ReadLines(is);
		TupleFeeder feeder = new TupleFeeder();
		Connector.connect(reader, feeder);
		OutputStream os = fs.writeTo("ListStates.txt");
		
		Slice s = new Slice(format.modelString(), 
				new Slice(format.sensorString(), 
						new GroupProcessor(1, 1) {{
							ApplyFunction f = new ApplyFunction(new FunctionTree(CoarsenValue.instance, format.stateString()));
							Slice in_s = new Slice(new IdentityFunction(), 
									new GroupProcessor(1, 1) {{ 
										TurnInto one = new TurnInto(1);
										Cumulate sum = new Cumulate(Numbers.addition);
										connect(one, sum);
										addProcessors(one, sum);
										associateInput(one).associateOutput(sum);
									}});
							connect(f, in_s);
							addProcessors(f, in_s).associateInput(f).associateOutput(in_s);
						}}
				)
		);
		connect(feeder, s);
		Pump p = new Pump();
		connect(s, p);
		KeepLast kl = new KeepLast();
		connect(p, kl);
		ApplyFunction pp = new ApplyFunction(new PrettyPrint());
		connect(kl, pp);
		connect(pp, new Print(new PrintStream(os)));
		p.run();
		os.close();
		fs.close();
		System.out.print("Code runs successfully.");

	}
	
	/**
	 * Coarsens numerical values by rounding them to the closest
	 * multiple of 10. This is a "custom" function that could be replaced by a
	 * more generic pipeline.
	 */
	public static class CoarsenValue extends UnaryFunction<Object,Object>
	{
		public static final CoarsenValue instance = new CoarsenValue();
		
		protected CoarsenValue()
		{
			super(Object.class, Object.class);
		}

		@Override
		public Object getValue(Object x)
		{
			return coarsen(x);
		}
		
		protected static Object coarsen(Object x)
		{
			if (x instanceof Number)
			{
				float f = ((Number) x).floatValue();
				return coarsen(f);
			}
			if (x instanceof Number)
			{
				float f = ((Number) x).floatValue();
				return (int) (Math.floor(f / 10f) * 10f);
			}
			if (x instanceof String)
			{
				String s = ((String) x).toString();
				if (s.endsWith("°C"))
				{
					return coarsen(ReadTemperature.readValue(s));
				}
				return s;
			}
			return x;
		}
	}
}
