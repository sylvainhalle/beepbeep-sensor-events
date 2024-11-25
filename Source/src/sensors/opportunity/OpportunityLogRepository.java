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
package sensors.opportunity;

import sensors.LogRepository;

/**
 * A file system that is open directly on the local folder where the data files
 * for the Opportunity datasets reside.
 */
public class OpportunityLogRepository extends LogRepository
{
	/**
	 * The (root) folder name where the data file is located.
	 */
	protected static final String FOLDER = "data/opportunity/dataset";

	public OpportunityLogRepository()
	{
		super(FOLDER);
	}
}
