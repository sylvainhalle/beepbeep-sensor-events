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

import java.io.PrintStream;
import java.util.Queue;

import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.io.Print;

/**
 * A pretty-printer that displays a sequence of HTML snippets in a page, adding
 * hyperlinks to move forward and backward in the sequence of snippets.
 * The snippets are separated by a <tt>&lt;div&gt;</tt> element that spans a very
 * large vertical area, to give the impression that the page "flicks" between
 * each snippet when clicking on the links.
 * 
 * @author Sylvain Hallé
 */
public class HtmlPrint extends Print
{
	protected int m_indexCounter = 0;

	public HtmlPrint(PrintStream ps)
	{
		super(ps);
		setSeparator("<div style=\"height:1080px;clear:both\">&nbsp;</div>");
		ps.println("<!DOCTYPE html>");
		ps.println("<html><body>");
		ps.println("<a href=\"#step-0\">&gt;</a>");
	}

	@Override
	protected void afterSeparator(PrintStream ps)
	{
		m_indexCounter++;
		ps.println("<a href=\"#step-" + (m_indexCounter - 1) + "\">&lt;</a>&nbsp;");
		ps.println("<a href=\"#step-" + (m_indexCounter + 1) + "\">&gt;</a>");
		ps.println("<a name=\"step-" + m_indexCounter + "\"></a>");
	}

	@Override
	protected boolean onEndOfTrace(Queue<Object[]> outputs) throws ProcessorException
	{
		m_out.println("</html>");
		return false;
	}
}
