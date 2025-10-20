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

import java.io.InputStream;

import ca.uqac.lif.cep.tmf.Source;
import ca.uqac.lif.fs.FileSystem;
import sensors.MultiDaySource;

public class NearsMultiDaySource extends MultiDaySource
{
	public NearsMultiDaySource(FileSystem fs, int first_day, int last_day)
	{
		super(fs, first_day, last_day, "json", new JsonFeederFactory());
	}
	
	public NearsMultiDaySource(FileSystem fs)
	{
		super(fs, "json", new JsonFeederFactory());
	}
	
	protected static class JsonFeederFactory implements FeederFactory
	{
		@Override
		public Source getFeeder(InputStream is)
		{
			return new JsonLineFeeder(is);
		}
	}
}
