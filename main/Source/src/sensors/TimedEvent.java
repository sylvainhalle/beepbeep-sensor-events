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
package sensors;

/**
 * A utility class that associates an event to the timestamp it
 * contains. This allows the {@link TreeMap} to sort them.
 */
public class TimedEvent implements Comparable<TimedEvent>
{
	protected final long m_timestamp;

	protected final Object m_event;

	public TimedEvent(long timestamp, Object e)
	{
		super();
		m_timestamp = timestamp;
		m_event = e;
	}
	
	public Object getEvent()
	{
		return m_event;
	}
	
	public long getTimestamp()
	{
		return m_timestamp;
	}

	@Override
	public int compareTo(TimedEvent e)
	{
		if (m_timestamp == e.m_timestamp)
		{
			return 0;
		}
		if (m_timestamp > e.m_timestamp)
		{
			return 1;
		}
		return -1;
	}
}