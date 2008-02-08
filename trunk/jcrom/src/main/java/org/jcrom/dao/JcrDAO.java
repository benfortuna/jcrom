/**
 * Copyright (C) Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jcrom.dao;

import java.util.List;
import javax.jcr.Node;

/**
 * The base interface for JCR Data Access Objects.
 * Normally, the interface will be implemented for each root object being
 * mapped to a Java Content Repository (JCR).
 * It is intended that such implementations will manage the JCR root path
 * for the entity being mapped. JCR session strategy is also to be 
 * declared by implementations.
 * 
 * @author Olafur Gauti Gudmundsson
 */
public interface JcrDAO<T> {
	
	/**
	 * Creates a new JCR Node from the object supplied.
	 * 
	 * @param entity the object to be mapped to a JCR node
	 * @return the newly created Node
	 * @throws java.lang.Exception
	 */
	public Node create( T entity ) throws Exception;
	
	/**
	 * Updates an existing JCR Node with the values extracted
	 * from the object supplied.
	 * 
	 * @param entity the object to be mapped to a JCR node
	 * @return the name of the JCR Node that was updated
	 * @throws java.lang.Exception
	 */
	public String update( T entity ) throws Exception;
	
	/**
	 * Updates an existing JCR Node with the values extracted
	 * from the object supplied. Use this method if the entity name
	 * has changed.
	 * 
	 * @param entity the object to be mapped to a JCR node
	 * @param oldName the old name of the entity (this will be used to load the
	 * node and move it to the new name)
	 * @return
	 * @throws java.lang.Exception
	 */
	public String update( T entity, String oldName ) throws Exception;
	
	/**
	 * Updates an existing JCR Node with the values extracted
	 * from the object supplied.
	 * 
	 * @param entity the object to be mapped to a JCR node
	 * @param childNameFilter comma separated list of names of child nodes to 
	 * load ("*" loads all, "none" loads no children, and "-" at the beginning
	 * makes it an exclusion filter)
	 * @param maxDepth the maximum depth of loaded child nodes (0 means no 
	 * child nodes are loaded, while a negative value means that no 
	 * restrictions are set on the depth).
	 * @return the name of the JCR Node that was updated
	 * @throws java.lang.Exception
	 */
	public String update( T entity, String childNameFilter, int maxDepth ) throws Exception;
	
	/**
	 * Updates an existing JCR Node with the values extracted
	 * from the object supplied. Use this method if the entity name
	 * has changed.
	 * 
	 * @param entity the object to be mapped to a JCR node
	 * @param oldName the old name of the entity (this will be used to load the
	 * node and move it to the new name)
	 * @param childNameFilter comma separated list of names of child nodes to 
	 * load ("*" loads all, "none" loads no children, and "-" at the beginning
	 * makes it an exclusion filter)
	 * @param maxDepth the maximum depth of loaded child nodes (0 means no 
	 * child nodes are loaded, while a negative value means that no 
	 * restrictions are set on the depth).
	 * @return the name of the JCR Node that was updated
	 * @throws java.lang.Exception
	 */
	public String update( T entity, String oldName, String childNameFilter, int maxDepth ) throws Exception;
	
	/**
	 * Updates an existing JCR Node with the values extracted
	 * from the object supplied. The node to update is loaded using the 
	 * full path supplied (from a @JcrPath field).
	 * 
	 * @param entity the object to be mapped to a JCR node
	 * @param path the full path of the node to update
	 * @return the name of the node that was updated
	 * @throws java.lang.Exception
	 */
	public String updateByPath( T entity, String path ) throws Exception;
	
	/**
	 * Updates an existing JCR Node with the values extracted
	 * from the object supplied. The node to update is loaded using the 
	 * full path supplied (from a @JcrPath field).
	 * 
	 * @param entity the object to be mapped to a JCR node
	 * @param path the full path of the node to update
	 * @param childNameFilter comma separated list of names of child nodes to 
	 * load ("*" loads all, "none" loads no children, and "-" at the beginning
	 * makes it an exclusion filter)
	 * @param maxDepth the maximum depth of loaded child nodes (0 means no 
	 * child nodes are loaded, while a negative value means that no 
	 * restrictions are set on the depth).
	 * @return the name of the node that was updated
	 * @throws java.lang.Exception
	 */
	public String updateByPath( T entity, String path, String childNodeFilter, int maxDepth ) throws Exception;
	
	/**
	 * Updates an existing JCR Node with the values extracted
	 * from the object supplied. The node to update is loaded using the 
	 * UUID supplied.
	 * 
	 * @param entity the object to be mapped to a JCR node
	 * @param uuid the JCR UUID of the node to update
	 * @return the name of the node that was updated
	 * @throws java.lang.Exception
	 */
	public String updateByUUID( T entity, String uuid ) throws Exception;
	
	/**
	 * Updates an existing JCR Node with the values extracted
	 * from the object supplied. The node to update is loaded using the
	 * UUID supplied.
	 * 
	 * @param entity the object to be mapped to a JCR node
	 * @param uuid the UUID of the node to update
	 * @param childNameFilter comma separated list of names of child nodes to 
	 * load ("*" loads all, "none" loads no children, and "-" at the beginning
	 * makes it an exclusion filter)
	 * @param maxDepth the maximum depth of loaded child nodes (0 means no 
	 * child nodes are loaded, while a negative value means that no 
	 * restrictions are set on the depth).
	 * @return the name of the JCR Node that was updated
	 * @throws java.lang.Exception
	 */
	public String updateByUUID( T entity, String uuid, String childNameFilter, int maxDepth ) throws Exception;
	
	/**
	 * Permanently delete the entity with the name supplied.
	 * 
	 * @param name the name of the entity
	 * @throws java.lang.Exception
	 */
	public void delete( String name ) throws Exception;
		
	public void deleteByPath( String path ) throws Exception;
	
	/**
	 * Permanently delete the entity with the UUID supplied.
	 * 
	 * @param uuid the JCR UUID of the entity
	 * @throws java.lang.Exception
	 */
	public void deleteByUUID( String uuid ) throws Exception;

	/**
	 * Check whether an entity with the name supplied exists in JCR.
	 * 
	 * @param name the name of the entity
	 * @return true if the entity exists, else false
	 * @throws java.lang.Exception
	 */
	public boolean exists( String name ) throws Exception;
	
	/**
	 * Get an entity from JCR.
	 * 
	 * @param name the name of the entity to be loaded
	 * @return an object instance mapped from the JCR node with the name
	 * supplied, or null if no such node was found
	 * @throws java.lang.Exception
	 */
	public T get( String name ) throws Exception;
	
	/**
	 * Get an entity from JCR.
	 * 
	 * @param name the name of the entity to be loaded
	 * @param childNameFilter comma separated list of names of child nodes to 
	 * load ("*" loads all, "none" loads no children, and "-" at the beginning
	 * makes it an exclusion filter)
	 * @param maxDepth the maximum depth of loaded child nodes (0 means no 
	 * child nodes are loaded, while a negative value means that no 
	 * restrictions are set on the depth).
	 * @return an object instance mapped from the JCR node with the name
	 * supplied, or null if no such node was found
	 * @throws java.lang.Exception
	 */
	public T get( String name, String childNameFilter, int maxDepth ) throws Exception;
	
	public T getByPath( String path ) throws Exception;
	public T getByPath( String path, String childNodeFilter, int maxDepth ) throws Exception;
	
	/**
	 * Load an entity from JCR by UUID lookup.
	 * 
	 * @param uuid the UUID generated by JCR
	 * @return an object instance mapped from the JCR node with the uuid
	 * @throws java.lang.Exception if no such node is found, or other things go wrong
	 */
	public T loadByUUID( String uuid ) throws Exception;
	
	/**
	 * Load an entity from JCR by UUID lookup.
	 * 
	 * @param uuid the UUID generated by JCR
	 * @return an object instance mapped from the JCR node with the uuid
	 * @throws java.lang.Exception if no such node is found, or other things go wrong
	 */
	public T loadByUUID( String uuid, String childNodeFilter, int maxDepth ) throws Exception;
	
	/**
	 * Find all entities represented by this DAO.
	 * 
	 * @return all entities represented by this DAO
	 * @throws java.lang.Exception
	 */
	public List<T> findAll() throws Exception;
	
	/**
	 * Find all entities represented by this DAO.
	 * Takes parameters that control the size and offset of the result.
	 * 
	 * @param startIndex the zero based index of the first item to return
	 * @param resultSize the number of items to return
	 * @return all entities represented by this DAO
	 * @throws java.lang.Exception
	 */
	public List<T> findAll( long startIndex, long resultSize ) throws Exception;
	
	/**
	 * Find all entities represented by this DAO.
	 * 
	 * @param childNameFilter comma separated list of names of child nodes to 
	 * load ("*" loads all, "none" loads no children, and "-" at the beginning
	 * makes it an exclusion filter)
	 * @param maxDepth the maximum depth of loaded child nodes (0 means no 
	 * child nodes are loaded, while a negative value means that no 
	 * restrictions are set on the depth).
	 * @return all entities represented by this DAO
	 * @throws java.lang.Exception
	 */
	public List<T> findAll( String childNameFilter, int maxDepth ) throws Exception;
	
	/**
	 * Find all entities represented by this DAO.
	 * Takes parameters that control the size and offset of the result, and
	 * filter which child nodes to load.
	 * 
	 * @param childNameFilter comma separated list of names of child nodes to 
	 * load ("*" loads all, "none" loads no children, and "-" at the beginning
	 * makes it an exclusion filter)
	 * @param maxDepth the maximum depth of loaded child nodes (0 means no 
	 * child nodes are loaded, while a negative value means that no 
	 * restrictions are set on the depth).
	 * @param startIndex the zero based index of the first item to return
	 * @param resultSize the number of items to return
	 * @return all entities represented by this DAO
	 * @throws java.lang.Exception
	 */
	public List<T> findAll( String childNameFilter, int maxDepth, long startIndex, long resultSize ) throws Exception;

}
