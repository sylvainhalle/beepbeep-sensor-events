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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.SynchronousProcessor;

/**
 * Unpacks a map into its values, each being output as a
 * separate event.
 * 
 * @author Sylvain Hallé
 */
public class UnpackMap extends SynchronousProcessor
{
	/**
	 * A comparator that can compare any two objects.
	 */
	protected static final GenericComparator s_comparator = new GenericComparator();
	
	/**
	 * Creates a new instance of the processor.
	 */
	public UnpackMap()
	{
		super(1, 1);
	}
	
	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		Object o = inputs[0];
		if (!(o instanceof Map))
		{
			throw new ProcessorException("Expected a map, got " + o.getClass().getSimpleName());
		}
		Map<?,?> map = (Map<?,?>) o;
		List<Object> keys = new ArrayList<>();
		keys.addAll(map.keySet());
		Collections.sort(keys, s_comparator);
		for (Object key : keys)
		{
			Object value = map.get(key);
			outputs.add(new Object[] { value });
		}
		return true;
	}

	@Override
	public UnpackMap duplicate(boolean with_state)
	{
		return new UnpackMap();
	}
	
	/**
	 * A generic comparator that can compare any two objects. It does so by
	 * applying the following rules:
	 * <ul>
	 * <li>If both objects are null, they are considered equal</li>
	 * <li>If one object is null and the other is not, the null object is
	 * considered smaller</li>
	 * <li>If both objects are instances of {@link Comparable}, they are
	 * compared using their natural ordering</li>
	 * <li>If both objects are instances of {@link String}, they are compared
	 * lexicographically</li>
	 * <li>If both objects are instances of {@link Number}, they are compared
	 * numerically</li>
	 * <li>Otherwise, the objects are considered equal</li>
	 * </ul>
	 */
	public static class GenericComparator implements Comparator<Object>
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public int compare(Object o1, Object o2)
		{
			if (o1 == null && o2 == null)
			{
				return 0;
			}
			if (o1 == null && o2 != null)
			{
				return -1;
			}
			if (o1 != null && o2 == null)
			{
				return 1;
			}
			// Both are not null
			if (o1 instanceof Comparable && o2 instanceof Comparable)
			{
				return ((Comparable) o1).compareTo(o2);
			}
			if (o1 instanceof String && o2 instanceof String)
			{
				return ((String) o1).compareTo((String) o2);
			}
			if (o1 instanceof Number && o2 instanceof Number)
			{
				return Double.compare(((Number) o1).doubleValue(), ((Number) o2).doubleValue());
			}
			return 0;
		}
	}
}
