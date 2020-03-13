package refactoringml.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class Database {
	private SessionFactory sf;
	private Session session;

	private static final Logger log = LogManager.getLogger(Database.class);

	public Database(SessionFactory sf) {
		this.sf = sf;
	}

	public void openSession() {
		this.session = sf.openSession();
		session.beginTransaction();
	}

	public void commit() {
		this.session.getTransaction().commit();
	}

	public void persist(Object obj) {
		session.persist(obj);
	}

	//Handles all the logic to persist an object to the database
	public void persistComplete(Object obj){
		openSession();
		persist(obj);
		commit();
		close();
	}

	public void update(Object obj) {
		session.update(obj);
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

	public boolean projectExists(String gitUrl) {
		Session shortSession = sf.openSession();
		boolean exists = shortSession.createQuery("from Project p where p.gitUrl = :gitUrl")
				.setParameter("gitUrl", gitUrl)
				.list().size() > 0;
		shortSession.close();

		return exists;
	}

	public long findAllRefactoringCommits(long projectId) { return findAllInstances("RefactoringCommit", projectId); }

	public long findAllStableCommits(long projectId) { return findAllInstances("StableCommit", projectId); }

	private long findAllInstances(String instance, long projectId){
		Session shortSession = sf.openSession();
		String query = "Select count(*) From " + instance + " where " + instance + ".project_id = " + projectId;
		Object result = shortSession.createSQLQuery(query).getSingleResult();
		shortSession.close();
		return Long.parseLong(result.toString());
	}

	public long findAllStableCommits(long projectId, int level) {
		Session shortSession = sf.openSession();
		String query = "Select count(*) From StableCommit where StableCommit.project_id = " + projectId + " AND StableCommit.commitThreshold = " + level;
		Object result = shortSession.createSQLQuery(query).getSingleResult();
		shortSession.close();
		return Long.parseLong(result.toString());
	}

	public void rollback() {
		session.getTransaction().rollback();
	}

	public CommitMetaData loadCommitMetaData(long id) {
		return session.get(CommitMetaData.class, id);
	}
}