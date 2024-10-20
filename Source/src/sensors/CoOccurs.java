package sensors;

import java.util.Queue;

import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.cep.UniformProcessor;

public class CoOccurs extends SynchronousProcessor
{
	
	public CoOccurs()
	{
		super(2, 1);
	}

	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		
		return true;
	}

	@Override
	public SynchronousProcessor duplicate(boolean with_state)
	{
		return new CoOccurs();
	}
	
	/**
	 * Receives two streams of events, a timestamp and a Boolean value.
	 * Outputs the last timestamp at which the second stream was true.
	 */
	public static class LastTrueTimestamp extends UniformProcessor
	{
		/**
		 * The last timestamp at which the input of the second stream was true.
		 */
		protected Number m_lastTimestamp;
		
		/**
		 * Creates a new instance of the processor.
		 */
		public LastTrueTimestamp()
		{
			super(2, 1);
			m_lastTimestamp = 0;
		}
		
		@Override
		protected boolean compute(Object[] inputs, Object[] outputs)
		{
			Number ts = (Number) inputs[0];
			Boolean b = (Boolean) inputs[1];
			if (b)
			{
				m_lastTimestamp = ts;
			}
			outputs[0] = m_lastTimestamp;
			return true;
		}
		
		@Override
		public void reset()
		{
			super.reset();
			m_lastTimestamp = 0;
		}
		
		@Override
		public LastTrueTimestamp duplicate(boolean with_state)
		{
			LastTrueTimestamp l = new LastTrueTimestamp();
			if (with_state)
			{
				l.m_lastTimestamp = m_lastTimestamp;
			}
			return l;
		}
	}
}
