/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023-2024 Sylvain Hallé, Rania Taleb

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

import java.util.HashMap;
import java.util.Map;

import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.UnaryFunction;

@SuppressWarnings("rawtypes")
public class FilterMap extends UnaryFunction<Map,Map>
{
	protected final Function m_condition;
	
	protected FilterMap(Function condition)
	{
		super(Map.class, Map.class);
		m_condition = condition;
	}

	@Override
	public Map getValue(Map m)
	{
		Map<Object,Object> out_map = new HashMap<Object,Object>();
		for (Object k : m.keySet())
		{
			Object v = m.get(k);
			Object[] out = new Object[1];
			m_condition.evaluate(new Object[] {k, v}, out);
			if (Boolean.TRUE.equals(out[0]))
			{
				out_map.put(k, v);
			}
		}
		return out_map;
	}
	
	@Override
	public FilterMap duplicate(boolean with_state)
	{
		return new FilterMap(m_condition);
	}
}
