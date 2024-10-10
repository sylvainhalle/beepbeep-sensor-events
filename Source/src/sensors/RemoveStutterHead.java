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

import java.util.Queue;

import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.util.Equals;

/**
 * Removes the head of a stuttering sequence. This processor takes as input
 * a sequence of events, and outputs the same sequence, except that the
 * first event is removed if it is identical to the one after it. What decides
 * on the equality of two events is configurable, by passing a 2:1 function
 * to the processor's constructor.
 * 
 * @author Sylvain Hallé
 */
public class RemoveStutterHead extends SynchronousProcessor
{
	/**
	 * The function that decides on whether two successive events are
	 * considered equal (and the second one therefore seen as a repetition of the
	 * first one). This function must take two events as input, and output
	 * a Boolean value.
	 */
	protected final Function m_comparison;
	
	/**
	 * The first event that was examined by the processor in this run of
	 * "similar" events.
	 */
	protected Object m_first;
	
	/**
	 * The last event that was received by the processor.
	 */
	protected Object m_last;
	
	/**
	 * Creates a new instance of the pattern.
	 * @param comparison The function that decides on whether two successive
	 * events are considered equal.
	 */
	public RemoveStutterHead(Function comparison)
	{
		super(1, 1);
		m_comparison = comparison;
		m_first = null;
		m_last = null;
	}
	
	public RemoveStutterHead()
	{
		this(Equals.instance);
	}
	
	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		if (m_first == null)
		{
			m_first = inputs[0];
			m_last = inputs[0];
			return true;
		}
		Object current = inputs[0];
		Object[] out = new Object[1];
		m_comparison.evaluate(new Object[] {m_first, current}, out);
		if (Boolean.TRUE.equals(out[0]))
		{
			// The two events are considered equal; do nothing
			m_last = current;
			return true;
		}
		// The two events are different; output the last event and update
		// the reference to the last event
		outputs.add(new Object[] {m_last});
		m_last = current;
		m_first = current;
		return true;
	}
	
	@Override
	public void reset()
	{
		super.reset();
		m_first = null;
		m_last = null;
	}
	
	@Override
	public boolean onEndOfTrace(Queue<Object[]> outputs) throws ProcessorException
	{
		if (m_last != null)
		{
			outputs.add(new Object[] { m_last });
			return true;
		}
		return false;
	}
	
	@Override
	public RemoveStutterHead duplicate(boolean with_state)
	{
		if (with_state)
		{
			throw new ProcessorException("Cannot duplicate this processor with state");
		}
		return new RemoveStutterHead(m_comparison);
	}
}
