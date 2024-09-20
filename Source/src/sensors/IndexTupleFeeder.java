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

import ca.uqac.lif.cep.tuples.FixedTupleBuilder;
import ca.uqac.lif.cep.tuples.TupleFeeder;

/**
 * A tuple feeder that adds an attribute to each created tuple containing the
 * index of the tuple (i.e.<!-- --> its location in the ordered sequence of
 * tuples since the instantiation of the processor).
 */
public class IndexTupleFeeder extends TupleFeeder
{
	/**
	 * The attribute in the tuple containing the event index.
	 */
	public static final String INDEX_ATTRIBUTE = "index";
	
	/**
	 * The original attributes of the tuple.
	 */
	protected final String[] m_attributes;
	
	/**
	 * Creates a tuple feeder.
	 * @param attributes The attributes to create for each tuple
	 */
	public IndexTupleFeeder(String ... attributes)
	{
		super();
		m_attributes = attributes;
		String[] atts = new String[attributes.length + 1];
		atts[0] = INDEX_ATTRIBUTE;
		for (int i = 0; i < attributes.length; i++)
		{
			atts[i + 1] = attributes[i];
		}
		m_builder = new FixedTupleBuilder(atts);
	}
	
	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		String token = ((String) inputs[0]).trim();
		if (token.isEmpty() || token.startsWith("#"))
		{
			// Ignore comment and empty lines
			return true;
		}
		String[] parts = token.split(m_separator);
		String[] ins = new String[m_attributes.length + 1];
		ins[0] = Long.toString(m_inputCount++);
		for (int i = 0; i < m_attributes.length; i++)
		{
			if (i < parts.length)
			{
				ins[i + 1] = parts[i];
			}
			else
			{
				ins[i + 1] = "";
			}
		}
		outputs.add(new Object[]{m_builder.createTupleFromString(ins)});
		return true;
	}
	
	@Override
	public IndexTupleFeeder setSeparator(String s)
	{
		super.setSeparator(s);
		return this;
	}
}
