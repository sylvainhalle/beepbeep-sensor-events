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

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A print stream that automatically manages indentation of text lines.
 */
public class PrettyPrintStream extends PrintStream
{
	/**
	 * Interface implemented by objects that can print themselves to a
	 * {@link PrettyPrintStream}.
	 */
	public static interface PrettyPrintable
	{
		/**
		 * Prints the current object to a {@link PrettyPrintStream}.
		 * @param ps The stream to print the object to
		 */
		public void print(PrettyPrintStream ps);
		
		/**
		 * Determines the number of rows used in the printing of an object.
		 * @return The number of rows
		 */
		public int countRows();
	}
	
	/**
	 * The characters to add for each level of indentation in the stream.
	 */
	public static String INDENT = "  ";
	
	/**
	 * The level of indentation to add to the start of each line.
	 */
	protected String m_indent;
	
	/**
	 * A flag indicating if the stream is at the start of a new line.
	 */
	protected boolean m_newLine;
	
	public PrettyPrintStream(OutputStream out)
	{
		super(out);
		m_indent = "";
		m_newLine = true;
	}

	/**
	 * Increases the level of indentation by one.
	 */
	public void indent()
	{
		m_indent += INDENT;
	}
	
	/**
	 * Decreases the level of indentation by one.
	 */
	public void outdent()
	{
		if (!m_indent.isEmpty())
		{
			m_indent = m_indent.substring(0, m_indent.length() - INDENT.length());
		}
	}

	@Override
	public void print(String s)
	{
		if (m_newLine)
		{
			super.print(m_indent);
		}
		super.print(s);
		m_newLine = false;
	}
	
	@Override
	public void println(String s)
	{
		if (m_newLine)
		{
			super.print(m_indent);
		}
		super.println(s);
		m_newLine = true;
	}
	

}
