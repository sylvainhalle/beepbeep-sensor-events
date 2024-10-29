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
package sensors.aras;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Queue;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.Tuple;
import ca.uqac.lif.cep.tuples.TupleMap;
import ca.uqac.lif.cep.util.Equals;
import sensors.ReadLinesStatus;

/**
 * Format for the Aras dataset, house A.
 */
public class ArasFormatHouseA extends ArasFormat
{
	/**
	 * The sensor ID corresponding to each of the first 20 columns of the
	 * dataset.
	 */
	public static final String[] S_NAMES = new String[] {
			"Ph1", "Ph2", "Ir1", "Fo1", "Fo2", "Di3", "Di4 ", "Ph3", "Ph4", "Ph5", "Ph6", "Co1", "Co2", "Co3", "So1", "So2", "Di1", "Di2", "Te1", "Fo3"
	};

	/**
	 * The sensor type corresponding to each of the first 20 columns of the
	 * dataset.
	 */
	public static final String[] S_SENSORS = new String[] {
			"Photocell", "Photocell", "IR", "Force Sensor", "Force Sensor", "Distance", "Distance", "Photocell", "Photocell", "Photocell", "Photocell", "Contact Sensor", "Contact Sensor", "Contact Sensor", "Sonar Distance", "Sonar Distance", "Distance", "Distance", "Temperature", "Force Sensor"
	};

	/**
	 * The subject corresponding to each of the first 20 columns of the
	 * dataset.
	 */
	public static final String[] S_SUBJECT = new String[] {
			"Wardrobe", "Convertible Couch", "TV receiver", "Couch", "Couch", "Chair", "Chair", "Fridge", "Kitchen Drawer", "Wardrobe", "Bathroom Cabinet", "House Door", "Bathroom Door", "Shower Cabinet Door", "Hall", "Kitchen", "Tap", "Water Closet", "Kitchen", "Bed"
	};

	/**
	 * The list of activities contained in the dataset. The string at index i is
	 * the activity corresponding to the integer i in the log.
	 */
	public static final String[] S_ACTIVITIES = new String[] {
			"Other", "Going Out", "Preparing Breakfast", "Having Breakfast", "Preparing Lunch", "Having Lunch", "Preparing Dinner", "Having Dinner", "Washing Dishes", "Having Snack", "Sleeping", "Watching TV", "Studying", "Having Shower", "Toileting", "Napping", "Using Internet", "Reading Book", "Laundry", "Shaving", "Brushing Teeth", "Talking on the Phone", "Listening to Music", "Cleaning", "Having Conversation", "Having Guest", "Changing Clothes"
	};

	/**
	 * The time at which the dataset is arbitrarily set to start. The Aras
	 * dataset does not specify any date, but only seconds elapsed from an
	 * unknown time zero. We choose to start at the beginning of the year 2024.
	 */
	public static final long START_TIME = LocalDateTime.of(2024, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli();

	@Override
	public Date parseDate(String s)
	{
		// There are no dates to parse in the Aras dataset
		throw new UnsupportedOperationException("There are no dates to parse in the Aras dataset");
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
		return new FetchAttribute(P_LOCATION);
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
		return new FetchAttribute(P_SUBJECT);
	}

	@Override
	public Function sensorId()
	{
		return new FetchAttribute(P_SENSOR);
	}

	@Override
	public Function index()
	{
		return new FetchAttribute(P_INDEX);
	}

	@Override
	public Function isTemperature()
	{
		return new FunctionTree(Equals.instance,
				modelString(), new Constant("Temperature"));
	}

	@Override
	public Tuple createPlacement(String location, String subject, String model)
	{
		TupleMap t = new TupleMap();
		t.put(P_LOCATION, "");
		t.put(P_SUBJECT, subject);
		t.put(P_MODEL, model);
		return t;
	}

	@Override
	public Tuple createId(String location, String subject, String model, String sensor)
	{
		TupleMap t = new TupleMap();
		t.put(P_SENSOR, sensor);
		return t;
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
		return "txt";
	}

	@Override
	public GroupProcessor getFeeder(InputStream is)
	{
		GroupProcessor g = new GroupProcessor(0, 1);
		{
			ReadLines r = new ReadLines(is);
			ArasUnpackFeeder f = new ArasUnpackFeeder();
			Connector.connect(r, f);
			g.associateOutput(0, f, 0);
		}
		return g;
	}

	@Override
	public GroupProcessor getFeeder(PrintStream os, String ... filenames) throws IOException
	{
		GroupProcessor g = new GroupProcessor(0, 1);
		{
			ReadLinesStatus r = new ReadLinesStatus(os, filenames);
			ArasUnpackFeeder f = new ArasUnpackFeeder();
			Connector.connect(r, f);
			g.associateOutput(0, f, 0);
		}
		return g;
	}

	@Override
	public GroupProcessor getRawFeeder(PrintStream os, String ... filenames) throws IOException
	{
		GroupProcessor g = new GroupProcessor(0, 1);
		{
			ReadLinesStatus r = new ReadLinesStatus(os, filenames);
			ArasFeeder f = new ArasFeeder();
			Connector.connect(r, f);
			g.associateOutput(0, f, 0);
		}
		return g;
	}

	@Override
	public Function activityString(int index)
	{
		return new FetchAttribute(index == 0 ? P_ACTIVITY1 : P_ACTIVITY2);
	}

	/**
	 * Reads text lines from an ARAS data file and produces a single "compound"
	 * event containing all the sensor readings in that line.
	 */
	public static class ArasFeeder extends SynchronousProcessor
	{
		public ArasFeeder()
		{
			super(1, 1);
		}

		@Override
		protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
		{
			int in_c = m_inputCount++;
			String[] parts = ((String) inputs[0]).split(" ");
			TupleMap event = new TupleMap();
			for (int i = 0; i < 20; i++)
			{
				event.put(S_NAMES[i], parts[i].compareTo("0") == 0 ? S_OFF : S_ON);
			}
			event.put(P_TIMESTAMP, START_TIME + (long) in_c * 1000);
			event.put(P_ACTIVITY1, S_ACTIVITIES[Integer.parseInt(parts[20].trim()) - 1]);
			event.put(P_ACTIVITY2, S_ACTIVITIES[Integer.parseInt(parts[21].trim()) - 1]);
			outputs.add(new Object[] {event});
			return true;
		}

		@Override
		public ArasFeeder duplicate(boolean with_state)
		{
			throw new UnsupportedOperationException("This feeder cannot be duplicated");
		}
	}

	/**
	 * Reads text lines from an ARAS data file and unpacks them into individual
	 * events for each sensor. In House A, there are 20 sensor readings in each
	 * line, thus each line is unpacked into 20 events.
	 */
	public static class ArasUnpackFeeder extends SynchronousProcessor
	{
		public ArasUnpackFeeder()
		{
			super(1, 1);
		}

		@Override
		protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
		{
			int in_c = m_inputCount++;
			String[] parts = ((String) inputs[0]).split(" ");
			String ac1 = S_ACTIVITIES[Integer.parseInt(parts[20].trim()) - 1];
			String ac2 = S_ACTIVITIES[Integer.parseInt(parts[21].trim()) - 1];
			for (int i = 0; i < 20; i++)
			{
				TupleMap event = new TupleMap();
				event.put(P_STATE, parts[i].compareTo("0") == 0 ? S_OFF : S_ON);
				event.put(P_SENSOR, S_NAMES[i]);
				event.put(P_MODEL, S_SENSORS[i]);
				event.put(P_SUBJECT, S_SUBJECT[i]);
				event.put(P_LOCATION, "");
				event.put(P_INDEX, in_c);
				event.put(P_ACTIVITY1, ac1);
				event.put(P_ACTIVITY2, ac2);
				event.put(P_TIMESTAMP, START_TIME + (long) in_c * 1000);
				outputs.add(new Object[] {event});
			}
			return true;
		}

		@Override
		public ArasUnpackFeeder duplicate(boolean with_state)
		{
			throw new UnsupportedOperationException("This feeder cannot be duplicated");
		}
	}
}
