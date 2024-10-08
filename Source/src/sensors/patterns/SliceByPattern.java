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

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Slice;
import sensors.UnpackMap;

public class SliceByPattern extends GroupProcessor
{
	/**
	 * The processor to evaluate on each slice.
	 */
	/*@ non_null @*/ protected final Processor m_p;
	
	/**
	 * The function to use to slice the input stream.
	 */
	/*@ non_null @*/ protected final Function m_f;
	
	/**
	 * Creates a new instance of the pattern.
	 * @param f The function to use to slice the input stream
	 * @param p The processor to evaluate on each slice
	 */
	public SliceByPattern(Function f, Processor p)
	{
		super(1, 1);
		m_p = p;
		m_f = f;
		Slice s = new Slice(f, p);
		KeepLast kl = new KeepLast();
		Connector.connect(s, kl);
		UnpackMap um = new UnpackMap();
		Connector.connect(kl, um);
		addProcessors(s, kl, um);
		associateInput(s);
		associateOutput(um);
	}
	
	@Override
	public SliceByPattern duplicate(boolean with_state)
	{
		return new SliceByPattern(m_f.duplicate(with_state), m_p.duplicate(with_state));
	}
}
