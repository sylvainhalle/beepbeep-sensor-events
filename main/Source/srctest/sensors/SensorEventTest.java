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

import java.util.Date;

import org.junit.Test;

import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.json.JsonMap;
import sensors.nears.NearsJsonFormat;

public class SensorEventTest
{
	NearsJsonFormat s_format = new NearsJsonFormat();
	
  @Test
  public void testNewEvent1()
  {
    JsonMap e = (JsonMap) NearsJsonFormat.newEvent("living", "tv", "dmof1", "2023-01-01T00:00:00.000Z", "temperature", 23.4);
    assertNotNull(e);
    assertEquals("living", s_format.locationString());
    assertEquals("23.40 °C", s_format.stateString());
  }
  
  @Test
  public void testNewEvent2()
  {
    Date d = s_format.parseDate("2023-01-01T00:00:00.000Z");
    JsonMap e = (JsonMap) NearsJsonFormat.newEvent("living", "tv", "dmof1", d.getTime(), "temperature", 23.4);
    assertNotNull(e);
    Function get_ts = s_format.timestamp();
    Object[] outs = new Object[1];
    get_ts.evaluate(new Object[] {e}, outs);
    assertEquals(d.getTime(), outs[0]);
  }
}
