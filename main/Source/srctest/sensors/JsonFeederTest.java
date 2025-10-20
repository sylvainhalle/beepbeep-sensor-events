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
package sensors;

import static org.junit.Assert.*;

import org.junit.Test;

import ca.uqac.lif.cep.Pullable;
import ca.uqac.lif.json.JsonElement;
import sensors.nears.JsonFeeder;

/**
 * Unit tests for {@link JsonFeeder}.
 */
public class JsonFeederTest
{
	@Test
	public void test1()
	{
		JsonFeeder f = new JsonFeeder(JsonFeederTest.class.getResourceAsStream("data/sample.json"));
		Pullable p = f.getPullableOutput();
		int pull_cnt = 0;
		while (p.hasNext())
		{
			Object o = p.pull();
			assertTrue(o instanceof JsonElement);
			pull_cnt++;
		}
		assertEquals(3, pull_cnt);
	}
}
