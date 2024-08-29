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
package sensors;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Queue;

import ca.uqac.lif.cep.io.ReadLines;

/**
 * Variant of {@link ReadLines} that pretty-prints the status of lines read
 * into an output stream. This processor only works on files, not on other
 * streams (such as <tt>stdin</tt>), since it first scans the file in order
 * to count the total number of lines it contains.
 * 
 * @author Sylvain Hallé
 */
public class ReadLinesStatus extends ReadLines
{
	/**
	 * The total number of lines in the input file to read.
	 */
	protected final long m_totalLines;

	/**
	 * The print stream where to display the progress bar.
	 */
	protected final PrintStream m_printStream;

	/**
	 * The interval (in number of events) at which the progress bar is updated.
	 */
	protected final long m_updateInterval;

	/**
	 * Creates a new instance of the processor.
	 * @param filename The name of the local file to read from
	 * @param ps The print stream where to display the progress bar
	 * @throws IOException Thrown if the file does not exist
	 */
	public ReadLinesStatus(String filename, PrintStream ps) throws IOException
	{
		super(new File(filename));
		m_totalLines = Files.lines(Paths.get(filename)).count();
		m_printStream = ps;
		m_updateInterval = 1; //m_totalLines / 1000;
	}

	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		boolean b = super.compute(inputs, outputs);
		if (m_inputCount % m_updateInterval == 0)
		{
			double progress = (double) m_inputCount / (double) m_totalLines;
			printProgressBar(progress);
		}
		m_inputCount++;
		if (!b)
		{
			m_printStream.print("\r\033[K"); // Flush line
		}
		return b;
	}

	/**
	 * Pretty-prints a progress bar for the current progress value.
	 * @param progress A value between 0 and 1 indicating progression
	 */
	protected void printProgressBar(double progress)
	{
		int width = 30;  // Width of the progress bar
		int filled = (int) (progress * width);
		int unfilled = width - filled;
		StringBuilder bar = new StringBuilder("[");
		for (int i = 0; i < filled; i++)
		{
			bar.append("=");
		}
		for (int i = 0; i < unfilled; i++)
		{
			bar.append(" ");
		}
		bar.append("]");
		m_printStream.print("\r" + bar.toString() + " " + String.format("%d", (int) (progress * 100)) + "%");
		m_printStream.flush();
	}
}
