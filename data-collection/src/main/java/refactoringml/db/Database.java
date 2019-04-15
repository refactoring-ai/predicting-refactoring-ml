package refactoringml.db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class Database {

	private SessionFactory sf;
	private Session session;

	public Database(SessionFactory sf) {
		this.sf = sf;
	}

	public void openSession() {
		this.session = sf.openSession();
		session.beginTransaction();
	}

	public void commit() {
		this.session.getTransaction().commit();
		this.session.close();
	}

	public void persist(Object obj) {
		session.persist(obj);
	}

	public void persistProcessMetric(Yes yes) {
		session.update(yes);

	}
}
