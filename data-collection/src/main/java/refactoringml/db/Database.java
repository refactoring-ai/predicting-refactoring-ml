package refactoringml.db;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

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

	public void commit(){
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

	//Simple SQL queries
	//Persist the given object on the DB, if not already exists
	public void persist(Object obj) {
		session.persist(obj);
	}

	//Simple SQL queries
	//Persist the given object on the DB, if not already exists
	public void persistAndCommit(Object obj) {
		session.persist(obj);
		commit();
	}

	//Update the given object in the DB, if it already exists
	public void update(Object obj) {
		session.update(obj);
	}

	//Update the given object in the DB, if it already exists
	public void updateAndCommit(Object obj) {
		session.update(obj);
		commit();
	}

	public Object remove(String key, String tableName) {
		return session.createQuery(String.format("DELETE FROM %s t WHERE s.id = %s", tableName, key)).executeUpdate();
	}

	//Drop the entire table from the SQl database
	//TODO: get the sql truncate to work
	public void truncate(String tableName){
		session.createQuery(String.format("DELETE FROM %s", tableName)).executeUpdate();
	}

	//Map imitation
	//Checks if the given key is assigned to an object in the db
	//Equal to a contains(object) of a Map
	public <T> boolean contains(Object object){
		return session.contains(object);
	}

	//find the object with the given type and key
	//Equal to get(key) of a Map
	//TODO: case sensitive lookup in the database
	public <T> T find(String key, Class<T> type){
		CriteriaBuilder builder = session.getCriteriaBuilder();
		CriteriaQuery<T> criteria = builder.createQuery(type);
		Root<T> root = criteria.from(type);
		criteria.select(root).where(builder.equal(root.get("fileName"), key));

		return session.createQuery(criteria).uniqueResult();
	}

	//Remove the given object from the database
	//Equal to remove(key) of a Map
	public <T> T remove(String key, Class<T> type){
		T object = find(key, type);
		session.delete(object);
		return object;
	}

	//Get all objects of the given type from the DB rom the table
	//Equal to values() of a Map
	public <T> List<T> getAll(String tableName, Class<T> type){
		return session.createQuery(String.format("SELECT o FROM %s o", tableName), type).getResultList();
	}

	public boolean containsTable(String tableName){
		return sf.createEntityManager().getMetamodel().getEntities().contains(tableName);
	}

	//Specific Queries
	public boolean projectExists(String gitUrl) {
		Session shortSession = sf.openSession();
		boolean exists = shortSession.createQuery("from Project p where p.gitUrl = :gitUrl")
				.setParameter("gitUrl", gitUrl)
				.list().size() > 0;
		shortSession.close();

		return exists;
	}

	public String mapToString(){
		return "";
	}
}