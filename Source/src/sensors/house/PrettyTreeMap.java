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
package sensors.house;

import java.util.Map;
import java.util.TreeMap;

import sensors.PrettyPrintStream;
import sensors.PrettyPrintStream.PrettyPrintable;

public class PrettyTreeMap<V> extends TreeMap<String,V> implements PrettyPrintable
{
	private static final long serialVersionUID = 1L;

	public void print(PrettyPrintStream ps)
	{
		ps.println("<table style=\"font-size:8pt;font-family:sans-serif;" + getBackground() + "\">");
		for (Map.Entry<String,V> e : entrySet())
		{
			ps.println("<tr style=\"border-top:solid 1px\"><th>" + e.getKey() + "</th>");
			ps.println("<td>");
			if (e.getValue() instanceof PrettyPrintable)
			{
				((PrettyPrintable) e.getValue()).print(ps);
			}
			else
			{
				ps.print(e.getValue());
			}
			ps.println("</td></tr>");
		}
		ps.println("</table>");
	}
	
	@Override
	public int countRows()
	{
		int rows = 1;
		for (Map.Entry<String,V> e : entrySet())
		{
			if (e.getValue() instanceof PrettyPrintable)
			{
				rows += ((PrettyPrintable) e.getValue()).countRows();
			}
		}
		return rows;
	}
	
	protected String getBackground()
	{
		return "background: rgba(0.9,0.9,0.9,0.15)";
	}
}		