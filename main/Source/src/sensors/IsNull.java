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

import ca.uqac.lif.cep.functions.UnaryFunction;

/**
 * Checks if an object is null.
 * @author Sylvain Hallé
 */
public class IsNull extends UnaryFunction<Object,Boolean>
{
	/**
	 * A single instance of this function.
	 */
	public static final IsNull instance = new IsNull();

	/**
	 * A protected constructor to enforce the singleton pattern.
	 */
	protected IsNull()
	{
		super(Object.class, Boolean.class);
	}

	@Override
	public Boolean getValue(Object x)
	{
		return x == null;
	}
}
