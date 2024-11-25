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
package sensors.nears.examples;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Queue;

import ca.uqac.lif.cep.Duplicable;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionException;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.Integrate;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.UnaryFunction;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import sensors.BoxAndWhiskers;
import sensors.EventFormat;
import sensors.MultiDaySource;
import sensors.PrettyPrint;
import sensors.house.PowerState;
import sensors.nears.NearsJsonFormat;
import sensors.nears.NearsLogRepository;
import sensors.nears.NearsMultiDaySource;
import sensors.nears.examples.PowerLaw.ExcursionEpisode;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Checks that current (I), power (P) and voltage (V) in every energy sensor
 * roughly follow the equation P = VI. The output of the pipeline is a map
 * where every sensor is associated with a box-and-whiskers "plot" of the ratio
 * P / (IV).
 */
public class PowerLaw
{
	/* The adapter for the event format. */
	protected static EventFormat format = new NearsJsonFormat();

	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		FileSystem fs = new NearsLogRepository("0032").open();
		//MultiDaySource feeder = new NearsMultiDaySource(fs, 1, 281);
		Processor feeder = format.getFeeder(fs.readFrom("nears-hub-0032-sorted.json"));
		OutputStream os = fs.writeTo("PowerLaw.txt");

		// Filter only energy events
		FilterOn fil = new FilterOn(new FunctionTree(Booleans.or,
				new FunctionTree(Booleans.or,
						new FunctionTree(Equals.instance, format.sensorString(), new Constant("instant_current")),
						new FunctionTree(Equals.instance, format.sensorString(), new Constant("instant_voltage"))),
				new FunctionTree(Equals.instance, format.sensorString(), new Constant("instant_power"))));
		connect(feeder, fil);

		Slice slice = new Slice(format.sensorPlacement(), new GroupProcessor(1, 1) {{
			Fork f = new Fork();
			// Branch 1: timestamp
			ApplyFunction get_ts = new ApplyFunction(format.timestamp());
			connect(f, 0, get_ts, 0);
			// Branch 2: P/VI ratio
			ApplyFunction to_f = new ApplyFunction(new PowerState.EventAsFunction(format));
			connect(f, 1, to_f, 0);
			Integrate in = new Integrate(new PowerState());
			connect(to_f, in);
			ApplyFunction pvi = new ApplyFunction(new PVIRatio());
			connect(in, pvi);
			ExcursionEpisode ee = new PowerLaw.ExcursionEpisode();
			connect(get_ts, 0, ee, 0);
			connect(pvi, 0, ee, 1);
			FilterOn last_fil = new FilterOn(new FunctionTree(Numbers.isGreaterOrEqual, StreamVariable.X, new Constant(30)));
			connect(ee, last_fil);
			Sets.PutInto put = new Sets.PutInto();
			connect(last_fil, put);
			addProcessors(f, get_ts, to_f, in, pvi, ee, last_fil, put).associateInput(f).associateOutput(put);
		}});
		connect(fil, slice);
		Pump p = new Pump();
		connect(slice, p);
		KeepLast kl = new KeepLast();
		connect(p, kl);
		ApplyFunction pp = new ApplyFunction(new PrettyPrint());
		connect(kl, pp);
		connect(pp, new Print.Println(new PrintStream(os)));

		/* Run the pipeline. */
		p.run();

		/* Close the resources. */
		os.close();
		fs.close();
	}

	/**
	 * For a given {@link PowerState}, computes the ratio P / (IV). This ratio
	 * should be close to 1 if the power law is respected.
	 */
	public static class PVIRatio extends UnaryFunction<PowerState,Number>
	{
		public PVIRatio()
		{
			super(PowerState.class, Number.class);
		}

		@Override
		public Number getValue(PowerState x)
		{
			if (x.getCurrent() == 0 || x.getVoltage() == 0)
			{
				return 0;
			}
			return x.getPower() / (x.getCurrent() * x.getVoltage());
		}
	}

	public static class ExcursionEpisode extends SynchronousProcessor
	{
		protected long m_beginTs = -1;

		public ExcursionEpisode()
		{
			super(2, 1);
		}

		@Override
		protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
		{
			long ts = (Long) inputs[0];
			double value = ((Number) inputs[1]).doubleValue();
			if (value > 0 && (value < 0.5 || value > 2))
			{
				if (m_beginTs == -1)
				{
					m_beginTs = ts;
				}
			}
			else
			{
				if (m_beginTs != -1)
				{
					outputs.add(new Object[] { (ts - m_beginTs) / 1000 });
					m_beginTs = -1;
				}
			}
			return true;
		}

		@Override
		public Processor duplicate(boolean with_state)
		{
			return new ExcursionEpisode();
		}
	}
}
