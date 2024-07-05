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

import ca.uqac.lif.cep.SynchronousProcessor;

/**
 * A processor that repeats the first event it receives a specified number of
 * times, after which it lets all other events through.
 */
public class StutterFirst extends SynchronousProcessor
{
	/**
	 * The event to repeat.
	 */
	protected Object m_first;
	
	/**
	 * The number of times to repeat the first event.
	 */
	protected final int m_times;
	
	/**
	 * Creates a new instance of the processor.
	 * @param times The number of times to repeat the first event
	 */
	public StutterFirst(int times)
	{
		super(1, 1);
		m_times = times;
	}

	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		if (m_first == null)
		{
			m_first = inputs[0];
			for (int i = 0; i < m_times; i++)
			{
				outputs.add(new Object[] {m_first});
			}
		}
		else
		{
			outputs.add(new Object[] {inputs[0]});
		}
		return true;
	}

	@Override
	public StutterFirst duplicate(boolean with_state)
	{
		StutterFirst sf = new StutterFirst(m_times);
		if (with_state)
		{
			sf.m_first = m_first;
		}
		return sf;
	}
}
