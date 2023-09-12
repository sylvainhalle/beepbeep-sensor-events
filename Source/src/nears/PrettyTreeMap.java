package nears;

import java.util.Map;
import java.util.TreeMap;

import nears.PrettyPrintStream.PrettyPrintable;

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