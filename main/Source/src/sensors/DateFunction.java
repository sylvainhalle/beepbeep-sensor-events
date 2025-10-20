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
package sensors;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import ca.uqac.lif.cep.functions.UnaryFunction;

/**
 * A function that extracts a value from an epoch time.
 * @author Sylvain Hallé
 */
public abstract class DateFunction extends UnaryFunction<Number,Number>
{
	/**
	 * The time zone relative to which features are expressed.
	 */
	public static final TimeZone TIME_ZONE = TimeZone.getDefault();
	
	/**
	 * A publicly visible instance of the {@link DayOfYear} function.
	 */
	public static final DayOfYear dayOfYear = new DayOfYear();
	
	/**
	 * A publicly visible instance of the {@link WeekOfYear} function.
	 */
	public static final WeekOfYear weekOfYear = new WeekOfYear();
	
	/**
	 * A publicly visible instance of the {@link TimeOfDay} function.
	 */
	public static final TimeOfDay timeOfDay = new TimeOfDay();
	
	/**
	 * The calendar instance used to extract data from the timestamps.
	 */
	protected static final Calendar s_calendar;
	
	static
	{
		s_calendar = new GregorianCalendar(TIME_ZONE);
	}	
	
	public DateFunction()
	{
		super(Number.class, Number.class);
	}
	
	/**
	 * Function that extracts the day of the year of a given timestamp.
	 */
	public static class DayOfYear extends DateFunction
	{
		protected DayOfYear()
		{
			super();
		}

		@Override
		public Number getValue(Number x)
		{
			s_calendar.setTimeInMillis(x.longValue());
			return s_calendar.get(Calendar.DAY_OF_YEAR);
		}	
	}
	
	/**
	 * Function that extracts the week of the year of a given timestamp.
	 */
	public static class WeekOfYear extends DateFunction
	{
		protected WeekOfYear()
		{
			super();
		}

		@Override
		public Number getValue(Number x)
		{
			s_calendar.setTimeInMillis(x.longValue());
			return s_calendar.get(Calendar.WEEK_OF_YEAR);
		}	
	}

	/**
	 * From a timestamp, gets the number of seconds elapsed since the start of
	 * the day in which the timestamp lies.
	 */
	public static class TimeOfDay extends DateFunction
	{
		protected TimeOfDay()
		{
			super();
		}

		@Override
		public Number getValue(Number x)
		{
			s_calendar.setTimeInMillis(x.longValue());
			long hour = s_calendar.get(Calendar.HOUR_OF_DAY);
			long minute = s_calendar.get(Calendar.MINUTE);
			long second = s_calendar.get(Calendar.SECOND);
			return hour * 3600l + minute * 60l + second;
		}	
	}
}
