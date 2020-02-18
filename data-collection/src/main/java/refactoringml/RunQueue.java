package refactoringml;

import com.rabbitmq.client.*;
import org.apache.log4j.Logger;
import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RunQueue {

	private static final Logger log = Logger.getLogger(RunQueue.class);
	public final static String QUEUE_NAME = "refactoring";
	private final Database db;
	private String storagePath;
	private String host;
	private final int threshold;
	private final boolean storeFullSourceCode;

	public RunQueue(String host, String url, String user, String pwd, String storagePath, int threshold, boolean storeFullSourceCode) {
		this.host = host;
		this.storagePath = storagePath;
		this.threshold = threshold;
		this.storeFullSourceCode = storeFullSourceCode;
		db = new Database(new HibernateConfig().getSessionFactory(url, user, pwd));
	}

	public static void main(String[] args) throws Exception {
		// we gotta wait a few minutes before the queue is up and the db is up...
		// Docker stuf...
		Thread.sleep(1000 * 60 * 2);

		String queueHost = "localhost";
		String url = "jdbc:mysql://localhost:3306/refactoringtest?useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC";
		String user = "root";
		String pwd = "";
		int threshold = 50;
		boolean storeFullSourceCode = true;
		//TODO: remove references to mauricios desktop, e.g. make this an argument
		String storagePath = "/Users/mauricioaniche/Desktop/results/";

		boolean test = false;
		if(!test) {
			queueHost = System.getenv("QUEUE_HOST");
			url = System.getenv("REF_URL");
			user = System.getenv("REF_USER");
			pwd = System.getenv("REF_DBPWD");
			threshold = Integer.parseInt(System.getenv("THRESHOLD"));
			storeFullSourceCode = Boolean.parseBoolean(System.getenv("STORE_FILES"));
			storagePath = System.getenv("STORAGE_PATH");
		}

		log.info("Queue configuration:\n" +
				"\tQueue host: " + queueHost + "\n" +
				"\tURL: " + url + "\n" +
				"\tUser: " + user + "\n" +
				"\tThreshold: " + threshold + "\n" +
				"\tStore full code?: " + storeFullSourceCode + "\n" +
				"\tStorage path: " + storagePath);

		new RunQueue(queueHost, url, user, pwd, storagePath, threshold, storeFullSourceCode).run();
	}

	private void run() throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);

		while(true) {
			log.debug("Waiting for new element from the queue...");
			try (Connection connection = factory.newConnection();
			     Channel channel = connection.createChannel()) {

				GetResponse chResponse = channel.basicGet(QUEUE_NAME, true);
				if (chResponse != null) {
					byte[] body = chResponse.getBody();
					String message = new String(body);

					log.debug("Got new element from queue: " + message);

					doWork(message);
				}
			}
		}
	}

	private void doWork(String message) {
		String[] msg = message.split(",");

		String dataset = msg[2];
		String gitUrl = msg[1];

		log.debug("Mine dataset: " + dataset + " with git url: " + gitUrl);
		try {
			new App(dataset, gitUrl, storagePath, threshold, db, storeFullSourceCode).run();
		} catch(Exception e) {
			log.error("Error while processing repository: " + gitUrl, e);
		}
	}
}