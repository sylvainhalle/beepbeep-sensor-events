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

import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.UniformProcessor;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionException;

/**
 * The integrator (more to come).
 */
public class Integrate extends UniformProcessor
{
	/*@ non_null @*/ protected final Object m_startValue;
	
	protected Object m_lastValue;
	
	public Integrate(/*@ non_null @*/ Object start_value)
	{
		super(1, 1);
		m_startValue = start_value;
		m_lastValue = m_startValue;
	}

	@Override
	protected boolean compute(Object[] inputs, Object[] outputs)
	{
		Function f = (Function) inputs[0];
		Object[] f_out = new Object[1];
		try
		{
			f.evaluate(new Object[] {m_lastValue}, f_out);
		}
		catch (FunctionException e)
		{
			throw new ProcessorException(e);
		}
		m_lastValue = f_out[0];
		outputs[0] = f_out[0];
		return true;
	}
	
	@Override
	public void reset()
	{
		super.reset();
		m_lastValue = m_startValue;
	}

	@Override
	public Integrate duplicate(boolean with_state)
	{
		Integrate i = new Integrate(m_startValue);
		if (with_state)
		{
			i.m_lastValue = m_startValue;
		}
		return i;
	}
}
