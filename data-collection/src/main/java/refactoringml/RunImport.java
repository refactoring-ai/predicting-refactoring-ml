package refactoringml;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RunImport {


	public static void main(String[] args) throws IOException, TimeoutException {

		String host = "localhost";
		String file = "projects-final.csv";

		if(args!=null && args.length > 0){
			host = args[0];
			file = args[1];
		}

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);

		try (Connection connection = factory.newConnection();
		     Channel channel = connection.createChannel()) {

			channel.queueDeclare(RunQueue.QUEUE_NAME, false, false, false, null);

			List<String> lines = FileUtils.readLines(new File(file));
			for(String line : lines) {
				String message = line;
				channel.basicPublish("", RunQueue.QUEUE_NAME, null, message.getBytes());
				System.out.println(" [x] Sent '" + message + "'");
			}

		}

	}
}
