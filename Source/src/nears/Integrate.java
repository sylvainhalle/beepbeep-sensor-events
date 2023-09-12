package nears;

import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.UniformProcessor;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionException;

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
