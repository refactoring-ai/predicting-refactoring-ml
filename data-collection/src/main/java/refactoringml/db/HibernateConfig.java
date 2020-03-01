package refactoringml.db;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import refactoringml.PMTrackerDatabase;
import refactoringml.ProcessMetricTracker;

import java.util.Properties;

public class HibernateConfig {

	private static SessionFactory sessionFactory;

	public SessionFactory getSessionFactory(String url, String user, String pwd) {
		return getSessionFactory(url, user, pwd, false);
	}

	public SessionFactory getSessionFactory(String url, String user, String pwd, boolean drop) {
		if(sessionFactory == null) {
			Configuration configuration = new Configuration();

			Properties settings = new Properties();
			settings.put(Environment.DRIVER, "com.mysql.cj.jdbc.Driver");
			settings.put(Environment.URL, url);
			settings.put(Environment.USER, user);
			settings.put(Environment.PASS, pwd);
			settings.put(Environment.DIALECT, "org.hibernate.dialect.MySQL5InnoDBDialect");
			settings.put(Environment.SHOW_SQL, "false");

			if(drop)
				settings.put(Environment.HBM2DDL_AUTO, "create-drop");
			else
				settings.put(Environment.HBM2DDL_AUTO, "update");

			settings.put("hibernate.connection.provider_class", "org.hibernate.connection.C3P0ConnectionProvider");
			settings.put("hibernate.c3p0.acquire_increment", 1);
			settings.put("hibernate.c3p0.idle_test_period", 60);
			settings.put("hibernate.c3p0.min_size", 1);
			settings.put("hibernate.c3p0.max_size", 5);
			settings.put("hibernate.c3p0.max_statements", 50);

			settings.put("hibernate.c3p0.timeout", 300);
			settings.put("hibernate.c3p0.maxConnectionAge", 3600);

			settings.put("hibernate.c3p0.maxIdleTimeExcessConnections", 3600);

			settings.put("hibernate.c3p0.acquireRetryAttempts", 10);
			settings.put("hibernate.c3p0.acquireRetryDelay", 60);

			settings.put("hibernate.c3p0.preferredTestQuery", "SELECT 1");
			settings.put("hibernate.c3p0.testConnectionOnCheckout", true);

			settings.put("hibernate.c3p0.autoCommitOnClose", false);
			settings.put("hibernate.c3p0.unreturnedConnectionTimeout", 300);

			configuration.setProperties(settings);

			configuration.addAnnotatedClass(RefactoringCommit.class);
			configuration.addAnnotatedClass(StableCommit.class);
			configuration.addAnnotatedClass(Project.class);

			//features of Instance for DB normalization
			configuration.addAnnotatedClass(CommitMetaData.class);
			configuration.addAnnotatedClass(ClassMetric.class);
			configuration.addAnnotatedClass(MethodMetric.class);
			configuration.addAnnotatedClass(VariableMetric.class);
			configuration.addAnnotatedClass(FieldMetric.class);
			configuration.addAnnotatedClass(ProcessMetrics.class);

			//PMDatabase
			configuration.addAnnotatedClass(ProcessMetricTracker.class);

			ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
					.applySettings(configuration.getProperties()).build();

			sessionFactory = configuration.buildSessionFactory(serviceRegistry);
		}

		return sessionFactory;
	}
}
