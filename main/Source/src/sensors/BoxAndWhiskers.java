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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ca.uqac.lif.cep.functions.UnaryFunction;

/**
 * Computes the five-number summary of a collection of numbers. The five
 * numbers are, in order: the minimum, the first quartile, the median, the
 * third quartile, and the maximum. These numbers comprise the necessary data
 * to draw a
 * <a href="https://en.wikipedia.org/wiki/Box_plot">box-and-whiskers</a> plot.
 * @author Sylvain Hallé
 */
@SuppressWarnings("rawtypes")
public class BoxAndWhiskers extends UnaryFunction<Collection,Number[]>
{
	/**
	 * A single publicly visible instance of the BoxAndWhiskers function.
	 */
	public static final transient BoxAndWhiskers instance = new BoxAndWhiskers();

	/**
	 * Creates a new instance of the function.
	 */
	protected BoxAndWhiskers()
	{
		super(Collection.class, Number[].class);
	}

	@Override
	public Number[] getValue(Collection c)
	{
		// Convert the collection to a List of Doubles
		List<Double> number_list = new ArrayList<>();
		for (Object o : c)
		{
			Double n = toNumber(o);
			if (n == null)
			{
				throw new IllegalArgumentException("The collection must contain only numbers");
			}
			number_list.add(n);
		}
		Collections.sort(number_list);
		Number[] out = new Number[5];
		out[0] = getPercentile(number_list, 0);
		out[1] = getPercentile(number_list, 25);
		out[2] = getPercentile(number_list, 50);
		out[3] = getPercentile(number_list, 75);
		out[4] = getPercentile(number_list, 100);
		return out;
	}

	@Override
	public BoxAndWhiskers duplicate(boolean with_state)	
	{
		return instance;
	}

	/**
	 * Returns the value corresponding to a given percentile of a sorted list of
	 * numbers. The function interpolates between the two surrounding values if
	 * necessary.
	 * @param sorted_list The list of numbers, sorted in ascending order
	 * @param percentile The percentile
	 * @return The value corresponding to the percentile
	 */
	protected static double getPercentile(List<Double> sorted_list, double percentile)
	{
		int size = sorted_list.size();
		if (size == 0) return Double.NaN;

		if (percentile == 100) {
			// Return the exact maximum value for the 100th percentile
			return sorted_list.get(size - 1);
		} else if (percentile == 0) {
			// Return the exact minimum value for the 0th percentile
			return sorted_list.get(0);
		}

		// Use nearest rank method to calculate the percentile
		double rank = percentile / 100 * (size - 1);
		int lowerIndex = (int) Math.floor(rank);
		int upperIndex = (int) Math.ceil(rank);

		if (lowerIndex == upperIndex)
		{
			return sorted_list.get(lowerIndex);
		}
		else
		{
			// Linear interpolation between the two surrounding values
			double lowerValue = sorted_list.get(lowerIndex);
			double upperValue = sorted_list.get(upperIndex);
			return lowerValue + (rank - lowerIndex) * (upperValue - lowerValue);
		}
	}

	/**
	 * Converts an object to a number. The object can be a Number or a String.
	 * @param o The object
	 * @return The number, or <tt>null</tt> if the object cannot be converted
	 */
	protected static Double toNumber(Object o)
	{
		if (o instanceof Number)
		{
			return ((Number) o).doubleValue();
		}
		if (o instanceof String)
		{
			try
			{
				return Double.parseDouble((String) o);
			}
			catch (NumberFormatException e)
			{
				return null;
			}
		}
		return null;
	}
}
