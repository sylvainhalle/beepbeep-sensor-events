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
package sensors.orange4home;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import sensors.LogRepository;

/**
 * A file system that is open directly on the local folder where the data file
 * for the Orange4Home dataset resides.
 */
public class Orange4HomeLogRepository extends LogRepository
{
	/**
	 * The folder name where the data file is located.
	 */
	protected static final String FOLDER = "data/orange4home";

	public Orange4HomeLogRepository()
	{
		super(FOLDER);
	}

	public Orange4HomeLogRepository(String sub_folder)
	{
		super(FOLDER + "/" + sub_folder);
	}

	public InputStream readPart(String file, String start, String end) throws FileNotFoundException
	{
		InputStream is = new FileInputStream(file);
		return is;
	}
}
