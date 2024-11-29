package sensors.kasteren;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;

import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.RaiseArity;
import ca.uqac.lif.cep.tmf.QueueSource;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.Tuple;
import ca.uqac.lif.cep.tuples.TupleMap;
import sensors.LabeledEventFormat;
import sensors.TimedEvent;

public class KasterenEventFormat implements LabeledEventFormat 
{
	/**
	 * The date formatter used to parse the date string.
	 */
	/*@ non_null @*/ public static final DateFormat DATE_FORMAT;

	public static final String P_ACTIVITY = "Activity";

	public static final String P_TIMESTAMP = "Time";

	public static final String P_SENSOR = "Sensor";

	public static final String P_STATE = "State";

	public static final String P_INDEX = "Index";

	public static final String P_ON = "On";

	public static final String P_OFF = "Off";

	protected static final TimeZone s_utc = TimeZone.getTimeZone("UTC");

	static
	{
		// Sets the date format to print in Zulu time
		DATE_FORMAT = new SimpleDateFormat("dd-LLL-yyyy HH:mm:ss");
		DATE_FORMAT.setTimeZone(s_utc);
	}

	@Override
	public Date parseDate(String s)
	{
		return readDate(s);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Function subjectString()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Function modelString()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Function sensorString()
	{
		return new FetchAttribute(P_SENSOR);
	}

	@Override
	public Function sensorPlacement()
	{
		// TODO Auto-generated method stub
		return null;
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
		return P_ON;
	}

	@Override
	public Object getOffConstant()
	{
		return P_OFF;
	}

	@Override
	public Object getOpenConstant()
	{
		return P_ON;
	}

	@Override
	public Object getClosedConstant()
	{
		return P_OFF;
	}

	@Override
	public String getExtension()
	{
		return "txt";
	}

	@Override
	public KasterenFeeder getFeeder(InputStream is)
	{
		return new KasterenFeeder();
	}

	@Override
	public KasterenFeeder getFeeder(PrintStream out, String... filenames) throws IOException
	{
		return new KasterenFeeder();
	}

	@Override
	public Function activityString()
	{
		return new FetchAttribute(P_ACTIVITY);
	}

	protected static Date readDate(String s)
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

	protected static long readDateToMillis(String s)
	{
		try
		{
			return DATE_FORMAT.parse(s).getTime();
		}
		catch (ParseException e)
		{
			return 0;
		}
	}

	/**
	 * This feeder operates in a different way from other datasets.
	 */
	public static class KasterenFeeder extends QueueSource
	{
		protected static final List<Tuple> s_events = new ArrayList<Tuple>();

		static
		{
			populateEvents(s_events);
		}

		public KasterenFeeder()
		{
			super(s_events);
			loop(false);
		}

		@Override
		public KasterenFeeder duplicate(boolean with_state)
		{
			throw new UnsupportedOperationException("This source cannot be duplicated");
		}

		protected static void populateEvents(List<Tuple> events)
		{
			boolean ready = false;
			List<TimedEvent> activities = getActivities(); // TODO: activities
			List<TimedEvent> t_events = new ArrayList<TimedEvent>();
			Scanner scanner = null;
			try
			{
				scanner = new Scanner(new File("data/kasteren/kasterenSenseData.txt"));
			}
			catch (FileNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			while (scanner.hasNextLine())
			{
				String line = scanner.nextLine();
				if (!ready)
				{
					if (line.startsWith("-----"))
					{
						ready = true;
					}
					continue;
				}
				if (line.startsWith("Length"))
				{
					continue;
				}
				String[] parts = line.split("\\t");
				long start_t = readDateToMillis(parts[0]);
				long end_t = readDateToMillis(parts[1]);
				int id = Integer.parseInt(parts[2]);
				{
					TupleMap e = new TupleMap();
					e.put(P_TIMESTAMP, start_t);
					e.put(P_STATE, P_ON);
					e.put(P_SENSOR, getSensorById(id));
					t_events.add(new TimedEvent(start_t, e));
				}
				{
					TupleMap e = new TupleMap();
					e.put(P_TIMESTAMP, end_t);
					e.put(P_STATE, P_OFF);
					e.put(P_SENSOR, getSensorById(id));
					t_events.add(new TimedEvent(end_t, e));
				}
			}
			Collections.sort(t_events); // Sort by timestamp
			for (int i = 0; i < t_events.size(); i++)
			{
				TimedEvent te = t_events.get(i);
				Tuple t = (Tuple) te.getEvent();
				t.put(P_INDEX, i);
				events.add(t);
			}
			scanner.close();
		}

		protected static List<TimedEvent> getActivities()
		{
			return null;
		}
	}

	protected static String getSensorById(int id)
	{
		switch (id)
		{
		case  1:    return "Microwave";         
		case  5:    return "Hall-Toilet door";  
		case  6:    return "Hall-Bathroom door";
		case  7:    return "Cups cupboard";     
		case  8:    return "Fridge";            
		case  9:    return "Plates cupboard";   
		case 12:    return "Frontdoor";         
		case 13:    return "Dishwasher";        
		case 14:    return "ToiletFlush";       
		case 17:    return "Freezer";           
		case 18:    return "Pans Cupboard";     
		case 20:    return "Washingmachine";    
		case 23:    return "Groceries Cupboard";
		case 24:    return "Hall-Bedroom door"; 
		}
		return null;
	}


}
