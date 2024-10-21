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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.cep.UniformProcessor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.tmf.Fork;

public class CoOccurs extends GroupProcessor
{
	protected final long m_width;
	
	protected final Function m_functionTimestamp;
	
	protected final Processor m_condition;
	
	protected final Processor[] m_conditions;
	
	public CoOccurs(long width, Function f_ts, Processor w_cond, Processor ... conditions)
	{
		super(1, 1);
		m_width = width;
		m_functionTimestamp = f_ts;
		m_condition = w_cond;
		m_conditions = conditions;
		Fork f1 = new Fork(3);
		Connector.connect(f1, 0, w_cond, 0);
		Fork f2 = new Fork(conditions.length);
		Connector.connect(f1, 1, f2, 0);
		ApplyFunction get_ts = new ApplyFunction(f_ts);
		Connector.connect(f1, 2, get_ts, 0);
		Fork f3 = new Fork(conditions.length + 1);
		Connector.connect(get_ts, 0, f3, 0);
		CheckCoOccurrence cco = new CheckCoOccurrence(m_width, conditions.length);
		addProcessors(f1, f2, f3, w_cond, get_ts, cco);
		for (int i = 0; i < conditions.length; i++)
		{
			Connector.connect(f2, i, conditions[i], 0);
			LastTrueTimestamp ltt = new LastTrueTimestamp();
			Connector.connect(conditions[i], 0, ltt, 1);
			Connector.connect(f3, i, ltt, 0);
			addProcessors(conditions[i], ltt);
			Connector.connect(ltt, 0, cco, i + 2);
		}
		Connector.connect(f3, conditions.length, cco, 0);
		Connector.connect(w_cond, 0, cco, 1);
		associateInput(0, f1, 0);
		associateOutput(0, cco, 0);
	}


	@Override
	public CoOccurs duplicate(boolean with_state)
	{
		if (with_state)
		{
			throw new UnsupportedOperationException("Stateful duplication not supported for this processor");
		}
		return new CoOccurs(m_width, m_functionTimestamp, m_condition, m_conditions);
	}
	
	/**
	 * Receives two streams of events, a timestamp and a Boolean value.
	 * Outputs the last timestamp at which the second stream was true. If a true
	 * value has not yet been observed, a negative timestamp is returned.
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
			m_lastTimestamp = -1;
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
			m_lastTimestamp = -1;
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
	
	public static class CheckCoOccurrence extends SynchronousProcessor
	{
		protected final long m_width;
		
		protected final List<WindowCandidate> m_candidates;
		
		public CheckCoOccurrence(long width, int num_conditions)
		{
			super(num_conditions + 2, 1);
			m_width = width;
			m_candidates = new LinkedList<>();
		}
		
		@Override
		protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
		{
			long ts = ((Number) inputs[0]).longValue();
			m_candidates.add(new WindowCandidate(ts, Boolean.TRUE.equals(inputs[1])));
			Iterator<WindowCandidate> it = m_candidates.iterator();
			while (it.hasNext())
			{
				WindowCandidate wc = it.next();
				wc.feed(inputs);
				Boolean status = wc.getStatus();
				if (status == null)
				{
					break;
				}
				outputs.add(new Object[] {status});
				it.remove();
			}
			return true;
		}

		@Override
		public Processor duplicate(boolean with_state)
		{
			// TODO Auto-generated method stub
			return null;
		}
		
		protected class WindowCandidate
		{
			protected final long m_initTimestamp;
			
			protected Boolean m_status;
			
			public WindowCandidate(long ts, boolean condition)
			{
				super();
				m_initTimestamp = ts;
				m_status = !condition ? true : null;
			}
			
			public Boolean getStatus()
			{
				return m_status;
			}
			
			public void feed(Object[] front)
			{
				if (m_status != null)
				{
					// Status already decided
					return;
				}
				long ts = ((Number) front[0]).longValue();
				// Check interval of time between min and max
				long min_ts = Long.MAX_VALUE;
				long max_ts = Long.MIN_VALUE;
				for (int i = 2; i < front.length; i++)
				{
					long cur_ts = ((Number) front[i]).longValue();
					min_ts = Math.min(min_ts, cur_ts);
					max_ts = Math.max(max_ts, cur_ts);
				}
				boolean all_defined = min_ts >= 0 && max_ts >= 0;
				boolean in_window = max_ts - min_ts <= m_width;
				boolean includes_timestamp = m_initTimestamp >= min_ts && m_initTimestamp <= max_ts;
				if (all_defined && in_window && includes_timestamp)
				{
					m_status = true;
					return;
				}
				if (ts - m_initTimestamp < m_width)
				{
					// Not enough time elapsed to decide yet
					return;
				}
				m_status = false;
			}
		}
	}
	
}
