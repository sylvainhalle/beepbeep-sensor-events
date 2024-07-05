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
package sensors.house;

import ca.uqac.lif.cep.Duplicable;
import ca.uqac.lif.cep.functions.UnaryFunction;

/**
 * Function that creates a copy of a {@link Duplicable} object.
 */
public class Duplicate extends UnaryFunction<Object,Object>
{
	/**
	 * A single publicly visible instance of the function.
	 */
	public static final Duplicate instance = new Duplicate();
	
	/**
	 * Creates a new instance of the function.
	 */
	protected Duplicate()
	{
		super(Object.class, Object.class);
	}

	@Override
	public Object getValue(Object x)
	{
		return ((Duplicable) x).duplicate();
	}
}
