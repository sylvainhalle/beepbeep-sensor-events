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
package nears;

import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.fs.HardDisk;

/**
 * A {@link HardDisk} that is open on the local folder where the data files for
 * the examples reside. This avoids repeating the hard-coded path in each
 * individual code example.
 */
public class LogRepository extends HardDisk
{
	/**
	 * The folder name where the data files are located. Make sure to change this
	 * to the actual path on your computer!
	 */
	protected static final String FOLDER = "/home/sylvain/sensor-data";
	
	/**
	 * The name of the (optional) sub-folder of the root data folder.
	 */
	protected final String m_subFolder;
	
	public LogRepository() throws FileSystemException
	{
		this("");
	}
	
	public LogRepository(String sub_folder) throws FileSystemException
	{
		super(FOLDER);
		m_subFolder = sub_folder;
	}
	
	/**
	 * Gets the name of the sub-folder of the root data folder.
	 * @return The sub-folder
	 */
	public String getSubFolder()
	{
		return m_subFolder;
	}
	
	@Override
	public LogRepository open() throws FileSystemException
	{
		super.open();
		chdir(m_subFolder);
		return this;
	}
}
