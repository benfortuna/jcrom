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

import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import org.jcrom.JcrMappingException;
import org.jcrom.Jcrom;
import org.jcrom.util.PathUtils;

/**
 * An abstract implementation of the JcrDAO interface. This should be extended
 * for specific entity implementations.
 * This class implements all the methods defined in the JcrDAO interface, and
 * provides a few protected methods that are useful for implementing custom
 * finder methods.
 * <br/><br/>
 * 
 * The constructor takes a JCR session, so an instance should be created per
 * session. The constructor also takes a Jcrom instance that can be shared
 * across multiple DAOs.
 * <br/><br/>
 *
 * @author Olafur Gauti Gudmundsson
 */
public abstract class AbstractJcrDAO<T> implements JcrDAO<T> {

	protected final String rootPath;
	protected final Jcrom jcrom;
	protected final Session session;
	protected final Class<T> entityClass;
	protected final String[] mixinTypes;
	protected final boolean isVersionable;
	
	/**
	 * Constructor.
	 * 
	 * @param entityClass the class handled by this DAO implementation
	 * @param rootPath the JCR root path under which entities should be created
	 * @param session the current JCR session
	 * @param jcrom the Jcrom instance to use for object mapping
	 */
	public AbstractJcrDAO( Class<T> entityClass, String rootPath, Session session, Jcrom jcrom ) {
		this(entityClass, rootPath, session, jcrom, new String[0]);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param entityClass the class handled by this DAO implementation
	 * @param rootPath the JCR root path under which entities should be created
	 * @param session the current JCR session
	 * @param jcrom the Jcrom instance to use for object mapping
	 * @param mixinTypes an array of mixin types to apply to new nodes
	 */
	public AbstractJcrDAO( Class<T> entityClass, String rootPath, Session session, Jcrom jcrom, String[] mixinTypes ) {
		this.entityClass = entityClass;
		this.rootPath = rootPath;
		this.session = session;
		this.jcrom = jcrom;
		this.mixinTypes = mixinTypes;
		this.isVersionable = isVersionable(mixinTypes);
	}
	
	private boolean isVersionable( String[] mixinTypes ) {
		for ( String mixinType : mixinTypes ) {
			if ( mixinType.equals("mix:versionable") ) {
				return true;
			}
		}
		return false;
	}

	private String fullPath( String name ) {
		String validName = PathUtils.createValidName(name);
		if ( rootPath == null || rootPath.equals("") ) {
			return "/" + validName;
		} else if ( !rootPath.endsWith("/") ) {
			return rootPath + "/" + validName;
		} else {
			return rootPath + validName;
		}
	}
	
	 private String relativePath( String absolutePath ) {
		 if ( absolutePath.startsWith("/") ) {
			 return absolutePath.substring(1);
		 } else {
			 return absolutePath;
		 }
	 }
	
	public Node create( T entity ) throws Exception {
		String entityName = jcrom.getName(entity);
		if ( entityName == null || entityName.equals("") ) {
			throw new JcrMappingException("The name of the entity being created is empty!");
		}
		Node parentNode = session.getRootNode().getNode(rootPath);
		Node newNode = jcrom.addNode(parentNode, entity, mixinTypes);
		session.save();
		if ( isVersionable ) {
			newNode.checkin();
		}
		return newNode;
	}
	
	public String update( T entity ) throws Exception {
		return update(entity, "*", -1);
	}
	
	public String update( T entity, String childNodeFilter, int maxDepth ) throws Exception {
		Node node = session.getRootNode().getNode(fullPath(jcrom.getName(entity)));
		return update(node, entity, childNodeFilter, maxDepth);
	}
	
	public String update( T entity, String oldName ) throws Exception {
		return update(entity, oldName, "*", -1);
	}
	
	public String update( T entity, String oldName, String childNodeFilter, int maxDepth ) throws Exception {
		Node node = session.getRootNode().getNode(fullPath(oldName));
		return update(node, entity, childNodeFilter, maxDepth);
	}

	public String updateByPath( T entity, String path ) throws Exception {
		return updateByPath(entity, path, "*", -1);
	}
	
	public String updateByPath( T entity, String path, String childNodeFilter, int maxDepth ) throws Exception {
		Node node = session.getRootNode().getNode(relativePath(path));
		return update(node, entity, childNodeFilter, maxDepth);
	}
	
	public String updateByUUID( T entity, String uuid ) throws Exception {
		return updateByUUID(entity, uuid, "*", -1);
	}
	
	public String updateByUUID( T entity, String uuid, String childNodeFilter, int maxDepth ) throws Exception {
		Node node = session.getNodeByUUID(uuid);
		return update(node, entity, childNodeFilter, maxDepth);
	}
	
	protected String update( Node node, T entity, String childNodeFilter, int maxDepth ) throws Exception {
		if ( isVersionable ) {
			node.checkout();
		}
		String name = jcrom.updateNode(node, entity, childNodeFilter, maxDepth);
		session.save();
		if ( isVersionable ) {
			node.checkin();
		}
		return name;
	}
	
	public void delete( String name ) throws Exception {
		session.getRootNode().getNode(fullPath(name)).remove();
		session.save();
	}
	
	public void deleteByPath( String path ) throws Exception {
		session.getRootNode().getNode(relativePath(path)).remove();
		session.save();
	}
	
	public void deleteByUUID( String uuid ) throws Exception {
		session.getNodeByUUID(uuid).remove();
		session.save();
	}
	
	public boolean exists( String name ) throws Exception {
		return session.getRootNode().hasNode(fullPath(name));
	}
	
	public T get( String name ) throws Exception {
		return get(name, "*", -1);
	}
	
	public T get( String name, String childNodeFilter, int maxDepth ) throws Exception {
		if ( exists(name) ) {
			Node node = session.getRootNode().getNode(fullPath(name));
			return (T)jcrom.fromNode(entityClass, node, childNodeFilter, maxDepth);
		} else {
			return null;
		}
	}
	
	public T getByPath( String path ) throws Exception {
		return getByPath(path, "*", -1);
	}
	
	public T getByPath( String path, String childNodeFilter, int maxDepth ) throws Exception {
		Node rootNode = session.getRootNode();
		if ( rootNode.hasNode(relativePath(path)) ) {
			Node node = rootNode.getNode(relativePath(path));
			return (T)jcrom.fromNode(entityClass, node, childNodeFilter, maxDepth);
		} else {
			return null;
		}
	}
	
	public T loadByUUID( String uuid ) throws Exception {
		return loadByUUID(uuid, "*", -1);
	}
	
	public T loadByUUID( String uuid, String childNodeFilter, int maxDepth ) throws Exception {
		Node node = session.getNodeByUUID(uuid);
		return (T)jcrom.fromNode(entityClass, node, childNodeFilter, maxDepth);
	}
	
	public T getVersion( String name, String versionName ) throws Exception {
		return getVersion(name, versionName, "*", -1);
	}
	public T getVersion( String name, String versionName, String childNodeFilter, int maxDepth ) throws Exception {
		return getVersion(session.getRootNode().getNode(fullPath(name)), versionName, childNodeFilter, maxDepth);
	}
	
	public T getVersionByPath( String path, String versionName ) throws Exception {
		return getVersionByPath(path, versionName, "*", -1);
	}
	public T getVersionByPath( String path, String versionName, String childNodeFilter, int maxDepth ) throws Exception {
		return getVersion(session.getRootNode().getNode(relativePath(path)), versionName, childNodeFilter, maxDepth);
	}
	
	public T getVersionByUUID( String uuid, String versionName ) throws Exception {
		return getVersionByUUID(uuid, versionName, "*", -1);
	}
	public T getVersionByUUID( String uuid, String versionName, String childNodeFilter, int maxDepth ) throws Exception {
		return getVersion(session.getNodeByUUID(uuid), versionName, childNodeFilter, maxDepth);
	}
	
	protected T getVersion( Node node, String versionName, String childNodeFilter, int maxDepth ) throws Exception {
		VersionHistory versionHistory = node.getVersionHistory();
		Version version = versionHistory.getVersion(versionName);
		return (T)jcrom.fromNode(entityClass, version.getNodes().nextNode(), childNodeFilter, maxDepth);
	}
	
	public void restoreVersion( String name, String versionName ) throws Exception {
		restoreVersion(session.getRootNode().getNode(fullPath(name)), versionName);
	}
	public void restoreVersionByPath( String path, String versionName ) throws Exception {
		restoreVersion(session.getRootNode().getNode(relativePath(path)), versionName);
	}
	public void restoreVersionByUUID( String uuid, String versionName ) throws Exception {
		restoreVersion(session.getNodeByUUID(uuid), versionName);
	}
	protected void restoreVersion( Node node, String versionName ) throws Exception {
		node.checkout();
		node.restore(versionName, true);
	}
	
	public void removeVersion( String name, String versionName ) throws Exception {
		removeVersion(session.getRootNode().getNode(fullPath(name)), versionName);
	}
	public void removeVersionByPath( String path, String versionName ) throws Exception {
		removeVersion(session.getRootNode().getNode(relativePath(path)), versionName);
	}
	public void removeVersionByUUID( String uuid, String versionName ) throws Exception {
		removeVersion(session.getNodeByUUID(uuid), versionName);
	}
	protected void removeVersion( Node node, String versionName ) throws Exception {
		node.getVersionHistory().removeVersion(versionName);
	}
	
	public long getVersionSize( String name ) throws Exception {
		return getVersionSize(session.getRootNode().getNode(fullPath(name)));
	}
	
	public long getVersionSizeByPath( String path ) throws Exception {
		return getVersionSize(session.getRootNode().getNode(relativePath(path)));
	}
	
	public long getVersionSizeByUUID( String uuid ) throws Exception {
		return getVersionSize(session.getNodeByUUID(uuid));
	}
	
	protected long getVersionSize( Node node ) throws Exception {
		VersionHistory versionHistory = node.getVersionHistory();
		return versionHistory.getAllVersions().getSize()-1;
	}
	
	public List<T> getVersionList( String name ) throws Exception {
		return getVersionList(session.getRootNode().getNode(fullPath(name)), "*", -1);
	}
	
	public List<T> getVersionList( String name, String childNameFilter, int maxDepth ) throws Exception {
		return getVersionList(session.getRootNode().getNode(fullPath(name)), childNameFilter, maxDepth);
	}
	
	public List<T> getVersionList( String name, String childNameFilter, int maxDepth, long startIndex, long resultSize ) throws Exception {
		return getVersionList(session.getRootNode().getNode(fullPath(name)), childNameFilter, maxDepth, startIndex, resultSize);
	}
	
	public List<T> getVersionListByPath( String path ) throws Exception {
		return getVersionList(session.getRootNode().getNode(relativePath(path)), "*", -1);
	}
	
	public List<T> getVersionListByPath( String path, String childNameFilter, int maxDepth ) throws Exception {
		return getVersionList(session.getRootNode().getNode(relativePath(path)), childNameFilter, maxDepth);
	}
	
	public List<T> getVersionListByPath( String path, String childNameFilter, int maxDepth, long startIndex, long resultSize ) throws Exception {
		return getVersionList(session.getRootNode().getNode(relativePath(path)), childNameFilter, maxDepth, startIndex, resultSize);
	}
	
	public List<T> getVersionListByUUID( String uuid ) throws Exception {
		return getVersionList(session.getNodeByUUID(uuid), "*", -1);
	}
	
	public List<T> getVersionListByUUID( String uuid, String childNameFilter, int maxDepth ) throws Exception {
		return getVersionList(session.getNodeByUUID(uuid), childNameFilter, maxDepth);
	}
	
	public List<T> getVersionListByUUID( String uuid, String childNameFilter, int maxDepth, long startIndex, long resultSize ) throws Exception {
		return getVersionList(session.getNodeByUUID(uuid), childNameFilter, maxDepth, startIndex, resultSize);
	}
	
	protected List<T> getVersionList( Node node, String childNameFilter, int maxDepth ) throws Exception {
		List<T> versionList = new ArrayList<T>();
		VersionHistory versionHistory = node.getVersionHistory();
		VersionIterator versionIterator = versionHistory.getAllVersions();
		versionIterator.skip(1);
		while ( versionIterator.hasNext() ) {
			Version version = versionIterator.nextVersion();
			NodeIterator nodeIterator = version.getNodes();
			while ( nodeIterator.hasNext() ) {
				versionList.add((T)jcrom.fromNode(entityClass, nodeIterator.nextNode(), childNameFilter, maxDepth));
			}
		}
		return versionList;
	}
	
	protected List<T> getVersionList( Node node, String childNameFilter, int maxDepth, long startIndex, long resultSize ) throws Exception {
		List<T> versionList = new ArrayList<T>();
		VersionHistory versionHistory = node.getVersionHistory();
		VersionIterator versionIterator = versionHistory.getAllVersions();
		versionIterator.skip(1 + startIndex);
		
		long counter = 0;
		while ( versionIterator.hasNext() ) {
			if ( counter == resultSize ) {
				break;
			}
			Version version = versionIterator.nextVersion();
			NodeIterator nodeIterator = version.getNodes();
			while ( nodeIterator.hasNext() ) {
				versionList.add((T)jcrom.fromNode(entityClass, nodeIterator.nextNode(), childNameFilter, maxDepth));
			}
			counter++;
		}
		return versionList;
	}
	
	
	public long getSize() throws Exception {
		NodeIterator nodeIterator = session.getRootNode().getNode(rootPath).getNodes();
		return nodeIterator.getSize();
	}
	
	public List<T> findAll() throws Exception {
		return findAll("*", -1);
	}
	
	public List<T> findAll( long startIndex, long resultSize ) throws Exception {
		return findAll("*", -1, startIndex, resultSize);
	}
	
	public List<T> findAll( String childNameFilter, int maxDepth ) throws Exception {
		return toList(session.getRootNode().getNode(rootPath).getNodes(), childNameFilter, maxDepth);
	}
	
	public List<T> findAll( String childNameFilter, int maxDepth, long startIndex, long resultSize ) throws Exception {
		NodeIterator nodeIterator = session.getRootNode().getNode(rootPath).getNodes();
		nodeIterator.skip(startIndex);
		return toList(nodeIterator, childNameFilter, maxDepth, resultSize);
	}
	
	
	/**
	 * Find JCR nodes that match the xpath supplied, and map to objects.
	 * 
	 * @param xpath the XPath for finding the nodes
	 * @param childNameFilter comma separated list of names of child nodes to 
	 * load ("*" loads all, "none" loads no children, and "-" at the beginning
	 * makes it an exclusion filter)
	 * @param maxDepth the maximum depth of loaded child nodes (0 means no 
	 * child nodes are loaded, while a negative value means that no 
	 * restrictions are set on the depth).
	 * @param startIndex the zero based index of the first item to return
	 * @param resultSize the number of items to return
	 * @return a list of all objects found
	 * @throws java.lang.Exception
	 */
	protected List<T> findByXPath( String xpath, String childNameFilter, int maxDepth, long startIndex, long resultSize ) throws Exception {
		QueryManager queryManager = session.getWorkspace().getQueryManager();
		Query query = queryManager.createQuery(xpath, Query.XPATH);
		QueryResult result = query.execute();
		NodeIterator nodeIterator = result.getNodes();
		nodeIterator.skip(startIndex);
		return toList(nodeIterator, childNameFilter, maxDepth, resultSize);
	}
	
	/**
	 * Find JCR nodes that match the xpath supplied, and map to objects.
	 * 
	 * @param xpath the XPath for finding the nodes
	 * @param childNameFilter comma separated list of names of child nodes to 
	 * load ("*" loads all, "none" loads no children, and "-" at the beginning
	 * makes it an exclusion filter)
	 * @param maxDepth the maximum depth of loaded child nodes (0 means no 
	 * child nodes are loaded, while a negative value means that no 
	 * restrictions are set on the depth).
	 * @return a list of all objects found
	 * @throws java.lang.Exception
	 */
	protected List<T> findByXPath( String xpath, String childNameFilter, int maxDepth ) throws Exception {
		QueryManager queryManager = session.getWorkspace().getQueryManager();
		Query query = queryManager.createQuery(xpath, Query.XPATH);
		QueryResult result = query.execute();
		return toList(result.getNodes(), childNameFilter, maxDepth);
	}
	
	/**
	 * Maps JCR nodes to a List of JcrEntity implementations.
	 * 
	 * @param nodeIterator the iterator pointing to the nodes
	 * @param childNameFilter comma separated list of names of child nodes to 
	 * load ("*" loads all, "none" loads no children, and "-" at the beginning
	 * makes it an exclusion filter)
	 * @param maxDepth the maximum depth of loaded child nodes (0 means no 
	 * child nodes are loaded, while a negative value means that no 
	 * restrictions are set on the depth).
	 * @return a list of objects mapped from the nodes
	 * @throws java.lang.Exception
	 */
	protected List<T> toList( NodeIterator nodeIterator, String childNameFilter, int maxDepth ) throws Exception {
		List<T> objects = new ArrayList<T>();
		while ( nodeIterator.hasNext() ) {
			objects.add( (T)jcrom.fromNode(entityClass, nodeIterator.nextNode(), childNameFilter, maxDepth) );
		}
		return objects;
	}
	
	/**
	 * Maps JCR nodes to a List of JcrEntity implementations.
	 * 
	 * @param nodeIterator the iterator pointing to the nodes
	 * @param childNameFilter comma separated list of names of child nodes to 
	 * load ("*" loads all, "none" loads no children, and "-" at the beginning
	 * makes it an exclusion filter)
	 * @param maxDepth the maximum depth of loaded child nodes (0 means no 
	 * child nodes are loaded, while a negative value means that no 
	 * restrictions are set on the depth).
	 * @param resultSize the number of items to retrieve from the iterator
	 * @return a list of objects mapped from the nodes
	 * @throws java.lang.Exception
	 */
	protected List<T> toList( NodeIterator nodeIterator, String childNameFilter, int maxDepth, long resultSize ) throws Exception {
		List<T> objects = new ArrayList<T>();
		long counter = 0;
		while ( nodeIterator.hasNext() ) {
			if ( counter == resultSize ) {
				break;
			}
			objects.add( (T)jcrom.fromNode(entityClass, nodeIterator.nextNode(), childNameFilter, maxDepth) );
			counter++;
		}
		return objects;
	}
}