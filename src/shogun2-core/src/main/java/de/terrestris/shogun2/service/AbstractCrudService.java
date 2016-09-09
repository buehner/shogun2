package de.terrestris.shogun2.service;

import java.io.IOException;
import java.util.List;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.terrestris.shogun2.dao.GenericHibernateDao;
import de.terrestris.shogun2.helper.IdHelper;
import de.terrestris.shogun2.model.PersistentObject;

/**
 * This abstract service class provides basic CRUD functionality.
 *
 * @author Nils Bühner
 * @see AbstractDaoService
 *
 */
public abstract class AbstractCrudService<E extends PersistentObject, D extends GenericHibernateDao<E, Integer>>
		extends AbstractDaoService<E, D> {

	/**
	 * Constructor that sets the concrete entity class for the service.
	 * Subclasses MUST call this constructor.
	 */
	protected AbstractCrudService(Class<E> entityClass) {
		super(entityClass);
	}

	/**
	 *
	 * @param e
	 * @return
	 */
	@PreAuthorize("hasRole(@configHolder.getSuperAdminRoleName())"
			+ " or (#e.id == null and hasPermission(#e, 'CREATE'))"
			+ " or (#e.id != null and hasPermission(#e, 'UPDATE'))")
	public void saveOrUpdate(E e) {
		dao.saveOrUpdate(e);
	}

	/**
	 * @param jsonObject
	 * @param entity
	 * @return
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
	@PreAuthorize("hasRole(@configHolder.getSuperAdminRoleName()) or hasPermission(#entity, 'UPDATE')")
	public E updatePartialWithJsonNode(E entity, JsonNode jsonObject, ObjectMapper objectMapper) throws IOException, JsonProcessingException {
		// update "partially". credits go to http://stackoverflow.com/a/15145480
		entity = objectMapper.readerForUpdating(entity).readValue(jsonObject);
		this.saveOrUpdate(entity);
		return entity;
	}

	/**
	 * Return the real object from the database. Returns null if the object does
	 * not exist.
	 *
	 * @param id
	 * @return
	 */
	@PostAuthorize("hasRole(@configHolder.getSuperAdminRoleName()) or hasPermission(returnObject, 'READ')")
	public E findById(Integer id) {
		return dao.findById(id);
	}

	/**
	 * Return a proxy of the object (without hitting the database). This should
	 * only be used if it is assumed that the object really exists and where
	 * non-existence would be an actual error.
	 *
	 * @param id
	 * @return
	 */
	@PostAuthorize("hasRole(@configHolder.getSuperAdminRoleName()) or hasPermission(returnObject, 'READ')")
	public E loadById(int id) {
		return dao.loadById(id);
	}

	/**
	 *
	 * @return
	 */
	@PostFilter("hasRole(@configHolder.getSuperAdminRoleName()) or hasPermission(filterObject, 'READ')")
	public List<E> findAll() {
		return dao.findAll();
	}

	/**
	 *
	 * @param e
	 */
	@PreAuthorize("hasRole(@configHolder.getSuperAdminRoleName()) or hasPermission(#e, 'DELETE')")
	public void delete(E e) {
		dao.delete(e);
	}

	/**
	 * Clones an entity by detaching it from the hibernate session and resetting the ID to null.
	 * The clone will be persisted as a new entity if persist is true.
	 *
	 * @param e The entity to clone
	 * @param persist whether or not the clone should be persisted as a new entity
	 *
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 */
	@PreAuthorize("hasRole(@configHolder.getSuperAdminRoleName()) or hasPermission(#e, 'READ')")
	public E cloneEntity(E e, boolean persist) throws NoSuchFieldException, IllegalAccessException {
		dao.evict(e);

		IdHelper.setIdOnPersistentObject(e, null);

		if(persist) {
			dao.saveOrUpdate(e);
		}

		return e;
	}
}
