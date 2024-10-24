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
package sensors.house;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import ca.uqac.lif.cep.Duplicable;
import ca.uqac.lif.cep.functions.UnaryFunction;
import sensors.EventFormat;
import sensors.PrettyPrintStream;
import sensors.PrettyPrintStream.PrettyPrintable;

import static sensors.EventFormat.evaluateUnary;

/**
 * Nested data structure representing the instantaneous state of a "house".
 * The nesting is structured as:
 * <blockquote>
 * location &mapsto; subject &mapsto; device &mapsto; sensor &mapsto; state
 * </blockquote>
 * That is, a house contains multiple locations identified by a name; each
 * location is made of multiple subjects, each identified by a name, and so on.
 * The state of a sensor is a tuple containing its latest value, along with
 * the timestamp at which this value was observed.
 * <p>
 * A {@link House} object comes with BeepBeep functions that allow it to be
 * updated upon incoming JSON events from the sensor infrastructure, see
 * {@link HouseDelta}.
 * 
 * @author Sylvain Hallé
 */
@SuppressWarnings("serial")
public class House extends PrettyTreeMap<sensors.house.House.Location> implements Duplicable
{
	/**
	 * A location is a map from subject names to subjects.
	 */
	public static class Location extends PrettyTreeMap<Subject> implements Duplicable
	{
		@Override
		public Location duplicate()
		{
			return duplicate(false);
		}
		
		@Override
		public Location duplicate(boolean with_state)
		{
			Location l = new Location();
			for (Map.Entry<String,Subject> e : entrySet())
			{
				l.put(e.getKey(), e.getValue().duplicate());
			}
			return l;
		}
	}

	/**
	 * A subject is a map from device names to devices.
	 */
	public static class Subject extends PrettyTreeMap<Device> implements Duplicable
	{
		@Override
		public Subject duplicate()
		{
			return duplicate(false);
		}
		
		@Override
		public Subject duplicate(boolean with_state)
		{
			Subject s = new Subject();
			for (Map.Entry<String,Device> e : entrySet())
			{
				s.put(e.getKey(), e.getValue().duplicate());
			}
			return s;
		}
	}

	/**
	 * A device is a map from sensor names to sensors.
	 */
	public static class Device extends PrettyTreeMap<Sensor> implements Duplicable
	{
		@Override
		public Device duplicate()
		{
			return duplicate(false);
		}
		
		@Override
		public Device duplicate(boolean with_state)
		{
			Device d = new Device();
			for (Map.Entry<String,Sensor> e : entrySet())
			{
				d.put(e.getKey(), e.getValue().duplicate());
			}
			return d;
		}
	}

	/**
	 * A sensor is a map from attributes to (arbitrary) values.
	 */
	public static class Sensor extends PrettyTreeMap<Object> implements PrettyPrintable, Duplicable
	{
		protected int m_tag = 0;
		
		@Override
		public Sensor duplicate()
		{
			return duplicate(false);
		}
		
		@Override
		public Sensor duplicate(boolean with_state)
		{
			Sensor s = new Sensor();
			s.putAll(this);
			return s;
		}
		
		public void tag()
		{
			m_tag = 5;
		}
		
		public void untag()
		{
			m_tag = Math.max(m_tag - 1, 0);
		}
		
		@Override
		protected String getBackground()
		{
			float gradient = ((float) m_tag) / 5f;
			return m_tag > 0 ? ("background:rgba(255,0,0," + gradient + ")") : "background: rgba(0.9,0.9,0.9,0.15)";
		}
	}

	/**
	 * A function that updates a single sensor of a house, overwriting any
	 * existing values by the ones provided in the constructor.
	 */
	public static class HouseDelta extends UnaryFunction<House,House>
	{
		protected final long m_timestamp;
		
		protected final String m_location;

		protected final String m_subject;

		protected final String m_device;

		protected final String m_sensor;

		protected final Object m_state;

		public HouseDelta(long timestamp, String location, String subject, String device, String sensor, Object state)
		{
			super(House.class, House.class);
			m_location = location;
			m_subject = subject;
			m_device = device;
			m_sensor = sensor;
			m_state = state;
			m_timestamp = timestamp;
		}

		@Override
		public House getValue(House x)
		{
			Location loc;
			x.untagAll();
			if (x.containsKey(m_location))
			{
				loc = x.get(m_location);
			}
			else
			{
				loc = new Location();
				x.put(m_location, loc);
			}
			Subject sub;
			if (loc.containsKey(m_subject))
			{
				sub = loc.get(m_subject);
			}
			else
			{
				sub = new Subject();
				loc.put(m_subject, sub);
			}
			Device dev;
			if (sub.containsKey(m_device))
			{
				dev = sub.get(m_device);
			}
			else
			{
				dev = new Device();
				sub.put(m_device, dev);
			}
			Sensor sen;
			if (dev.containsKey(m_sensor))
			{
				sen = dev.get(m_sensor);
			}
			else
			{
				sen = new Sensor();
				dev.put(m_sensor, sen);
			}
			sen.put("value", m_state);
			sen.put("time", m_timestamp);
			sen.tag();
			return x;
		}
	}

	/**
	 * Turns a JSON event from the sensor infrastructure into the corresponding
	 * {@link HouseDelta} function instance.
	 */
	public static class EventToHouseDelta extends UnaryFunction<Object,HouseDelta>
	{
		protected final EventFormat s_format;
		
		public EventToHouseDelta(EventFormat format)
		{
			super(Object.class, HouseDelta.class);
			s_format = format;
		}

		@Override
		public HouseDelta getValue(Object x)
		{
			Long timestamp = (Long) evaluateUnary(s_format.timestamp(), x);
			String location = (String) evaluateUnary(s_format.locationString(), x);
			String subject = (String) evaluateUnary(s_format.subjectString(), x);
			String device = (String) evaluateUnary(s_format.modelString(), x);
			String sensor = (String) evaluateUnary(s_format.sensorString(), x);
			String state = (String) evaluateUnary(s_format.stateString(), x);
			return new HouseDelta(timestamp, location, subject, device, sensor, state);
		}
	}

	@Override
	public String toString()
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrettyPrintStream ps = new PrettyPrintStream(baos);
		print(ps);
		return baos.toString();
	}
	
	/**
	 * Unsets the <tt>tag</tt> attribute of all sensors in the house data
	 * structure.
	 */
	public void untagAll()
	{
		for (Location loc : values())
		{
			for (Subject sub : loc.values())
			{
				for (Device dev : sub.values())
				{
					for (Sensor sen : dev.values())
					{
						sen.untag();
					}
				}
			}
		}
	}

	@Override
	public House duplicate()
	{
		return duplicate(false);
	}

	@Override
	public House duplicate(boolean with_state)
	{
		if (with_state)
		{
			throw new UnsupportedOperationException("Cannot duplicate a house with state");
		}
		return new House();
	}
}
