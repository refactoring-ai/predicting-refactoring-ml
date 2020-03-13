package refactoringml.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.TransactionException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import java.sql.Connection;
import java.sql.SQLException;

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

	//shutdown the session factory and all connections
	public void shutdown(){
		close();
		sf.close();
		sf = null;
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

	//Handles all the logic to update an object to the database
	public void updateComplete(Object obj){
		openSession();
		update(obj);
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

	//safely rollback a transaction with the db
	public void rollback() {
		//this session object itself should never be null, thus we don't check for it
		//nothing to do in this case, recovering the session or transaction is to much effort and the db takes care of a failed transaction
		if(!session.isOpen()) {
			log.error("Session was already closed during attempted rollback: Doing Nothing.");
			return;
		}

		if(!session.isConnected()){
			try{
				Connection connection =	sf.getSessionFactoryOptions().getServiceRegistry().
						getService(ConnectionProvider.class).getConnection();
				session.reconnect(connection);
			} catch (SQLException e) {
				log.error("Failed to reconnect session object.", e);
			}
		}

		//standard case for a rollback
		if(session.isConnected() && session.getTransaction() != null) {
			try{
				session.getTransaction().rollback();
				return;
			} catch (TransactionException e) {
				log.error("Failed to rollback session: " + session.toString(), e);
			}
		}
		//other cases:
		//1. not connected to the DB : we could raise an error here, because something is probably wrong with the db
		//2. connected but no transaction object : nothing to do
		log.error("Session is in a bad state: " + session.toString());
	}

	public CommitMetaData loadCommitMetaData(long id) {
		return session.get(CommitMetaData.class, id);
	}
}