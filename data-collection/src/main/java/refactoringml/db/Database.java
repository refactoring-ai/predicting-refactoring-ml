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

	public long findAllRefactoringCommits(Project project) {
		return findAllInstances("refactoringcommit", project.getId());
	}

	public long findAllStableCommits(Project project) {
		return findAllInstances("stablecommit", project.getId());
	}

	private long findAllInstances(String instance, long projectId){
		Session shortSession = sf.openSession();
		String query = "Select count(*) From " + instance + " where " + instance + ".project_id = " + projectId;
		Object result = shortSession.createSQLQuery(query).getSingleResult();
		shortSession.close();
		return Long.parseLong(result.toString());
	}

	public long findAllStableCommits(Project project, int level) {
		Session shortSession = sf.openSession();
		String query = "Select count(*) From stablecommit where stablecommit.project_id = " + project.getId() + " AND stablecommit.commitThreshold = " + level;
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