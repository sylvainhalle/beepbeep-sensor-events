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
package sensors.patterns;

import java.util.Queue;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.SynchronousProcessor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Numbers;

/**
 * Filters events based on their index in the stream.
 */
public class IndexRangePattern extends GroupProcessor
{
	/**
	 * The index of the first event to keep.
	 */
	protected final int m_start;
	
	/**
	 * The index of the last event to keep.
	 */
	protected final int m_end;
	
	/**
	 * Creates a new index range pattern.
	 * @param start The index of the first event to keep
	 * @param end The index of the last event to keep
	 */
	public IndexRangePattern(int start, int end)
	{
		super(1, 1);
		m_start = start;
		m_end = end;
		Fork f = new Fork();
		Filter fil = new Filter();
		Connector.connect(f, 0, fil, 0);
		CounterPattern cp = new CounterPattern();
		Connector.connect(f, 1, cp, 0);
		ApplyFunction ite = new ApplyFunction(new FunctionTree(Booleans.and,
				new FunctionTree(Numbers.isGreaterOrEqual, StreamVariable.X, new Constant(start)),
				new FunctionTree(Numbers.isLessOrEqual, StreamVariable.X, new Constant(end))
				));
		Connector.connect(cp, ite);
		EndIfFalse eif = new EndIfFalse();
		Connector.connect(ite, eif);
		Connector.connect(eif, 0, fil, 1);
		associateInput(f);
		associateOutput(fil);
		addProcessors(f, fil, cp, ite);
	}
	
	@Override
	public IndexRangePattern duplicate(boolean with_state)
	{
		if (with_state)
		{
			throw new ProcessorException("Cannot duplicate processor with state");
		}
		return new IndexRangePattern(m_start, m_end);
	}
	
	/**
	 * A passthrough that sends the end of stream signal whenever it receives
	 * the input event {@code false}.
	 */
	protected static class EndIfFalse extends SynchronousProcessor
	{
		public EndIfFalse()
		{
			super(1, 1);
		}

		@Override
		protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
		{
			if (Boolean.FALSE.equals(inputs[0]))
			{
				return false;
			}
			outputs.add(new Object[] {inputs[0]});
			return true;
		}

		@Override
		public EndIfFalse duplicate(boolean with_state)
		{
			return new EndIfFalse();
		}
	}
}
