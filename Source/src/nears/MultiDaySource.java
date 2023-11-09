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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ca.uqac.lif.cep.tmf.Splice;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;

/**
 * Source feeding events from a range of files, each containing JSON events for
 * a single day. The source provides these events as if each of these separate
 * files were concatenated as a single file.
 * @author Sylvain Hallé
 */
public class MultiDaySource extends Splice
{
	/**
	 * Creates a new multi-day source, specifying the range of days to read from
	 * a given folder. The source will sequentially feed events from files
	 * names <tt>x.json</tt> through <tt>y.json</tt>, where <tt>x</tt> is the
	 * number of the first day and <tt>x</tt> is the number of the last day. 
	 * @param fs A {@link FileSystem} instance open on the folder where the JSON
	 * files to read reside
	 * @param first_day The number of the first day of the range to read
	 * @param last_day The number of the last day of the range to read
	 */
	public MultiDaySource(FileSystem fs, int first_day, int last_day)
	{
		super(getFeeders(fs, first_day, last_day));
	}
	
	/**
	 * Creates a new multi-day source by reading all events from a given folder.
	 * The source assumes the first file is called {@code 1.json} and
	 * iterates by incrementing the filename until no file of a given number is
	 * found.
	 * @param fs A {@link FileSystem} instance open on the folder where the JSON
	 * files to read reside
	 */
	public MultiDaySource(FileSystem fs)
	{
		this(fs, 1, -1);
	}
	
	/**
	 * Gets the array of {@link JsonLineFeeder} corresponding to each of the
	 * files to read from. This method is used by the constructor of this class.
	 * @param fs A {@link FileSystem} instance open on the folder where the JSON
	 * files to read reside
	 * @param first_day The number of the first day of the range to read
	 * @param last_day The number of the last day of the range to read
	 * @return The array of line feeders
	 */
	protected static JsonLineFeeder[] getFeeders(FileSystem fs, int first_day, int last_day)
	{
		List<JsonLineFeeder> l_feeders = new ArrayList<JsonLineFeeder>();
		for (int i = first_day; i <= last_day || last_day < 0; i++)
		{
			try
			{
				InputStream is = fs.readFrom(i + ".json");
				l_feeders.add(new JsonLineFeeder(is));
			}
			catch (FileSystemException e)
			{
				if (last_day > 0)
				{
					// Show error only if a file was expected
					e.printStackTrace();
				}
				else
				{
					break;
				}
			}
		}
		JsonLineFeeder[] feeders = new JsonLineFeeder[l_feeders.size()];
		l_feeders.toArray(feeders);
		return feeders;
	}
}
