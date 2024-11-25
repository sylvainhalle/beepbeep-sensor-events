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
package sensors.opportunity;

import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.Tuple;

/**
 * Fetches an attribute from a tuple. If the value of the attribute is null,
 */
public class FetchAttributeOrDefault extends FetchAttribute
{
	/**
	 * The default value to output in case the attribute's value is null.
	 */
	protected final Object m_default;
	
	public FetchAttributeOrDefault(String attribute_name, Object def)
	{
		super(attribute_name);
		m_default = def;
	}
	
	@Override
	public Object getValue(Tuple t) 
	{
		Object v = super.getValue(t);
		return v == null ? m_default : v;
	}
	
	@Override
	public FetchAttributeOrDefault duplicate(boolean with_state)
	{ 
		return new FetchAttributeOrDefault(getAttributeName(), m_default);
	}
}
