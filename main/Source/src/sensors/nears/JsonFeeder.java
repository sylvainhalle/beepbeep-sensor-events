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
package sensors.nears;

import java.io.InputStream;
import java.util.Queue;
import java.util.Scanner;

import ca.uqac.lif.cep.ProcessorException;
import ca.uqac.lif.cep.tmf.Source;
import ca.uqac.lif.json.JsonElement;
import ca.uqac.lif.json.JsonParser;
import ca.uqac.lif.json.JsonParser.JsonParseException;

/**
 * A source of JSON events taking its data from a JSON file. This source
 * exploits the particular structure of specific JSON files, structured
 * as follows:
 * <pre>
 * <span style="background:cyan">[{</span>
 *   first event
 * },
 * <span style="background:red">{</span>
 *   second event
 * <span style="background:red">},</span>
 * ...
 * {
 *   last event
 * <span style="background:cyan">}]</span>
 * </pre>
 * It looks for the cyan lines to identify the start and end of the files,
 * and for the red lines to identify the boundaries of each event. Each block
 * of lines delimited by these markers is then parsed separately.
 * It will not correctly parse arbitrary JSON files.
 * @author Sylvain Hallé
 */
public class JsonFeeder extends Source
{
	/**
   * The parser used to parse the elements. All instances of the
   * processor share the same parser.
   */
  /*@ non_null @*/ protected static final JsonParser s_parser = new JsonParser();
  
  /**
   * The input stream to read JSON content from.
   */
  /*@ non_null @*/ protected final InputStream m_is;
	
  /**
   * The scanner pulling lines from the input stream.
   */
  /*@ non_null @*/ protected final Scanner m_scanner;
	
  /**
   * A string builder accumulating the text lines of the current JSON element.
   * This builder is cleared between each output event.
   */
  /*@ non_null @*/ protected final StringBuilder m_message;
	
  /**
   * Creates a new JSON feeder.
   * @param is The input stream to read JSON content from
   */
	public JsonFeeder(/*@ non_null @*/ InputStream is)
	{
		super(1);
		m_is = is;
		m_scanner = new Scanner(m_is);
		m_message = new StringBuilder();
	}

	@Override
	protected boolean compute(Object[] inputs, Queue<Object[]> outputs)
	{
		while (m_scanner.hasNextLine())
		{
			String line = m_scanner.nextLine();
			if (line.startsWith("[{"))
			{
				// First line
				m_message.append("{");
				continue;
			}
			if (line.startsWith("}"))
			{
				// End of object
				m_message.append("}");
				try
				{
					JsonElement j = s_parser.parse(m_message.toString());
					outputs.add(new Object[] {j});
					m_message.setLength(0);
				}
				catch (JsonParseException e)
				{
					throw new ProcessorException(e);
				}
				if (line.endsWith("]"))
				{
					// End of file
					return false;
				}
				return true;
			}
			m_message.append(line);
			return true;
		}
		return false;
	}

	@Override
	public JsonFeeder duplicate(boolean with_state)
	{
		throw new UnsupportedOperationException("This source cannot be duplicated");
	}
	
	@Override
	public void stop()
	{
		m_scanner.close();
	}
}
