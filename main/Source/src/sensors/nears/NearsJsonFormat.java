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
package sensors.nears;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.json.JPathFunction;
import ca.uqac.lif.cep.json.StringValue;
import ca.uqac.lif.cep.tuples.FixedTupleBuilder;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.tuples.Tuple;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.json.JsonElement;
import ca.uqac.lif.json.JsonMap;
import ca.uqac.lif.json.JsonParser;
import ca.uqac.lif.json.JsonParser.JsonParseException;
import sensors.EventFormat;
import sensors.nears.examples.DateToTimestampNears;

/**
 * Extracts features from events in the format of the NEARS smart home
 * platform.
 */
public class NearsJsonFormat implements EventFormat
{
	/**
	 * The date formatter used to parse the date string.
	 */
	/*@ non_null @*/ public static final DateFormat DATE_FORMAT;

	/*@ non_null @*/ public static final String JP_LOCATION = "location";

	/*@ non_null @*/ public static final String JP_MODEL = "model";

	/*@ non_null @*/ public static final String JP_SENSOR = "sensor";

	/*@ non_null @*/ public static final String JP_STATE = "state";

	/*@ non_null @*/ public static final String JP_SUBJECT = "subject";

	/*@ non_null @*/ public static final String JP_TIMESTAMP = "sentAt/$date";

	/*@ non_null @*/ public static final String V_CONTACT = "contact";

	/*@ non_null @*/ public static final String V_TEMPERATURE = "temperature";

	/*@ non_null @*/ protected static final String V_ON = "ON";
	
	/*@ non_null @*/ protected static final String V_OFF = "OFF";
	
	/*@ non_null @*/ protected static final String V_OPEN = "OPEN";
	
	/*@ non_null @*/ protected static final String V_CLOSED = "CLOSED";

	protected static final JsonParser s_parser = new JsonParser();

	protected static final TimeZone s_utc = TimeZone.getTimeZone("UTC");

	static
	{
		// Sets the date format to print in Zulu time
		DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
		DATE_FORMAT.setTimeZone(s_utc);
	}

	/**
	 * The builder creating objects identifying a sensor's uniquely defined
	 * location.
	 */
	protected static final FixedTupleBuilder s_placementPuilder = new FixedTupleBuilder("location", "subject", "model");

	@Override
	public Date parseDate(String s)
	{
		try
		{
			return DATE_FORMAT.parse(s);
		}
		catch (ParseException e)
		{
			return null;
		}
	}

	/**
	 * In the NEARS platform, the timestamp is a string at the end of path
	 * {@code sentAt/$date} in the JSON element. This string is extracted and
	 * then converted into a Unix timestamp (i.e. a {@code long}).
	 */
	@Override
	public Function timestamp()
	{
		return new FunctionTree(DateToTimestampNears.instance,
				new FunctionTree(StringValue.instance,
						new JPathFunction(JP_TIMESTAMP)));
	}

	@Override
	public Function stateString()
	{
		return new FunctionTree(StringValue.instance,
				new JPathFunction(JP_STATE));
	}

	@Override
	public Function locationString()
	{
		return new FunctionTree(StringValue.instance,
				new JPathFunction(JP_LOCATION));
	}

	@Override
	public Function subjectString()
	{
		return new FunctionTree(StringValue.instance,
				new JPathFunction(JP_SUBJECT));
	}

	@Override
	public Function sensorString()
	{
		return new FunctionTree(StringValue.instance,
				new JPathFunction(JP_SENSOR));
	}

	@Override
	public Function modelString()
	{
		return new FunctionTree(StringValue.instance,
				new JPathFunction(JP_MODEL));
	}
	
	/**
	 * In the NEARS platform, the placement of a sensor is uniquely determined
	 * by the combination of three attributes in an event: {@code location},
	 * {@code subject} and {@code model}.
	 */
	@Override
	public Function sensorPlacement()
	{
		return new FunctionTree(new MergeScalars("location", "subject", "model"),
				new FunctionTree(StringValue.instance, new JPathFunction(JP_LOCATION)),
				new FunctionTree(StringValue.instance, new JPathFunction(JP_SUBJECT)),
				new FunctionTree(StringValue.instance, new JPathFunction(JP_MODEL)));
	}

	/**
	 * In the NEARS platform, the placement of a sensor is uniquely determined
	 * by the combination of three attributes in an event: {@code location},
	 * {@code subject} and {@code model}.
	 */
	@Override
	public Tuple createPlacement(String location, String subject, String model)
	{
		return s_placementPuilder.createTuple(location, subject, model);
	}
	
	/**
	 * In the NEARS platform, the placement of a sensor is uniquely determined
	 * by the combination of three attributes in an event: {@code location},
	 * {@code subject} and {@code model}.
	 */
	@Override
	public Function sensorId()
	{
		return new FunctionTree(new MergeScalars("location", "subject", "model", "sensor"),
				new FunctionTree(StringValue.instance, new JPathFunction(JP_LOCATION)),
				new FunctionTree(StringValue.instance, new JPathFunction(JP_SUBJECT)),
				new FunctionTree(StringValue.instance, new JPathFunction(JP_MODEL)),
				new FunctionTree(StringValue.instance, new JPathFunction(JP_SENSOR)));
	}
	
	@Override
	public Function index()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * In the NEARS platform, the placement of a sensor is uniquely determined
	 * by the combination of three attributes in an event: {@code location},
	 * {@code subject} and {@code model}.
	 */
	@Override
	public Tuple createId(String location, String subject, String model, String sensor)
	{
		return s_placementPuilder.createTuple(location, subject, model, sensor);
	}
	
	@Override
	public String getOnConstant()
	{
		return V_ON;
	}
	
	@Override
	public String getOffConstant()
	{
		return V_OFF;
	}
	
	@Override
	public String getOpenConstant()
	{
		return V_OPEN;
	}
	
	@Override
	public String getClosedConstant()
	{
		return V_CLOSED;
	}
	
	@Override
	public String getExtension()
	{
		return ".json";
	}
	
	@Override
	public JsonLineFeeder getFeeder(InputStream is)
	{
		return new JsonLineFeeder(is);
	}
	
	@Override
	public JsonLineFeeder getFeeder(PrintStream out, String ... filenames) throws IOException
	{
		return new JsonLineFeeder(new FileInputStream(new File(filenames[0])));
	}

	/**
	 * Creates a new sensor event in the format of the platform, by providing
	 * values to each mandatory field. The method takes care of reproducing the
	 * "quirks" of the format, such as encoding temperature values as strings
	 * ending in <tt>" °C"</tt>.
	 * @param location The value of the <tt>location</tt> field
	 * @param subject The value of the <tt>subject</tt> field
	 * @param model The value of the <tt>model</tt> field
	 * @param timestamp A timestamp, expressed as a string in the expected format
	 * (see {@link #DATE_FORMAT})
	 * @param sensor The value of the <tt>sensor</tt> field
	 * @param state The value of the <tt>state</tt> field
	 * @return The sensor event
	 */
	public static JsonElement newEvent(String location, String subject, String model, String timestamp, String sensor, Object state)
	{
		JsonMap map = new JsonMap();
		map.put(JP_LOCATION, location);
		map.put(JP_SUBJECT, subject);
		map.put(JP_MODEL, model);
		// Look out, this part is hard-coded as it is a nested field
		{
			JsonMap sentat = new JsonMap();
			sentat.put("$date", timestamp);
			map.put("sentAt", sentat);
		}
		map.put(JP_SENSOR, sensor);
		if (state instanceof String)
		{
			map.put(JP_STATE, state);
		}
		else if (state instanceof Number)
		{
			if (sensor.compareTo(V_TEMPERATURE) == 0)
			{
				map.put(JP_STATE, String.format("%.2f °C", ((Number) state).floatValue()));
			}
			else
			{
				map.put(JP_STATE, ((Number) state).floatValue());
			}
		}
		return map;
	}

	/**
	 * Creates a new sensor event in the format of the platform, by providing
	 * values to each mandatory field. The method takes care of reproducing the
	 * "quirks" of the format, such as encoding temperature values as strings
	 * ending in <tt>" °C"</tt>.
	 * @param location The value of the <tt>location</tt> field
	 * @param subject The value of the <tt>subject</tt> field
	 * @param model The value of the <tt>model</tt> field
	 * @param timestamp A Unix timestamp (in milliseconds)
	 * @param sensor The value of the <tt>sensor</tt> field
	 * @param state The value of the <tt>state</tt> field
	 * @return The sensor event
	 */
	public static JsonElement newEvent(String location, String subject, String model, long timestamp, String sensor, Object state)
	{
		return newEvent(location, subject, model, DATE_FORMAT.format(new Date(timestamp)), sensor, state);
	}

	/**
	 * Creates a new sensor event in the format of the platform, by providing
	 * values to each mandatory field. The method takes care of reproducing the
	 * "quirks" of the format, such as encoding temperature values as strings
	 * ending in <tt>" °C"</tt>.
	 * @param location The value of the <tt>location</tt> field
	 * @param subject The value of the <tt>subject</tt> field
	 * @param model The value of the <tt>model</tt> field
	 * @param timestamp A timestamp expressed as a Java {@link Date} object
	 * @param sensor The value of the <tt>sensor</tt> field
	 * @param state The value of the <tt>state</tt> field
	 * @return The sensor event
	 */
	public static JsonElement newEvent(String location, String subject, String model, Date timestamp, String sensor, Object state)
	{
		return newEvent(location, subject, model, DATE_FORMAT.format(timestamp), sensor, state);
	}

	/**
	 * Parses a new sensor event from a JSON string.
	 * @param content The string containing the event
	 * @return The parsed JSON event, or <tt>null</tt> if the parsing could not
	 * succeed
	 */
	/*@ null @*/ public static JsonElement newEvent(/*@ null @*/ String content)
	{
		if (content == null)
		{
			return null;
		}
		try
		{
			return s_parser.parse(content);
		}
		catch (JsonParseException e)
		{
			return null;
		}
	}
	
	/**
	 * In this dataset, a temperature event is spotted by the "sensor"
	 * attribute of the tuple having the value "temperature".
	 */
	@Override
	public Function isTemperature()
	{
		return new FunctionTree(Equals.instance, new FunctionTree(StringValue.instance, new JPathFunction(JP_SENSOR)), new Constant("temperature"));
	}
}
