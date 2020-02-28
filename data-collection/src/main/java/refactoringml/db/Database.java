package refactoringml.db;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class Database {
	private static final Logger log = Logger.getLogger(Database.class);

	private SessionFactory sf;
	private Session session;

	public Database(SessionFactory sf) {
		this.sf = sf;
	}

	//Session interaction
	public void openSession() {
		this.session = sf.openSession();
		session.beginTransaction();
	}

	public void commit() {
		this.session.getTransaction().commit();
		close();
	}

	public void rollback() {
		session.getTransaction().rollback();
	}

	public void close() {
		try {
			if (session != null)
				session.close();
		} catch(Exception e) {
			// what to do? this really shouldn't happen.
			log.error("Error when closing the connection to the Database: ", e);
		} finally {
			this.session = null;
		}
	}

	//Queries
	//Persist the given object on the DB, if not already exists
	public void persist(Object obj) {
		session.persist(obj);
	}

	//Update the given object in the DB, if it already exists
	public void update(Object obj) {
		session.update(obj);
	}

	//Drop the entire table from the DB
	public void drop(String tableName){

	}

	//find the
	public void find(String key, String tableName){

	}

	//Specific Queries
	public RefactoringCommit findRefactoringCommit(Long refactoringCommitId) {
		return session.get(RefactoringCommit.class, refactoringCommitId);
	}

	public boolean projectExists(String gitUrl) {
		Session shortSession = sf.openSession();
		boolean exists = shortSession.createQuery("from Project p where p.gitUrl = :gitUrl")
				.setParameter("gitUrl", gitUrl)
				.list().size() > 0;
		shortSession.close();

		return exists;
	}
}