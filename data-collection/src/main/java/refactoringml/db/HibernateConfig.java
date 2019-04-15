package refactoringml.db;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import java.util.Properties;

public class HibernateConfig {

	private static SessionFactory sessionFactory;

	private void build() {

		Configuration configuration = new Configuration();

		Properties settings = new Properties();
		settings.put(Environment.DRIVER, "com.mysql.cj.jdbc.Driver");
		settings.put(Environment.URL, "jdbc:mysql://localhost:3306/refactoring2?useSSL=false");
		settings.put(Environment.USER, "root");
		settings.put(Environment.PASS, "");
		settings.put(Environment.DIALECT, "org.hibernate.dialect.MySQL5Dialect");
		settings.put(Environment.SHOW_SQL, "true");
		settings.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
		settings.put(Environment.HBM2DDL_AUTO, "update");

		settings.put("hibernate.connection.provider_class", "org.hibernate.connection.C3P0ConnectionProvider");
		settings.put("hibernate.c3p0.acquire_increment", 1);
		settings.put("hibernate.c3p0.idle_test_period", 60);
		settings.put("hibernate.c3p0.min_size", 1);
		settings.put("hibernate.c3p0.max_size", 2); // 1 connection is actually enough...
		settings.put("hibernate.c3p0.max_statements", 50);
		settings.put("hibernate.c3p0.timeout", 0);
		settings.put("hibernate.c3p0.acquireRetryAttempts", 1);
		settings.put("hibernate.c3p0.acquireRetryDelay", 250);

		configuration.setProperties(settings);

		configuration.addAnnotatedClass(Yes.class);
		configuration.addAnnotatedClass(No.class);
		configuration.addAnnotatedClass(Project.class);

		ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySettings(configuration.getProperties()).build();

		sessionFactory = configuration.buildSessionFactory(serviceRegistry);
	}

	public SessionFactory getSessionFactory() {
		if(sessionFactory == null)
			build();

		return sessionFactory;
	}
}
