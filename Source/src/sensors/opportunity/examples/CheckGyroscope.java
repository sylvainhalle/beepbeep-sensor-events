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
package sensors.opportunity.examples;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Call;
import ca.uqac.lif.cep.io.WriteOutputStream;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.mtnp.plot.gnuplot.Scatterplot;
import sensors.LogRepository;
import sensors.ToBytes;
import sensors.opportunity.FetchAttributeOrDefault;
import sensors.opportunity.OpportunityFormat;
import sensors.opportunity.OpportunityLogRepository;
import sensors.patterns.PlotPattern;

/**
 * Calculates the angular position deduced from the gyroscope data.
 * Unfortunately, the dataset does not specify the units of the gyroscope
 * values. We will assume that they are in mrad/s (since the documentation
 * does indicate that the original value (whatever that is, we suspect
 * rad/s) is multiplied by 1000.
 * @author Sylvain Hallé
 */
public class CheckGyroscope
{
	/* The adapter for the event format. */
	protected static OpportunityFormat format = new OpportunityFormat();

	/* The folder where the data files reside. */
	protected static final LogRepository fs = new OpportunityLogRepository();

	public static void main(String[] args) throws FileSystemException, IOException
	{
		boolean call_gp = true, integrate = false;;
		
		String dataset_nb = "1"; // 1, 2, 3, or 4
		String sensor_name = "BACK";
		//String unit_type = "Accelerometer";
		String unit_type = "InertialMeasurementUnit";

		fs.open();
		InputStream is = fs.readFrom("S" + dataset_nb +"-ADL1.dat");
		OutputStream os = fs.writeTo(unit_type + "_gyro_" + dataset_nb + "_" + sensor_name + (call_gp ? ".png" : ".gp"));
		Processor feeder = format.getRawFeeder(is);
		PlotPattern plot = new PlotPattern(new String[] {"t", "x", "y", "z"},
				new Scatterplot().withPoints(false).withLines(true),
				new ApplyFunction(new FunctionTree(Numbers.division, format.timestamp(), new Constant(1000))),
				/*
				new ApplyFunction(new FetchAttributeOrDefault(unit_type + " " + sensor_name + " gyroX", 0)),
				new ApplyFunction(new FetchAttributeOrDefault(unit_type + " " + sensor_name + " gyroY", 0)),
				new ApplyFunction(new FetchAttributeOrDefault(unit_type + " " + sensor_name + " gyroZ", 0))
				*/
				new IntegrateAcceleration(unit_type + " " + sensor_name + " gyroX"),
				new IntegrateAcceleration(unit_type + " " + sensor_name + " gyroY"),
				new IntegrateAcceleration(unit_type + " " + sensor_name + " gyroZ")
				);
		connect(feeder, plot);
		ApplyFunction to_bytes = new ApplyFunction(ToBytes.instance);
		connect(plot, to_bytes);
		Pump p = new Pump();
		connect(to_bytes, p);
		if (call_gp)
		{
			Call draw = new Call("gnuplot");
			connect(p, draw);
			WriteOutputStream print = new WriteOutputStream(os);
			connect(draw, print);
		}
		else
		{
			WriteOutputStream print = new WriteOutputStream(os);
			connect(p, print);
		}

		/* Run the pipeline. */
		p.run();

		/* Close the resources. */
		is.close();
		os.close();
		fs.close();
	}

	public static class IntegrateAcceleration extends GroupProcessor
	{
		/**
		 * The sensor from which to fetch the acceleration data.
		 */
		protected final String m_sensor;

		/**
		 * Creates a new instance of the processor.
		 * @param sensor The sensor from which to fetch the acceleration data
		 */
		public IntegrateAcceleration(String sensor)
		{
			super(1, 1);
			m_sensor = sensor;
			Fork fork1 = new Fork(2);		
			Fork fork2 = new Fork(2);
			connect(fork1, 0, fork2, 0);
			Trim trim = new Trim(1);
			connect(fork2, 0, trim, 0);
			ApplyFunction duration = new ApplyFunction(new FunctionTree(Numbers.subtraction,
					new FunctionTree(format.timestamp(), StreamVariable.X),
					new FunctionTree(format.timestamp(), StreamVariable.Y)));
			connect(trim, 0, duration, 0);
			connect(fork2, 1, duration, 1);
			ApplyFunction speed_delta = new ApplyFunction(new FunctionTree(Numbers.division,
					new FunctionTree(Numbers.multiplication,
					StreamVariable.X, new FunctionTree(
							new FetchAttributeOrDefault(sensor, 0), StreamVariable.Y)),
					new Constant(2 * Math.PI * 1000000f)));
			// We divide by 2 * pi * 1M, since time is in ms and angular velocity is in mrad/s,
			// in order to get the number of complete "turns".
			connect(duration, 0, speed_delta, 0);
			connect(fork1, 1, speed_delta, 1);
			Cumulate integrate = new Cumulate(Numbers.addition);
			connect(speed_delta, integrate);
			addProcessors(fork1, fork2, trim, duration, speed_delta, integrate);
			associateInput(0, fork1, 0);
			associateOutput(0, integrate, 0);
		}

		@Override
		public IntegrateAcceleration duplicate(boolean with_state)
		{
			if (with_state)
			{
				throw new UnsupportedOperationException("Cannot duplicate with state");
			}
			return new IntegrateAcceleration(m_sensor);
		}
	}

}
