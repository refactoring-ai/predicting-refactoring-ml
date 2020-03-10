package refactoringml;

import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import static refactoringml.util.PropertiesUtils.getProperty;

public class RunQueue {
	private static final Logger log = LogManager.getLogger(RunQueue.class);
	public final static String QUEUE_NAME = "refactoring";
	private final Database db;
	private String storagePath;
	private String host;
	private final boolean storeFullSourceCode;

	public RunQueue(String host, String url, String user, String pwd, String storagePath, boolean storeFullSourceCode) {
		this.host = host;
		this.storagePath = storagePath;
		this.storeFullSourceCode = storeFullSourceCode;
		db = new Database(new HibernateConfig().getSessionFactory(url, user, pwd));
	}

	public static void main(String[] args) throws Exception {
		// we gotta wait a few minutes before the queue is up and the db is up...
		Thread.sleep(1000 * Long.parseLong(getProperty("queueWaitTime")));

		String queueHost = "localhost";
		String url = "jdbc:mysql://localhost:3306/refactoringtest?useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC";
		String user = "root";
		String pwd = "";
		boolean storeFullSourceCode = true;
		//TODO: remove references to mauricios desktop, e.g. make this an argument
		String storagePath = "/Users/mauricioaniche/Desktop/results/";

		boolean test = false;
		if (!test) {
			queueHost = System.getenv("QUEUE_HOST");
			url = System.getenv("REF_URL");
			user = System.getenv("REF_USER");
			pwd = System.getenv("REF_DBPWD");
			storeFullSourceCode = Boolean.parseBoolean(System.getenv("STORE_FILES"));
			storagePath = System.getenv("STORAGE_PATH");
		}

		log.info("Queue host: " + queueHost);
		log.info("URL: " + url);
		log.info("User: " + user);
		log.info("Store full code?: " + storeFullSourceCode);
		log.info("Storage path: " + storagePath);

		new RunQueue(queueHost, url, user, pwd, storagePath, storeFullSourceCode).run();
	}

	private void run() throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);

		while(true) {
			log.info("Fetching new element from the rabbitmq queue...");

			try (Connection connection = factory.newConnection();
				 Channel channel = connection.createChannel()) {

				GetResponse chResponse = channel.basicGet(QUEUE_NAME, true);
				if (chResponse != null) {
					byte[] body = chResponse.getBody();
					String message = new String(body);
					log.info("Got new element from rabbitmq queue: " + message);
					doWork(message);
					//force garbage collection to free some memory
					System.gc();
				}
			}
		}
	}

	private void doWork(String message) {
		String[] msg = message.split(",");

		String dataset = msg[2];
		String gitUrl = msg[1];

		log.info("Mine dataset: " + dataset + " with git url: " + gitUrl);
		try {
			new App(dataset, gitUrl, storagePath, db, storeFullSourceCode).run();
		} catch (Exception e) {
			log.error("Error while processing " + gitUrl, e);
		}
	}
}