package sensors.casas;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.FixedTupleBuilder;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.tuples.Tuple;
import ca.uqac.lif.cep.util.Strings;
import sensors.DateToTimestampCasas;
import sensors.EventFormat;

public class CasasTxtFormat implements EventFormat
{
	/**
	 * The date formatter used to parse the date string.
	 */
	/* @ non_null @ */ public static final DateFormat DATE_FORMAT;

	/* @ non_null @ */ public static final String TXT_MODEL = "model";// the physical model

	/* @ non_null @ */ public static final String TXT_SENSOR = "sensor"; // sensor name

	/* @ non_null @ */ public static final String TXT_STATE = "state"; // the message sent by the
																																			// sensor

	/* @ non_null @ */ public static final String TXT_LOCATION = "location"; // Is the room level
																																						// location of the
																																						// sensor

	/* @ non_null @ */ public static final String TXT_SUBJECT = "subject"; // represents the more
																																					// detailed location in
																																					// the CASAS dataset

	/* @ non_null @ */ public static final String TXT_TIME = "time";

	/* @ non_null @ */ public static final String TXT_DATE = "date";

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
		DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
		DATE_FORMAT.setTimeZone(s_utc);

	}

	/**
	 * The builder creating objects identifying a sensor's uniquely defined
	 * location.
	 */
	protected static final FixedTupleBuilder s_placementBuilder = new FixedTupleBuilder("location",
			"subject", "model");
	protected static final FixedTupleBuilder s_idBuilder = new FixedTupleBuilder("location",
			"subject", "model", "sensor");

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
	 * In the CASAS dataset, the first two fields in an event corresponds to the
	 * date in the form "yyyy-MM-dd' and time in the form 'HH:mm:ss.SSSSSX"
	 * respectively. The two strings are extracted and then converted into a Unix
	 * timestamp (i.e. a {@code long}).
	 */
	@Override
	public Function timestamp()
	{
		return new FunctionTree(DateToTimestampCasas.instance,
				new FunctionTree(Strings.concat,
						new FunctionTree(Strings.concat, new FetchAttribute(TXT_DATE), new Constant("T")),
						new FetchAttribute(TXT_TIME)));
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
	public Function modelString()
	{
		return new FetchAttribute(TXT_MODEL);
	}

	/**
	 * In the CASAS platform, the placement of a sensor can be uniquely determined
	 * by the combination of three attributes in an event: {@code location},
	 * {@code subject} and {@code model}.
	 */

	@Override
	public Function sensorPlacement()
	{
		return new FunctionTree(new MergeScalars("location", "subject", "model"),
				new FetchAttribute(TXT_LOCATION), new FetchAttribute(TXT_SUBJECT),
				new FetchAttribute(TXT_MODEL));
	}

	/**
	 * In the CASAS dataset, the placement of a sensor is uniquely determined by the
	 * combination of three attributes in an event: {@code location},
	 * {@code subject} and {@code model}.
	 */
	@Override
	public Tuple createPlacement(String location, String subject, String model)
	{
		return s_placementBuilder.createTuple(location, subject, model);
	}

	/**
	 * In the CASAS dataset, the placement of a sensor is uniquely determined by the
	 * combination of three attributes in an event: {@code location},
	 * {@code subject} and {@code model}.
	 */
	@Override
	public Function sensorId()
	{
		return new FunctionTree(new MergeScalars("location", "subject", "model", "sensor"),
				new FetchAttribute(TXT_LOCATION), new FetchAttribute(TXT_SUBJECT),
				new FetchAttribute(TXT_MODEL), new FetchAttribute(TXT_SENSOR));
	}

	/**
	 * In the CASAS dataset, the placement of a sensor is uniquely determined by the
	 * combination of three attributes in an event: {@code location},
	 * {@code subject} and {@code model}.
	 */

	@Override
	public Tuple createId(String location, String subject, String model, String sensor)
	{
		return s_idBuilder.createTuple(location, subject, model, sensor);
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

	@Override
	public ReadLines getFeeder(InputStream is)
	{
		// TODO Auto-generated method stub
		return new ReadLines(is);
	}

}
