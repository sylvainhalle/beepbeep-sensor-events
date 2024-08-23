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
package sensors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.TreeSet;

import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.cep.functions.Function;

/**
 * Reorders a stream of events by increasing timestamp values. The processor
 * assumes that from each incoming event can be extracted a "timestamp" in the
 * form of a <tt>long</tt> integer, using a user-supplied function. Events are
 * then kept in a buffer and, if their extracted timestamp is not motonically
 * increasing, are put back in the correct order.
 */
public class Reorder extends SynchronousProcessor
{
	/**
	 * The function extracting the timestamp from an event.
	 */
	protected final Function m_ordering;
	
	/**
	 * The maximum time interval by which two events may be out of order.
	 */
	protected final long m_interval;
	
	/**
	 * The timestamp of the last event emitted by the processor so far. 
	 */
	protected long m_lastTimestamp;
	
	/**
	 * The {@link TreeSet} used to store the events and enumerate them in sorted
	 * timestamp order. 
	 */
	/*@ non_null @*/ protected final TreeSet<TimedEvent> m_events;
	
	public Reorder(Function ordering, long interval)
	{
		super(1, 1);
		m_ordering = ordering;
		m_interval = interval;
		m_events = new TreeSet<>();
		m_lastTimestamp = -1;
	}

	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		Object[] outs = new Object[1];
		m_ordering.evaluate(inputs, outs);
		long time = (Long) outs[0];
		TimedEvent te = new TimedEvent(time, inputs[0]);
		m_events.add(te);
		m_lastTimestamp = Math.max(m_lastTimestamp, time);
		List<Object> to_output = getEventsToRelease();
		for (Object o : to_output)
		{
			outputs.add(new Object[] {o});
		}
		return true;
	}
	
	protected List<Object> getEventsToRelease()
	{
		List<Object> out = new ArrayList<Object>();
		Iterator<TimedEvent> it = m_events.iterator();
		while (it.hasNext())
		{
			TimedEvent e = it.next();
			if (m_lastTimestamp - e.m_timestamp >= m_interval)
			{
				out.add(e.m_event);
				it.remove();
			}
			else
			{
				break;
			}
		}
		return out; 
	}

	@Override
	public Reorder duplicate(boolean with_state)
	{
		Reorder r = new Reorder(m_ordering, m_interval);
		if (with_state)
		{
			r.m_events.addAll(m_events);
			r.m_lastTimestamp = m_lastTimestamp;
		}
		return r;
	}
	
	@Override
	public void reset()
	{
		super.reset();
		m_events.clear();
		m_lastTimestamp = -1;
	}
}
