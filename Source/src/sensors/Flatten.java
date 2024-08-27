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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ca.uqac.lif.cep.functions.UnaryFunction;

/**
 * Flattens a nested collection of objects into a single set of elementary
 * objects.
 */
@SuppressWarnings("rawtypes")
public class Flatten extends UnaryFunction<Collection,Collection>
{
	/**
	 * A single publicly visible instance of the function.
	 */
	public static final Flatten instance = new Flatten();
	
	/**
	 * Creates an instance of the function.
	 */
	protected Flatten()
	{
		super(Collection.class, Collection.class);
	}

	@Override
	public Collection getValue(Collection x)
	{
		Set<Object> set = new HashSet<Object>();
		flatten(set, x);
		return set;
	}
	
	/**
	 * Flattens a nested collection.
	 * @param set The set where the elements of the collection are to be
	 * collected.
	 * @param col The collection of objects
	 */
	protected static void flatten(Set<Object> set, Collection<?> col)
	{
		for (Object o : col)
		{
			if (o instanceof Collection)
			{
				flatten(set, (Collection<?>) o);
			}
			else
			{
				set.add(o);
			}
		}
	}
	
	@Override
	public Flatten duplicate(boolean with_state)
	{
		return this;
	}
}
