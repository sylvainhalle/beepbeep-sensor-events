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

import ca.uqac.lif.cep.functions.Function;

/**
 * Extension to the {@link EventFormat} interface for datasets where activities
 * are labeled.
 * 
 * @author Sylvain Hallé
 */
public interface LabeledEventFormat extends EventFormat
{
	/**
	 * Returns the BeepBeep {@link Function} that fetches the activity
	 * of a sensor event. 
	 * @return The activity function
	 */
	/*@ non_null @*/ public Function activityString();
}
