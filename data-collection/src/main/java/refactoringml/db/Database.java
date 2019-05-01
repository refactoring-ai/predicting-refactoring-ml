package refactoringml.db;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class Database {

	private SessionFactory sf;
	private Session session;

	private static final Logger log = Logger.getLogger(Database.class);

	public Database(SessionFactory sf) {
		this.sf = sf;
	}

	public void openSession() {
		this.session = sf.openSession();
		session.beginTransaction();
	}

	public void commit() {
		this.session.getTransaction().commit();
		close();
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
			log.error("error when closing the connection", e);
		} finally {
			this.session = null;
		}
	}

	public Yes findYes(Long yesId) {
		return session.get(Yes.class, yesId);
	}

	public boolean projectExists(String gitUrl) {
		Session shortSession = sf.openSession();
		boolean exists = shortSession.createQuery("from Project p where p.gitUrl = :gitUrl")
				.setParameter("gitUrl", gitUrl)
				.list().size() > 0;
		shortSession.close();

		return exists;
	}

	public void cleanProject(Project project) {

		long countYes = (long) session.createSQLQuery("select count(*) from yes where project = :project and (authorOwnership is null or variableAppearances < 0)")
				.setParameter("project", project)
				.uniqueResult();

		session.createQuery("delete from Yes where project = :project and (authorOwnership is null or variableAppearances < 0)")
				.setParameter("project", project)
				.executeUpdate();

		long countNo = (long) session.createSQLQuery("select count(*) from no where project = :project and (authorOwnership is null or variableAppearances < 0)")
				.setParameter("project", project)
				.uniqueResult();

		session.createQuery("delete from No where project = :project and (authorOwnership is null or variableAppearances < 0)")
				.setParameter("project", project)
				.executeUpdate();

		project.setCleanedRows((int) countYes + (int) countNo);

	}

	public void rollback() {
		session.getTransaction().rollback();
	}
}
