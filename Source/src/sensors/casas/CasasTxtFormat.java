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
package sensors.casas;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.util.Strings;
import sensors.LabeledEventFormat;
import sensors.IndexTupleFeeder;

public abstract class CasasTxtFormat implements LabeledEventFormat
{
	/**
	 * The date formatter used to parse the date string.
	 */
	/* @ non_null @ */ public static final DateFormat DATE_FORMAT;

	/* @ non_null @ */ public static final String TXT_SENSOR = "sensor"; // sensor name

	/* @ non_null @ */ public static final String TXT_STATE = "state"; // the message sent by the
																																			// sensor

	/* @ non_null @ */ public static final String TXT_LOCATION = "location"; // Is the room level
																																						// location of the
																																						// sensor

	/* @ non_null @ */ public static final String TXT_SUBJECT = "subject"; // represents the more
																																					// detailed location in
																																					// the CASAS dataset
	
	/* @ non_null @ */ public static final String TXT_INDEX = IndexTupleFeeder.INDEX_ATTRIBUTE;
	
	/* @ non_null @ */ public static final String TXT_MODEL = "model";
	
	/* @ non_null @ */ public static final String TXT_ACTIVITY = "activity"; // a field that is present only in the CASAS dataset 

	/* @ non_null @ */ public static final String TXT_CONTACT = "contact"; // We do not have a contact
																																					// sensors in CASAS
																																					// dataset
	
	/* @ non_null @ */ public static final String V_TEMPERATURE = "temperature";

	/* @ non_null @ */ protected static final String V_ON = "ON";

	/* @ non_null @ */ protected static final String V_OFF = "OFF";

	/* @ non_null @ */ protected static final String V_OPEN = "OPEN";

	/* @ non_null @ */ protected static final String V_CLOSED = "CLOSE";

	protected static final TimeZone s_utc = TimeZone.getTimeZone("UTC");
	
	

	static
	{
		// Sets the date format to print in Zulu time
		DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
		DATE_FORMAT.setTimeZone(s_utc);
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
	
	/**
	 * In the CASAS dataset, several message states are possible depending on the
	 * type of the sensor: Control4-Motion and Control4-MotionArea: report ON or OFF
	 * Control4-LightSensor: integer values ranging from 0 to 100
	 * Control4-BatteryPercent: integer ranging from 0 to 100 Control4-Radio: report
	 * the message OK Control4-Door: report OPEN or CLOSE Control4-Temperature: a
	 * decimal in Celsius with 0.5 degrees Celsius accuracy Control4-Light: integer
	 * ranging from 0 to 100 Control4-Button: report the messages TAP or
	 * TAP_COUNT_01 or DEPRESS or RELEASE
	 */
	@Override
	public Function stateString()
	{
		return new FetchAttribute(TXT_STATE);
	}

	@Override
	public Function locationString()
	{
		return new FetchAttribute(TXT_LOCATION);
	}

	@Override
	public Function subjectString()
	{
		return new FetchAttribute(TXT_SUBJECT);
	}

	@Override
	public Function sensorString()
	{
		return new FetchAttribute(TXT_SENSOR);
	}
	
	@Override
	public Function index()
	{
		return new FunctionTree(Numbers.numberCast, new FetchAttribute(TXT_INDEX));
	}
	
	@Override
	public Function activityString()
	{
		return new FetchAttribute(TXT_ACTIVITY);
	}

	/**
	 * In the CASAS platform, there is no dedicated "model" field in events.
	 * Rather, the model can be extracted by the sequence of letters in the
	 * "sensor" field. For example, in "LS008", the LS indicates that this is a
	 * light sensor.
	 */
	@Override
	public Function modelString()
	{
		return new FunctionTree(new Strings.FindRegexOnce("([A-Z]+)\\d+"), new FetchAttribute(TXT_SENSOR));
	}

	@Override
	public Object getOnConstant()
	{
		return V_ON;
	}

	@Override
	public Object getOffConstant()
	{
		return V_OFF;
	}

	@Override
	public Object getOpenConstant()
	{
		return V_OPEN;
	}

	@Override
	public Object getClosedConstant()
	{
		return V_CLOSED;
	}

	@Override
	public String getExtension()
	{
		return ".txt";
	}
}
