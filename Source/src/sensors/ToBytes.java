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

import ca.uqac.lif.cep.functions.UnaryFunction;

/**
 * Converts a string into an array of bytes by calling the
 * {@link String#getBytes()} method.
 * @author Sylvain Hallé
 */
public class ToBytes extends UnaryFunction<String,byte[]>
{
	/**
	 * A public instance of this function.
	 */
	public static final transient ToBytes instance = new ToBytes();
	
	/**
	 * Creates a new instance of the function.
	 */
	protected ToBytes()
	{
		super(String.class, byte[].class);
	}
	
	@Override
	public byte[] getValue(String x)
	{
		if (x == null)
		{
			return null;
		}
		return x.getBytes();
	}
}
