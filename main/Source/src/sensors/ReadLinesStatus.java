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
import java.util.Scanner;

import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.tmf.Source;

/**
 * A processor that can read multiple files one after the other, and
 * pretty-prints the status of lines read into an output stream. This processor
 * only works on files, not on other streams (such as <tt>stdin</tt>), since it
 * first scans the file in order to count the total number of lines it
 * contains.
 * 
 * @author Sylvain Hallé
 */
public class ReadLinesStatus extends Source
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
	 * The name of the file(s) to read from.
	 */
	protected final String[] m_filenames;

	/**
	 * The index of the current file being read.
	 */
	protected int m_currentFileIndex;

	/**
	 * The scanner used to read the current file.
	 */
	protected Scanner m_scanner;

	/**
	 * Creates a new instance of the processor.
	 * @param filenames The name(s) of the local file(s) to read from
	 * @param ps The print stream where to display the progress bar
	 * @throws IOException Thrown if the file does not exist
	 */
	public ReadLinesStatus(PrintStream ps, String ... filenames) throws IOException
	{
		super(1);
		m_filenames = filenames;
		long total_lines = 0;
		for (String filename : filenames)
		{
			total_lines += Files.lines(Paths.get(filename)).count();
		}
		m_totalLines = total_lines;
		m_printStream = ps;
		m_updateInterval = m_totalLines / 1000;
		m_scanner = null;
		m_currentFileIndex = -1;
	}

	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		if (m_scanner == null || !m_scanner.hasNextLine())
		{
			if (!nextFile())
			{
				if (m_printStream != null)
				{
					m_printStream.print("\r\033[K"); // Flush line
				}
				return false;
			}
		}
		String line = m_scanner.nextLine();
		if (m_inputCount % m_updateInterval == 0)
		{
			double progress = (double) m_inputCount / (double) m_totalLines;
			printProgressBar(progress);
		}
		m_inputCount++;
		outputs.add(new Object[] {line});
		return true;
	}

	protected boolean nextFile()
	{
		m_currentFileIndex++;
		System.out.println(m_inputCount + "," + m_currentFileIndex);
		if (m_currentFileIndex >= m_filenames.length)
		{
			return false;
		}
		try
		{
			if (m_scanner != null)
			{
				m_scanner.close();
			}
			m_scanner = new Scanner(new File(m_filenames[m_currentFileIndex]));
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Pretty-prints a progress bar for the current progress value.
	 * @param progress A value between 0 and 1 indicating progression
	 */
	protected void printProgressBar(double progress)
	{
		if (m_printStream == null)
		{
			return;
		}
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

	@Override
	public ReadLinesStatus duplicate(boolean with_state)
	{
		if (with_state)
		{
			throw new UnsupportedOperationException("Cannot duplicate with state");
		}
		try
		{
			return new ReadLinesStatus(m_printStream, m_filenames);
		}
		catch (IOException e)
		{
			throw new ProcessorException(e);
		}
	}
}
