package refactoringml.db;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.stream.Collectors;

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

	public List<RefactoringCommit> findAllRefactoringCommits(Project project) {
		Session shortSession = sf.openSession();
		List<RefactoringCommit> results = shortSession.createQuery("From RefactoringCommit where project = :project order by id asc")
				.setParameter("project", project)
				.list();
		shortSession.close();
		return results;
	}

	public List<StableCommit> findAllStableCommits(Project project) {
		Session shortSession = sf.openSession();
		List<StableCommit> results =  shortSession.createQuery("From StableCommit where project = :project order by id asc")
				.setParameter("project", project)
				.list();
		shortSession.close();
		return results;
	}

	public List<StableCommit> findAllStableCommits(Project project, int level) {
		return findAllStableCommits(project).stream().filter(stableCommit -> stableCommit.getCommitThreshold() == level).collect(Collectors.toList());
	}

	public void rollback() {
		session.getTransaction().rollback();
	}

	public CommitMetaData loadCommitMetaData(long id) {
		return session.get(CommitMetaData.class, id);
	}
}