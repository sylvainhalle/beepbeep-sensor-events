package sensors.casas;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import sensors.LogRepository;

/**
 * A file system that is open directly on the local folder where the data file
 * for the Casas dataset resides.
 */
public class CasasLogRepository extends LogRepository
{
	/**
	 * The folder name where the data file is located.
	 */
	protected static final String FOLDER = "casas-data";

	public CasasLogRepository()
	{
		super(FOLDER);
	}

	public CasasLogRepository(String sub_folder)
	{
		super(FOLDER + "/" + sub_folder);
	}

	public InputStream readPart(String file, String start, String end) throws FileNotFoundException
	{
		InputStream is = new FileInputStream(file);
		return is;
	}
}
