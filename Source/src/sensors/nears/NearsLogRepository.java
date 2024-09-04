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
package sensors.nears;

import sensors.LogRepository;

/**
 * A file system that is open directly on the local folder where the data
 * files for the NEARS dataset reside.
 */
public class NearsLogRepository extends LogRepository
{
	/**
	 * The folder name where the data files are located. Make sure to change this
	 * to the actual path on your computer!
	 */
	protected static final String FOLDER = "data/nears";
	
	public NearsLogRepository()
	{
		super(FOLDER);
	}
	
	public NearsLogRepository(String sub_folder)
	{
		super(FOLDER + "/" + sub_folder);
	}
}
