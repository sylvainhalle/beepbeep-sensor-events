/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023 Sylvain Hallé

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
package nears.util;

import java.util.Iterator;
import java.util.Queue;
import java.util.TreeSet;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.json.JsonElement;
import ca.uqac.lif.json.JsonPath;
import ca.uqac.lif.json.JsonString;
import nears.DateToTimestamp;
import nears.SensorEvent;

/**
 * Processor that ingests a stream of events, and, upon reaching the end,
 * outputs them in sorted order of their timestamp. This is useful to read
 * a source containing scrambled events, and put them back according to their
 * temporal ordering.
 * 
 * @author Sylvain Hallé
 */
public class OrderTimestamps extends SynchronousProcessor
{
	/**
	 * The {@link TreeSet} used to store the events and enumerate them in sorted
	 * timestamp order. 
	 */
	/*@ non_null @*/ protected final TreeSet<JsonEvent> m_events;
	
	/**
	 * Creates a new instance of the processor.
	 */
	public OrderTimestamps()
	{
		super(1, 1);
		m_events = new TreeSet<JsonEvent>();
	}
	
	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		JsonElement e = (JsonElement) inputs[0];
		long ts = DateToTimestamp.getTimestamp(((JsonString) JsonPath.get(e, SensorEvent.JP_TIMESTAMP)).stringValue());
		m_events.add(new JsonEvent(ts, e));
		return true;
	}
	
	@Override
	protected boolean onEndOfTrace(Queue<Object[]> outputs) throws ProcessorException
  {
		Iterator<JsonEvent> it = m_events.iterator();
		while (it.hasNext())
		{
			JsonEvent je = it.next();
			outputs.add(new Object[] {je.m_event});
		}
    return false;
  }

	@Override
	public Processor duplicate(boolean with_state)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * A utility class that associates a JSON element to the timestamp it
	 * contains. This allows the {@link TreeMap} to sort them.
	 */
	protected static class JsonEvent implements Comparable<JsonEvent>
	{
		protected final long m_timestamp;
		
		protected final JsonElement m_event;
		
		public JsonEvent(long timestamp, JsonElement e)
		{
			super();
			m_timestamp = timestamp;
			m_event = e;
		}
		
		@Override
		public int compareTo(JsonEvent e)
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
}
