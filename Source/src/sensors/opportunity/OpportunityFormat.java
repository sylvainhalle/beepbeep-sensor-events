/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023-2024 Sylvain Hallé, Rania Taleb

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
package sensors.opportunity;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Queue;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.cep.UniformProcessor;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.RaiseArity;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.Tuple;
import ca.uqac.lif.cep.tuples.TupleMap;
import sensors.LabeledEventFormat;
import sensors.ReadLinesStatus;

/**
 * Format for the Opportunity dataset. Log lines in this format are made of 250
 * columns, separated by spaces. The first column is a timestamp, the next 242
 * are individual sensor readings, and the last 7 are activity labels.
 * <p>
 * Column names for sensor values are similar to the following:
 * <pre>InertialMeasurementUnit BACK accX</pre>
 * From such a string, the first token is considered as the "model" of the
 * sensor, the second is the "subject", and the third is the "sensor" itself.
 */
public class OpportunityFormat implements LabeledEventFormat
{
	/**
	 * The names of the columns in the Opportunity dataset.
	 */
	public static final String[] COL_NAMES = new String[] { "MILLISEC", "Accelerometer RKN^ accX",
			"Accelerometer RKN^ accY", "Accelerometer RKN^ accZ", "Accelerometer HIP accX",
			"Accelerometer HIP accY", "Accelerometer HIP accZ", "Accelerometer LUA^ accX",
			"Accelerometer LUA^ accY", "Accelerometer LUA^ accZ", "Accelerometer RUA_ accX",
			"Accelerometer RUA_ accY", "Accelerometer RUA_ accZ", "Accelerometer LH accX",
			"Accelerometer LH accY", "Accelerometer LH accZ", "Accelerometer BACK accX",
			"Accelerometer BACK accY", "Accelerometer BACK accZ", "Accelerometer RKN_ accX",
			"Accelerometer RKN_ accY", "Accelerometer RKN_ accZ", "Accelerometer RWR accX",
			"Accelerometer RWR accY", "Accelerometer RWR accZ", "Accelerometer RUA^ accX",
			"Accelerometer RUA^ accY", "Accelerometer RUA^ accZ", "Accelerometer LUA_ accX",
			"Accelerometer LUA_ accY", "Accelerometer LUA_ accZ", "Accelerometer LWR accX",
			"Accelerometer LWR accY", "Accelerometer LWR accZ", "Accelerometer RH accX",
			"Accelerometer RH accY", "Accelerometer RH accZ", "InertialMeasurementUnit BACK accX",
			"InertialMeasurementUnit BACK accY", "InertialMeasurementUnit BACK accZ",
			"InertialMeasurementUnit BACK gyroX", "InertialMeasurementUnit BACK gyroY",
			"InertialMeasurementUnit BACK gyroZ", "InertialMeasurementUnit BACK magneticX",
			"InertialMeasurementUnit BACK magneticY", "InertialMeasurementUnit BACK magneticZ",
			"InertialMeasurementUnit BACK Quaternion1", "InertialMeasurementUnit BACK Quaternion2",
			"InertialMeasurementUnit BACK Quaternion3", "InertialMeasurementUnit BACK Quaternion4",
			"InertialMeasurementUnit RUA accX", "InertialMeasurementUnit RUA accY",
			"InertialMeasurementUnit RUA accZ", "InertialMeasurementUnit RUA gyroX",
			"InertialMeasurementUnit RUA gyroY", "InertialMeasurementUnit RUA gyroZ",
			"InertialMeasurementUnit RUA magneticX", "InertialMeasurementUnit RUA magneticY",
			"InertialMeasurementUnit RUA magneticZ", "InertialMeasurementUnit RUA Quaternion1",
			"InertialMeasurementUnit RUA Quaternion2", "InertialMeasurementUnit RUA Quaternion3",
			"InertialMeasurementUnit RUA Quaternion4", "InertialMeasurementUnit RLA accX",
			"InertialMeasurementUnit RLA accY", "InertialMeasurementUnit RLA accZ",
			"InertialMeasurementUnit RLA gyroX", "InertialMeasurementUnit RLA gyroY",
			"InertialMeasurementUnit RLA gyroZ", "InertialMeasurementUnit RLA magneticX",
			"InertialMeasurementUnit RLA magneticY", "InertialMeasurementUnit RLA magneticZ",
			"InertialMeasurementUnit RLA Quaternion1", "InertialMeasurementUnit RLA Quaternion2",
			"InertialMeasurementUnit RLA Quaternion3", "InertialMeasurementUnit RLA Quaternion4",
			"InertialMeasurementUnit LUA accX", "InertialMeasurementUnit LUA accY",
			"InertialMeasurementUnit LUA accZ", "InertialMeasurementUnit LUA gyroX",
			"InertialMeasurementUnit LUA gyroY", "InertialMeasurementUnit LUA gyroZ",
			"InertialMeasurementUnit LUA magneticX", "InertialMeasurementUnit LUA magneticY",
			"InertialMeasurementUnit LUA magneticZ", "InertialMeasurementUnit LUA Quaternion1",
			"InertialMeasurementUnit LUA Quaternion2", "InertialMeasurementUnit LUA Quaternion3",
			"InertialMeasurementUnit LUA Quaternion4", "InertialMeasurementUnit LLA accX",
			"InertialMeasurementUnit LLA accY", "InertialMeasurementUnit LLA accZ",
			"InertialMeasurementUnit LLA gyroX", "InertialMeasurementUnit LLA gyroY",
			"InertialMeasurementUnit LLA gyroZ", "InertialMeasurementUnit LLA magneticX",
			"InertialMeasurementUnit LLA magneticY", "InertialMeasurementUnit LLA magneticZ",
			"InertialMeasurementUnit LLA Quaternion1", "InertialMeasurementUnit LLA Quaternion2",
			"InertialMeasurementUnit LLA Quaternion3", "InertialMeasurementUnit LLA Quaternion4",
			"InertialMeasurementUnit L-SHOE EuX", "InertialMeasurementUnit L-SHOE EuY",
			"InertialMeasurementUnit L-SHOE EuZ", "InertialMeasurementUnit L-SHOE Nav_Ax",
			"InertialMeasurementUnit L-SHOE Nav_Ay", "InertialMeasurementUnit L-SHOE Nav_Az",
			"InertialMeasurementUnit L-SHOE Body_Ax", "InertialMeasurementUnit L-SHOE Body_Ay",
			"InertialMeasurementUnit L-SHOE Body_Az", "InertialMeasurementUnit L-SHOE AngVelBodyFrameX",
			"InertialMeasurementUnit L-SHOE AngVelBodyFrameY",
			"InertialMeasurementUnit L-SHOE AngVelBodyFrameZ",
			"InertialMeasurementUnit L-SHOE AngVelNavFrameX",
			"InertialMeasurementUnit L-SHOE AngVelNavFrameY",
			"InertialMeasurementUnit L-SHOE AngVelNavFrameZ", "InertialMeasurementUnit L-SHOE Compass",
			"InertialMeasurementUnit R-SHOE EuX", "InertialMeasurementUnit R-SHOE EuY",
			"InertialMeasurementUnit R-SHOE EuZ", "InertialMeasurementUnit R-SHOE Nav_Ax",
			"InertialMeasurementUnit R-SHOE Nav_Ay", "InertialMeasurementUnit R-SHOE Nav_Az",
			"InertialMeasurementUnit R-SHOE Body_Ax", "InertialMeasurementUnit R-SHOE Body_Ay",
			"InertialMeasurementUnit R-SHOE Body_Az", "InertialMeasurementUnit R-SHOE AngVelBodyFrameX",
			"InertialMeasurementUnit R-SHOE AngVelBodyFrameY",
			"InertialMeasurementUnit R-SHOE AngVelBodyFrameZ",
			"InertialMeasurementUnit R-SHOE AngVelNavFrameX",
			"InertialMeasurementUnit R-SHOE AngVelNavFrameY",
			"InertialMeasurementUnit R-SHOE AngVelNavFrameZ", "InertialMeasurementUnit R-SHOE Compass",
			"Accelerometer CUP accX",
			"Accelerometer CUP accY",
			"Accelerometer CUP accZ",
			"Accelerometer CUP gyroX",
			"Accelerometer CUP gyroY",
			"Accelerometer SALAMI accX",
			"Accelerometer SALAMI accY",
			"Accelerometer SALAMI accZ",
			"Accelerometer SALAMI gyroX",
			"Accelerometer SALAMI gyroY",
			"Accelerometer WATER accX",
			"Accelerometer WATER accY",
			"Accelerometer WATER accZ",
			"Accelerometer WATER gyroX",
			"Accelerometer WATER gyroY",
			"Accelerometer CHEESE accX",
			"Accelerometer CHEESE accY",
			"Accelerometer CHEESE accZ",
			"Accelerometer CHEESE gyroX",
			"Accelerometer CHEESE gyroY",
			"Accelerometer BREAD accX",
			"Accelerometer BREAD accY",
			"Accelerometer BREAD accZ",
			"Accelerometer BREAD gyroX",
			"Accelerometer BREAD gyroY",
			"Accelerometer KNIFE1 accX",
			"Accelerometer KNIFE1 accY",
			"Accelerometer KNIFE1 accZ",
			"Accelerometer KNIFE1 gyroX",
			"Accelerometer KNIFE1 gyroY",
			"Accelerometer MILK accX",
			"Accelerometer MILK accY",
			"Accelerometer MILK accZ",
			"Accelerometer MILK gyroX",
			"Accelerometer MILK gyroY",
			"Accelerometer SPOON accX",
			"Accelerometer SPOON accY",
			"Accelerometer SPOON accZ",
			"Accelerometer SPOON gyroX",
			"Accelerometer SPOON gyroY",
			"Accelerometer SUGAR accX",
			"Accelerometer SUGAR accY",
			"Accelerometer SUGAR accZ",
			"Accelerometer SUGAR gyroX",
			"Accelerometer SUGAR gyroY",
			"Accelerometer KNIFE2 accX",
			"Accelerometer KNIFE2 accY",
			"Accelerometer KNIFE2 accZ",
			"Accelerometer KNIFE2 gyroX",
			"Accelerometer KNIFE2 gyroY",
			"Accelerometer PLATE accX",
			"Accelerometer PLATE accY",
			"Accelerometer PLATE accZ",
			"Accelerometer PLATE gyroX",
			"Accelerometer PLATE gyroY",
			"Accelerometer GLASS accX",
			"Accelerometer GLASS accY",
			"Accelerometer GLASS accZ",
			"Accelerometer GLASS gyroX",
			"Accelerometer GLASS gyroY",
			"REED SWITCH DISHWASHER S1", "REED SWITCH FRIDGE S3", "REED SWITCH FRIDGE S2",
			"REED SWITCH FRIDGE S1", "REED SWITCH MIDDLEDRAWER S1", "REED SWITCH MIDDLEDRAWER S2",
			"REED SWITCH MIDDLEDRAWER S3", "REED SWITCH LOWERDRAWER S3", "REED SWITCH LOWERDRAWER S2",
			"REED SWITCH UPPERDRAWER", "REED SWITCH DISHWASHER S3", "REED SWITCH LOWERDRAWER S1",
			"REED SWITCH DISHWASHER S2", "Accelerometer DOOR1 accX", "Accelerometer DOOR1 accY",
			"Accelerometer DOOR1 accZ", "Accelerometer LAZYCHAIR accX", "Accelerometer LAZYCHAIR accY",
			"Accelerometer LAZYCHAIR accZ", "Accelerometer DOOR2 accX", "Accelerometer DOOR2 accY",
			"Accelerometer DOOR2 accZ", "Accelerometer DISHWASHER accX", "Accelerometer DISHWASHER accY",
			"Accelerometer DISHWASHER accZ", "Accelerometer UPPERDRAWER accX",
			"Accelerometer UPPERDRAWER accY", "Accelerometer UPPERDRAWER accZ",
			"Accelerometer LOWERDRAWER accX", "Accelerometer LOWERDRAWER accY",
			"Accelerometer LOWERDRAWER accZ", "Accelerometer MIDDLEDRAWER accX",
			"Accelerometer MIDDLEDRAWER accY", "Accelerometer MIDDLEDRAWER accZ",
			"Accelerometer FRIDGE accX", "Accelerometer FRIDGE accY", "Accelerometer FRIDGE accZ",
			"LOCATION TAG1 X", "LOCATION TAG1 Y", "LOCATION TAG1 Z", "LOCATION TAG2 X", "LOCATION TAG2 Y",
			"LOCATION TAG2 Z", "LOCATION TAG3 X", "LOCATION TAG3 Y", "LOCATION TAG3 Z", "LOCATION TAG4 X",
			"LOCATION TAG4 Y", "LOCATION TAG4 Z", "Locomotion", "HL_Activity", "LL_Left_Arm",
			"LL_Left_Arm_Object", "LL_Right_Arm", "LL_Right_Arm_Object", "ML_Both_Arms" };

	/**
	 * The constant used to represent the "on" state of a sensor.
	 */
	public static final String S_ON = "1";

	/**
	 * The constant used to represent the "off" state of a sensor.
	 */
	public static final String S_OFF = "0";

	/**
	 * The name of the attribute containing the timestamp in an event.
	 */
	public static final String P_TIMESTAMP = COL_NAMES[0];

	/**
	 * The name of the attribute containing the location in an event.
	 */
	public static final String P_LOCATION = "location";

	/**
	 * The name of the attribute containing the subject in an event.
	 */
	public static final String P_SUBJECT = "subject";

	/**
	 * The name of the attribute containing the model in an event.
	 */
	public static final String P_MODEL = "model";

	/**
	 * The name of the attribute containing the sensor in an event.
	 */
	public static final String P_SENSOR = "sensor";

	/**
	 * The name of the attribute containing the state in an event.
	 */
	public static final String P_STATE = "state";

	/**
	 * The name of the attribute containing the index in an event.
	 */
	public static final String P_INDEX = "index";

	@Override
	public Date parseDate(String s)
	{
		// There are no dates to parse in the Opportunity dataset
		return null;
	}

	@Override
	public Function timestamp()
	{
		return new FetchAttribute(P_TIMESTAMP);
	}

	@Override
	public Function stateString()
	{
		return new FetchAttribute(P_STATE);
	}

	@Override
	public Function locationString()
	{
		// There is no notion of sensor placement in this dataset
		return null;
	}

	@Override
	public Function subjectString()
	{
		return new FetchAttribute(P_SUBJECT);
	}

	@Override
	public Function modelString()
	{
		return new FetchAttribute(P_MODEL);
	}

	@Override
	public Function sensorString()
	{
		return new FetchAttribute(P_SENSOR);
	}

	@Override
	public Function sensorPlacement()
	{
		// There is no notion of sensor placement in this dataset
		return null;
	}

	@Override
	public Function sensorId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Function index()
	{
		return new FetchAttribute(P_INDEX);
	}

	@Override
	public Function isTemperature()
	{
		// No event in this format is a temperature reading
		return new RaiseArity(1, new Constant(false));
	}

	@Override
	public Tuple createPlacement(String location, String subject, String model)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tuple createId(String location, String subject, String model, String sensor)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getOnConstant()
	{
		return S_ON;
	}

	@Override
	public Object getOffConstant()
	{
		return S_OFF;
	}

	@Override
	public Object getOpenConstant()
	{
		return S_ON;
	}

	@Override
	public Object getClosedConstant()
	{
		return S_OFF;
	}

	@Override
	public String getExtension()
	{
		return "dat";
	}

	@Override
	public Processor getFeeder(InputStream is)
	{
		GroupProcessor g = new GroupProcessor(0, 1);
		{
			ReadLines r = new ReadLines(is);
			OpportunityUnpackFeeder f = new OpportunityUnpackFeeder();
			Connector.connect(r, f);
			g.associateOutput(0, f, 0);
		}
		return g;
	}

	@Override
	public Processor getFeeder(PrintStream ps, String... filenames) throws IOException
	{
		GroupProcessor g = new GroupProcessor(0, 1);
		{
			ReadLinesStatus r = new ReadLinesStatus(ps, filenames);
			OpportunityUnpackFeeder f = new OpportunityUnpackFeeder();
			Connector.connect(r, f);
			g.associateOutput(0, f, 0);
		}
		return g;
	}

	@Override
	public Function activityString()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Gets a processor instance that produces "raw" (integrated) events from the
	 * Opportunity input files.
	 * 
	 * @param ps
	 *          The print stream to which to write progress information
	 * @param filenames
	 *          The names of the input files
	 * @return A processor instance
	 * @throws IOException
	 *           If an error occurs while reading the files
	 */
	public Processor getRawFeeder(PrintStream ps, String... filenames) throws IOException
	{
		GroupProcessor g = new GroupProcessor(0, 1);
		{
			ReadLinesStatus r = new ReadLinesStatus(ps, filenames);
			OpportunityFeeder f = new OpportunityFeeder();
			Connector.connect(r, f);
			g.associateOutput(0, f, 0);
		}
		return g;
	}
	
	/**
	 * Gets a processor instance that produces "raw" (integrated) events from the
	 * Opportunity input files.
	 * 
	 * @param is
	 *          The input stream from which to read the data
	 * @return A processor instance
	 */
	public Processor getRawFeeder(InputStream is)
	{
		GroupProcessor g = new GroupProcessor(0, 1);
		{
			ReadLines r = new ReadLines(is);
			OpportunityFeeder f = new OpportunityFeeder();
			Connector.connect(r, f);
			g.associateOutput(0, f, 0);
		}
		return g;
	}

	/**
	 * Reads text lines from an Opportunity data file and unpacks them into
	 * individual events for each sensor. There are about 250 sensor readings in
	 * each line, thus each line is unpacked into that many events.
	 */
	public static class OpportunityUnpackFeeder extends SynchronousProcessor
	{
		public OpportunityUnpackFeeder()
		{
			super(1, 1);
		}

		@Override
		protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
		{
			int in_c = m_inputCount++;
			String[] parts = ((String) inputs[0]).split(" ");
			Long ts = Long.parseLong(parts[0]);
			// Todo: parse indices 243-249 for activity labels
			for (int i = 1; i < 243; i++)
			{
				if (parts[i].compareTo("NaN") == 0)
				{
					// Don't produce an event for a sensor outputting NaN
					continue;
				}
				String sensor_desc = COL_NAMES[i];
				TupleMap event = new TupleMap();
				event.put(P_STATE, Long.parseLong(parts[i]));
				event.put(P_SENSOR, getSensor(sensor_desc));
				event.put(P_MODEL, getModel(sensor_desc));
				event.put(P_SUBJECT, getSubject(sensor_desc));
				event.put(P_LOCATION, "");
				event.put(P_INDEX, in_c);
				event.put(P_TIMESTAMP, ts);
				outputs.add(new Object[] {event});
			}
			return true;
		}

		@Override
		public OpportunityUnpackFeeder duplicate(boolean with_state)
		{
			throw new UnsupportedOperationException("This feeder cannot be duplicated");
		}
	}

	/**
	 * Extracts what corresponds to the "model" part of a sensor description.
	 * @param sensor_desc The sensor description
	 * @return The model part
	 */
	protected static String getModel(String sensor_desc)
	{
		return sensor_desc.split(" ")[0];
	}

	/**
	 * Extracts what corresponds to the "subject" part of a sensor description.
	 * @param sensor_desc The sensor description
	 * @return The subject part
	 */
	protected static String getSubject(String sensor_desc)
	{
		return sensor_desc.split(" ")[1];
	}

	/**
	 * Extracts what corresponds to the "sensor" part of a sensor description.
	 * @param sensor_desc The sensor description
	 * @return The sensor part
	 */
	protected static String getSensor(String sensor_desc)
	{
		return sensor_desc.split(" ")[2];
	}

	/**
	 * Processor that takes a line of the Opportunity dataset and outputs a tuple
	 * containing the values of the columns.
	 */
	public static class OpportunityFeeder extends UniformProcessor
	{
		/**
		 * Creates a new instance of the feeder.
		 */
		public OpportunityFeeder()
		{
			super(1, 1);
		}

		@Override
		protected boolean compute(Object[] inputs, Object[] outputs)
		{
			String line = (String) inputs[0];
			String[] parts = line.split(" ");
			TupleMap tm = new TupleMap();
			for (int i = 0; i < parts.length; i++)
			{
				if (parts[i].compareTo("NaN") == 0)
				{
					tm.put(COL_NAMES[i], null);
				}
				else
				{
					tm.put(COL_NAMES[i], Long.parseLong(parts[i]));
				}
			}
			tm.put(P_INDEX, m_inputCount++);
			outputs[0] = tm;
			return true;
		}

		@Override
		public OpportunityFeeder duplicate(boolean with_state)
		{
			if (with_state)
			{
				throw new UnsupportedOperationException("Cannot duplicate with state");
			}
			return new OpportunityFeeder();
		}
	}
}
