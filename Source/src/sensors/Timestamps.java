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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Locale;

import beepbeep.groovy.Numbers;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.UnaryFunction;

/**
 * A set of functions to manipulate Unix timestamps.
 * 
 * @author Sylvain Hallé
 */
public class Timestamps
{
	/**
	 * Extracts the year from a Unix timestamp.
	 */
	public static final transient GetYear getYear = new GetYear();

	/**
	 * A function that extracts a timestamp from an event, and applies some
	 * processing to its corresponding {@link Instant}.
	 * @param <T> The type of the output of the function
	 */
	public static abstract class TimestampFunction<T> extends UnaryFunction<Number,T>
	{
		/**
		 * Creates a new instance of the function.
		 * @param clazz The class of the output of the function
		 */
		protected TimestampFunction(Class<T> clazz)
		{
			super(Number.class, clazz);
		}

		@Override
		public T getValue(Number x)
		{
			Instant instant = Instant.ofEpochMilli(x.longValue());
			LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
			return processInstant(dateTime);
		}

		/**
		 * Processes an {@link Instant} object.
		 * @param d The instant to process
		 * @return The result of the processing
		 */
		protected abstract T processInstant(LocalDateTime d);
	}

	/**
	 * Gets the year from a Unix timestamp.
	 */
	public static class GetYear extends TimestampFunction<Number>
	{
		/**
		 * Creates a new instance of the function.
		 */
		protected GetYear()
		{
			super(Number.class);
		}

		@Override
		public Number processInstant(LocalDateTime d)
		{
			return d.getYear();
		}
	}

	/**
	 * Gets the year-week pair from a Unix timestamp. The year-week
	 * pair is a string of the form "YYYY-WW", where YYYY is the year
	 * and WW is the week number in the year (with a leading zero if
	 * necessary).
	 */
	public static class GetYearWeek extends TimestampFunction<String>
	{
		/**
		 * Creates a new instance of the function.
		 */
		protected GetYearWeek()
		{
			super(String.class);
		}

		@Override
		public String processInstant(LocalDateTime d)
		{
			int year = d.getYear();
			WeekFields weekFields = WeekFields.of(Locale.getDefault());
			int week_nb = d.get(weekFields.weekOfWeekBasedYear());
			return String.format("%d-%02d", year, week_nb);
		}
	}
	
	/**
	 * A function that transforms a number of minutes into a number of
	 * milliseconds.
	 */
	public static class Minutes extends FunctionTree
	{
		public Minutes(Function x)
		{
			super(Numbers.multiplication, x, new Constant(60000l));
		}
	}
	
	/**
	 * A function that transforms a number of hours into a number of
	 * milliseconds.
	 */
	public static class Hours extends FunctionTree
	{
		public Hours(Function x)
		{
			super(Numbers.multiplication, x, new Constant(60 * 60000l));
		}
	}
}