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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.GregorianCalendar;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.Pullable;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.fs.FileSystemException;
import sensors.nears.NearsJsonFormat;
import sensors.nears.NearsLogRepository;

/**
 * Utility program that splits an input file into separate files for each
 * day, numbered sequentially (i.e. <tt>1.json</tt>, <tt>2.json</tt>, etc.
 * The files are placed in a folder corresponding to the number of the original
 * JSON file.
 * 
 * @author Sylvain Hallé
 */
public class DaySplit
{
	/* The adapter for the event format. */
	protected static EventFormat format = new NearsJsonFormat();
	
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new NearsLogRepository("0034");
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input file and output directory. */
		String file_number = "0034";
		fs.open();
		InputStream is = fs.readFrom("nears-hub-" + file_number + "-sorted.json");
		fs.mkdir(file_number);
		fs.chdir(file_number);
		
		/* Read file line by line, and start a new file every time the day of
		 * the year in the timestamp changes. */
		Calendar cal = new GregorianCalendar();
		Processor feeder = format.getFeeder(is);
		int cnt = 0;
		int cur_day = -1;
		PrintStream os = null;
		Pullable p = feeder.getPullableOutput();
		Function ts_function = format.timestamp();
		while (p.hasNext())
		{
			Object event = p.pull();
			cal.setTimeInMillis((Long) EventFormat.evaluateUnary(ts_function, event));
			int day = cal.get(Calendar.DAY_OF_YEAR);
			if (cur_day != day)
			{
				if (os != null)
				{
					os.close();
				}
				cnt++;
				os = new PrintStream(fs.writeTo(cnt + format.getExtension()));
				cur_day = day;
			}
			os.println(event);
		}
		System.out.println("Wrote " + cnt + " files");
		
		/* Clean up. */
		if (os != null)
		{
			os.close();
		}
		is.close();
		fs.close();
	}

}
