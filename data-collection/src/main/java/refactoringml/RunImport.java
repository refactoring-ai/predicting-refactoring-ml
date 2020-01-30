package refactoringml;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RunImport {


	public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {

		// we gotta wait 2 minutes before the queue is up...
		// Docker stuff...
		Thread.sleep(1000 * 60 * 1);

		String host = System.getenv("QUEUE_HOST");
		String file = System.getenv("FILE_TO_IMPORT");

		System.out.println("Host: " + host);
		System.out.println("File: " + file);

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);

		try (Connection connection = factory.newConnection();
		     Channel channel = connection.createChannel()) {

			// the first true makes the queue durable, i.e., it survives even if rabbitmq restarts
			channel.queueDeclare(RunQueue.QUEUE_NAME, true, false, false, null);

			List<String> lines = FileUtils.readLines(new File(file));
			for(String line : lines) {
				String message = line;
				channel.basicPublish("", RunQueue.QUEUE_NAME, null, message.getBytes());
				System.out.println(" [x] Sent '" + message + "'");
			}

		}

	}
}
