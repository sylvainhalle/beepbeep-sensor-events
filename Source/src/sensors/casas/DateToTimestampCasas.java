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
package sensors.casas;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import ca.uqac.lif.cep.functions.FunctionException;
import ca.uqac.lif.cep.functions.UnaryFunction;

/**
 * Converts a (string) date into a Unix timestamp.
 * 
 * @author Sylvain Hallé
 */
public class DateToTimestampCasas extends UnaryFunction<String,Number>
{	
	/**
	 * A publicly visible instance of the function.
	 */
	/*@ non_null @*/ public static final transient DateToTimestampCasas instance = new DateToTimestampCasas();
	
	/**
	 * The date formatter used to parse the date string.
	 */
	/*@ non_null @*/ protected static final DateFormat s_format = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS");

	/**
	 * Creates a new instance of the function.
	 */
	protected DateToTimestampCasas()
	{
		super(String.class, Number.class);
	}

	@Override
	public Number getValue(String s)
	{
		try
		{
			return s_format.parse(pad(s)).getTime();
		}
		catch (ParseException e)
		{
			throw new FunctionException(e);
		}
	}
	
	public static long getTimestamp(String s)
	{
		try
		{
			return s_format.parse(pad(s)).getTime();
		}
		catch (ParseException e)
		{
			return 0l;
		}
	}
	
	/**
	 * Pads the string to account for the fact that not all time values in the
	 * log are formatted in the same way. The number of digits in the fractions
	 * of a second may vary from 0 to 6, and the decimal period is not present
	 * when there are 0 digits. The method formats it so that all time values
	 * have exactly 3 decimals.
	 * @param s The string
	 * @return
	 */
	protected static String pad(String s)
	{
		int len = s.length();
		// String has too many decimals, truncate
		if (len > 23)
		{
			return s.substring(0, 22);
		}
		else if (len == 22)
		{
			return s + "0";
		}
		else if (len == 21)
		{
			return s + "00";
		}
		else if (len == 20)
		{
			return s + "000";
		}
		else if (len == 19)
		{
			return s + ".000";
		}
		return s; // Should not happen
	}
}
