package refactoringml;

import com.rabbitmq.client.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RunImport {


	public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {

		// we gotta wait a few minutes before the queue is up...
		// Docker stuff...
		Thread.sleep(1000 * 60 * 1);

		String host = System.getenv("QUEUE_HOST");
		String file = System.getenv("FILE_TO_IMPORT");

		System.out.println("Host: " + host);
		System.out.println("File: " + file);

		if(file.trim().isEmpty()) {
			System.out.println("No files to import");
			System.exit(0);
		}

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);

		try (Connection connection = factory.newConnection();
		     Channel channel = connection.createChannel()) {

			// the first true makes the queue durable, i.e., it survives even if rabbitmq restarts
			channel.queueDeclare(RunQueue.QUEUE_NAME, true, false, false, null);

			List<String> lines = FileUtils.readLines(new File(file));
			for(String line : lines) {
				String message = line;
				channel.basicPublish("", RunQueue.QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
				System.out.println(" [x] Sent '" + message + "'");
			}

		}

	}
}
