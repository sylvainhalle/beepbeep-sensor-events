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

import java.util.Queue;

import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.cep.functions.Function;

public class QuietenDown extends SynchronousProcessor
{
	protected final Function m_condition;
	
	protected Object m_lastValue;
	
	public QuietenDown(Function condition)
	{
		super(1, 1);
		m_condition = condition;
		m_lastValue = null;
	}

	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		Object[] out = new Object[1];
		out[0] = false;
		if (m_lastValue != null)
		{
			m_condition.evaluate(new Object[] {m_lastValue, inputs[0]}, out);
		}
		if (m_lastValue == null || (Boolean) out[0])
		{
			m_lastValue = inputs[0];
			outputs.add(new Object[] {true});
		}
		else
		{
			outputs.add(new Object[] {false});
		}
		return true;
	}

	@Override
	public QuietenDown duplicate(boolean with_state)
	{
		QuietenDown q = new QuietenDown(m_condition);
		if (with_state)
		{
			q.m_lastValue = m_lastValue;
		}
		return q;
	}
}
