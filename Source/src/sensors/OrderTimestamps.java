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

import java.util.Iterator;
import java.util.Queue;
import java.util.TreeSet;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.cep.functions.Function;

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
	/*@ non_null @*/ protected final TreeSet<TimedEvent> m_events;
	
	/**
	 * The function that extracts a (Unix) timestamp from an event.
	 */
	protected final Function m_timestampFunction;
	
	/**
	 * Creates a new instance of the processor.
	 * @param The function that extracts a (Unix) timestamp from an event
	 */
	public OrderTimestamps(Function ts_function)
	{
		super(1, 1);
		m_timestampFunction = ts_function;
		m_events = new TreeSet<TimedEvent>();
	}
	
	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		Object[] outs = new Object[1];
		m_timestampFunction.evaluate(inputs, outs);
		long ts = (long) outs[0];
		m_events.add(new TimedEvent(ts, inputs[0]));
		return true;
	}
	
	@Override
	protected boolean onEndOfTrace(Queue<Object[]> outputs) throws ProcessorException
  {
		Iterator<TimedEvent> it = m_events.iterator();
		while (it.hasNext())
		{
			TimedEvent je = it.next();
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
}
