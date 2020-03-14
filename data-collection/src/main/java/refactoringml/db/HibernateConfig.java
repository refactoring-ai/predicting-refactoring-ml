package refactoringml.db;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;
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
			settings.put("hibernate.c3p0.min_size", 1);
			settings.put("hibernate.c3p0.max_size", 5);
			settings.put("hibernate.c3p0.max_statements", 100);

			settings.put("hibernate.c3p0.numHelperThreads", 10);

			// this one should be here only for debug purposes
			// settings.put("hibernate.c3p0.debugUnreturnedConnectionStackTraces", true);
			// settings.put("hibernate.c3p0.unreturnedConnectionTimeout", 120);

			settings.put("hibernate.c3p0.timeout", 0);
			settings.put("hibernate.c3p0.maxConnectionAge", 0);

			settings.put("hibernate.c3p0.maxIdleTimeExcessConnections", 0);

			settings.put("hibernate.c3p0.acquireRetryAttempts", 10);
			settings.put("hibernate.c3p0.acquireRetryDelay", 5);

			settings.put("hibernate.c3p0.autoCommitOnClose", false);

			// testing connections on checkout is more reliable, but also
			// less performant: https://www.mchange.com/projects/c3p0/#idleConnectionTestPeriod
			// for now, it's ok, as we don't really need high performance...
			settings.put("hibernate.c3p0.testConnectionOnCheckout", true);
			settings.put("hibernate.c3p0.idle_test_period", 120);settings.put("hibernate.c3p0.idle_test_period", 120);
			settings.put("hibernate.c3p0.preferredTestQuery", "SELECT 1");

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

			ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
					.applySettings(configuration.getProperties()).build();

			sessionFactory = configuration.buildSessionFactory(serviceRegistry);
		}

		return sessionFactory;
	}
}
