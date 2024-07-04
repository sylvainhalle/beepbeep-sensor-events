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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import sensors.DateToTimestamp;

/**
 * Unit tests for {@link DateToTimestamp}.
 */
public class DateToTimestampTest
{
	@Test
	public void test1()
	{
		long ts1 = DateToTimestamp.getTimestamp("2021-09-02T00:02:04.786-05:00");
		long ts2 = DateToTimestamp.getTimestamp("2021-07-02T00:02:37.615Z");
		System.out.println(ts1);
		assertTrue(ts2 < ts1);
	}
}
