/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023 Sylvain Hallé

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
package sensors.nears;

import ca.uqac.lif.cep.functions.UnaryFunction;

/**
 * Converts a temperature string in the form "10.9 °C" or "2E+1 °C" into a
 * number (e.g.<!-- --> 10.9 or 20).
 * 
 * @author Sylvain Hallé
 */
public class ReadTemperature extends UnaryFunction<String,Number>
{
	/**
	 * A publicly visible instance of the function.
	 */
	/*@ non_null @*/ public static final transient ReadTemperature instance = new ReadTemperature();
	
	/**
	 * Creates a new instance of the function.
	 */
	protected ReadTemperature()
	{
		super(String.class, Number.class);
	}

	@Override
	public Number getValue(String s)
	{
		return readValue(s);
	}
	
	public static Number readValue(String s)
	{
		String temp = s;
		if (s.endsWith("°C"))
		{
			// Temperature
			temp = s.substring(0, s.length() - 2).trim();
		}
		return Float.valueOf(temp);
	}

}
