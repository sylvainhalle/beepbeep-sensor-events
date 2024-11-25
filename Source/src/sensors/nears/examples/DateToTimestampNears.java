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
package sensors.nears.examples;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import ca.uqac.lif.cep.functions.FunctionException;
import ca.uqac.lif.cep.functions.UnaryFunction;

/**
 * Converts a date into a Unix timestamp.
 * 
 * @author Sylvain Hallé
 */
public class DateToTimestampNears extends UnaryFunction<String,Number>
{	
	/**
	 * A publicly visible instance of the function.
	 */
	/*@ non_null @*/ public static final transient DateToTimestampNears instance = new DateToTimestampNears();
	
	/**
	 * The date formatter used to parse the date string.
	 */
	/*@ non_null @*/ protected static final DateFormat s_format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

	/**
	 * Creates a new instance of the function.
	 */
	protected DateToTimestampNears()
	{
		super(String.class, Number.class);
	}

	@Override
	public Number getValue(String s)
	{
		try
		{
			return s_format.parse(s).getTime();
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
			return s_format.parse(s).getTime();
		}
		catch (ParseException e)
		{
			return 0l;
		}
	}
}
