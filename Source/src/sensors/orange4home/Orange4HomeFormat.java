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
package sensors.orange4home;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.RaiseArity;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.UnaryFunction;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.FixedTupleBuilder;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.tuples.Tuple;
import ca.uqac.lif.cep.tuples.TupleMap;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import sensors.IndexTupleFeeder;
import sensors.LabeledEventFormat;
import sensors.ReadLinesStatus;
import sensors.CurrentActivity.UpdateActivity;
import sensors.CurrentActivity.UpdateActivityFunction;

/**
 * The name of the sensor is a sequence of words separated with underscores.
 * The number of words is variable, such that mapping such a string to the
 * location/subject/model/sensor scheme requires some adaptation. The rules
 * being followed are:
 * <ul>
 * <li>Some pairs of words must be taken together, e.g. <tt>top_left</tt> or
 * <tt>tv_plug</tt>; the underscore between these words is removed through a
 * method called {@link #normalizeName(String)}</li>
 * <li>The first word is always <em>location</em></li>
 * <li>The last word is always <em>sensor</em></li>
 * <li>If a middle word exists, it is <em>subject</em></li>
 * </ul>
 * There is no such notion of "model", so this value is the empty string in
 * all events.
 */
public class Orange4HomeFormat implements LabeledEventFormat
{
	/**
	 * The date formatter used to parse the date string.
	 */
	/* @ non_null @ */ public static final DateFormat DATE_FORMAT;

	/**
	 * The name of attribute "subject" in a tuple.
	 */
	public static final String INDEX = IndexTupleFeeder.INDEX_ATTRIBUTE;

	/**
	 * The name of attribute "subject" in a tuple.
	 */
	public static final String DATE_TIME = "datetime";

	/**
	 * The name of attribute "subject" in a tuple.
	 */
	public static final String SUBJECT = "subject";

	/**
	 * The name of attribute "location" in a tuple.
	 */
	public static final String LOCATION = "location";

	/**
	 * The name of attribute "sensor" in a tuple.
	 */
	public static final String SENSOR = "sensor";

	/**
	 * The name of attribute "state" in a tuple.
	 */
	public static final String STATE = "state";

	/**
	 * The name of attribute "activity" in a tuple.
	 */
	public static final String ACTIVITY = "current";

	/**
	 * The builder creating objects identifying a sensor's uniquely defined
	 * location.
	 */
	protected static final FixedTupleBuilder s_placementPuilder = new FixedTupleBuilder(LOCATION, SUBJECT);

	/**
	 * The builder creating objects identifying a sensor's uniquely defined
	 * identifier.
	 */
	protected static final FixedTupleBuilder s_idPuilder = new FixedTupleBuilder(LOCATION, SUBJECT, SENSOR);

	static
	{
		DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
	}

	@Override
	public Date parseDate(String s)
	{
		// Parse the input date string
		Date date = null;
		try
		{
			date = DATE_FORMAT.parse(s);
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}
		return date;
	}

	@Override
	public Function timestamp()
	{
		return new FunctionTree(DateToTimestampOrange.instance, new FetchAttribute(DATE_TIME));
	}

	@Override
	public Function stateString()
	{
		return new FetchAttribute(STATE);
	}

	@Override
	public Function activityString()
	{
		return new FetchAttribute(ACTIVITY);
	}

	@Override
	public Function locationString()
	{
		return new FetchAttribute(LOCATION);
	}

	@Override
	public Function subjectString()
	{
		return new FetchAttribute(SUBJECT);
	}

	@Override
	public Function modelString()
	{
		return new RaiseArity(1, new Constant(""));
	}

	@Override
	public Function sensorString()
	{
		return new FetchAttribute(SENSOR);
	}

	@Override
	public Function sensorPlacement()
	{
		return new FunctionTree(new MergeScalars(LOCATION, SUBJECT),
				new FetchAttribute(LOCATION),
				new FetchAttribute(SUBJECT));
	}

	@Override
	public Function sensorId()
	{
		return new FunctionTree(new MergeScalars(LOCATION, SUBJECT, SENSOR),
				new FetchAttribute(LOCATION),
				new FetchAttribute(SUBJECT),
				new FetchAttribute(SENSOR));
	}

	@Override
	public Function index()
	{
		return new FunctionTree(Numbers.numberCast, new FetchAttribute(INDEX));
	}

	@Override
	public Tuple createPlacement(String location, String subject, String model)
	{
		return s_placementPuilder.createTuple(location, subject, model);
	}

	@Override
	public Tuple createId(String location, String subject, String model, String sensor)
	{
		return s_idPuilder.createTuple(location, subject, model, sensor);
	}

	@Override
	public String getOnConstant()
	{
		return "ON";
	}

	@Override
	public String getOffConstant()
	{
		return "OFF";
	}

	@Override
	public String getOpenConstant()
	{
		return "OPEN";
	}

	@Override
	public String getClosedConstant()
	{
		return "CLOSED";
	}

	@Override
	public String getExtension()
	{
		return ".csv";
	}

	@Override
	public GroupProcessor getFeeder(InputStream is)
	{
		GroupProcessor g = new GroupProcessor(0, 1);
		{
			ReadLines r = new ReadLines(is);
			OrangeTupleFeeder f = new OrangeTupleFeeder();
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
			OrangeTupleFeeder f = new OrangeTupleFeeder();
			Connector.connect(r, f);
			g.associateOutput(0, f, 0);
		}
		return g;
	}

	protected static String normalizeName(String s)
	{
		s = s.replaceAll("presence_table", "table_presence");
		s = s.replaceAll("presence_couch", "couch_presence");
		s = s.replaceAll("_plug", "plug");
		s = s.replaceAll("_setpoint", "setpoint");
		s = s.replaceAll("_total$", "total");
		s = s.replaceAll("_mode", "mode");
		s = s.replaceAll("_ext", "ext");
		s = s.replaceAll("_instantaneous", "instantaneous");
		s = s.replaceAll("total_energy", "totalenergy");
		s = s.replaceAll("partial_energy", "partialenergy");
		s = s.replaceAll("top_left", "topleft");
		s = s.replaceAll("top_right", "topright");
		s = s.replaceAll("middle_left", "middleleft");
		s = s.replaceAll("middle_right", "middleright");
		s = s.replaceAll("bottom_left", "bottomleft");
		s = s.replaceAll("bottom_right", "bottomright");
		return s;
	}

	public static class OrangeTupleFeeder extends SynchronousProcessor
	{
		protected String m_currentActivity;

		protected long m_activityCount;

		protected OrangeTupleFeeder()
		{
			super(1, 1);
			m_currentActivity = "";
			m_activityCount = 0;
		}

		@Override
		protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
		{
			if (m_inputCount == 0)
			{
				// Ignore first line
				m_inputCount++;
				return true;
			}
			String line = (String) inputs[0];
			String[] parts = line.split(",");
			if (line.contains("label,"))
			{
				// Do not emit this line as an event, but rather update the label of
				// the currently ongoing activity
				String act_label = parts[2];
				if (act_label.startsWith("STOP:"))
				{
					m_currentActivity = "";
					m_activityCount++;
				}
				else
				{
					String[] act_parts = act_label.split(":");
					m_currentActivity = act_parts[1];
				}
				m_inputCount++;
				return true;
			}
			TupleMap t = new TupleMap();
			t.put(DATE_TIME, parts[0]);
			t.put(STATE, parts[2]);
			String inside = normalizeName(parts[1]);
			String[] inside_parts = inside.split("_");
			t.put(LOCATION, inside_parts[0]);
			if (inside_parts.length == 2)
			{
				t.put(SUBJECT, "");
				t.put(SENSOR, inside_parts[1]);
			}
			else if (inside_parts.length == 3)
			{
				t.put(SUBJECT, inside_parts[1]);
				t.put(SENSOR, inside_parts[2]);
			}
			else
			{
				System.out.println("Weird format");
			}
			t.put(ACTIVITY, m_currentActivity + m_activityCount);
			t.put(INDEX, m_inputCount++);
			outputs.add(new Object[] {t});
			return true;
		}

		@Override
		public Processor duplicate(boolean with_state)
		{
			throw new UnsupportedOperationException("Duplication of this processor is not supported");
		}
	}

	public class GetUpdateActivity extends UnaryFunction<Tuple,UpdateActivityFunction>
	{
		public GetUpdateActivity()
		{
			super(Tuple.class, UpdateActivityFunction.class);
		}

		@Override
		public UpdateActivityFunction getValue(Tuple t)
		{
			String act = (String) t.get(ACTIVITY);
			if (act == null || act.isBlank())
			{
				return new UpdateActivity("");
			}
			return new UpdateActivity(act);
		}
	}
	
	/**
	 * In this dataset, a temperature event is spotted by the "sensor"
	 * attribute of the tuple having the value "temperature".
	 */
	@Override
	public Function isTemperature()
	{
		return new FunctionTree(Equals.instance, new FunctionTree(new FetchAttribute(SENSOR), StreamVariable.X), new Constant("temperature"));
	}
}
