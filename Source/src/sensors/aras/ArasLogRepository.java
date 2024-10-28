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
package sensors.aras;

import java.util.ArrayList;
import java.util.List;

import ca.uqac.lif.fs.FileSystemException;
import sensors.LogRepository;

/**
 * A file system that is open directly on the local folder where the data file
 * for the Casas "HH" datasets reside.
 */
public class ArasLogRepository extends LogRepository
{
	/**
	 * The (root) folder name where the data file is located.
	 */
	protected static final String FOLDER = "data/aras";

	public ArasLogRepository(String sub_folder)
	{
		super(FOLDER + "/" + sub_folder);
	}
	
	public String[] getLogFiles() throws FileSystemException
	{
		List<String> list = new ArrayList<String>();
		for (int i = 1; i < 50; i++)
		{
			String filename = "DAY_" + i + ".txt";
			if (!this.isFile(filename))
			{
				break;
			}
			list.add(m_root + "/" + filename);
		}
		return list.toArray(new String[list.size()]);
	}
}
