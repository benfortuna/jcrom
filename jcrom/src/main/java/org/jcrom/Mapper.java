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
package org.jcrom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.jcrom.annotations.JcrChildNode;
import org.jcrom.annotations.JcrCreated;
import org.jcrom.annotations.JcrFileNode;
import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrParentNode;
import org.jcrom.annotations.JcrProperty;
import org.jcrom.annotations.JcrUUID;
import org.jcrom.util.NameFilter;
import org.jcrom.util.PathUtils;
import org.jcrom.util.ReflectionUtils;

/**
 * This class handles the heavy lifting of mapping a JCR node
 * to a JCR entity object, and vice versa.
 * 
 * @author Olafur Gauti Gudmundsson
 */
class Mapper {
	
	/** The Class that this instance maps to/from */
	private final Class entityClass;
	
	/**
	 * Create a Mapper for a specific class.
	 * 
	 * @param entityClass the class that we will me mapping to/from
	 */
	Mapper( Class entityClass ) {
		this.entityClass = entityClass;
	}
	
	Class getEntityClass() {
		return entityClass;
	}
	
	private Field findNameField( Object obj ) {
		for ( Field field : ReflectionUtils.getDeclaredAndInheritedFields(obj.getClass()) ) {
			if ( field.isAnnotationPresent(JcrName.class) ) {
				return field;
			}
		}
		return null;
	}
	
	String getNodeName( Object object ) throws Exception {
		return (String) findNameField(object).get(object);
	}
	
	private void setNodeName( Object object, String name ) throws Exception {
		findNameField(object).set(object, name);
	}

	/**
	 * Transforms the node supplied to an instance of the entity class
	 * that this Mapper was created for.
	 * 
	 * @param node the JCR node from which to create the object
	 * @param childNodeFilter comma separated list of names of child nodes to load 
	 * ("*" loads all, while "none" loads no children)
	 * @param maxDepth the maximum depth of loaded child nodes (0 means no child nodes are loaded,
	 * while a negative value means that no restrictions are set on the depth).
	 * @return an instance of the JCR entity class, mapped from the node
	 * @throws java.lang.Exception
	 */
	Object fromNode( Node node, String childNodeFilter, int maxDepth ) throws Exception {
		Object obj = entityClass.newInstance();
		mapNodeToClass(obj, entityClass, node, new NameFilter(childNodeFilter), maxDepth, null, 0);
		return obj;
	}
	
	/**
	 * 
	 * @param node
	 * @param entity
	 * @param childNodeFilter
	 * @param maxDepth
	 * @throws java.lang.Exception
	 */
	String updateNode( Node node, Object entity, String childNodeFilter, int maxDepth ) throws Exception {
		if ( entity.getClass() != entityClass ) {
			throw new JcrMappingException("This mapper was constructed for [" + entityClass.getName() + "] and cannot handle [" + entity.getClass().getName() + "]");
		}
		
		return updateNode(node, entity, entityClass, new NameFilter(childNodeFilter), maxDepth, 0);
	}
	
	private Object findEntityByName( List entities, String name ) throws Exception {
		for ( int i = 0; i < entities.size(); i++ ) {
			Object entity = (Object) entities.get(i);
			if ( PathUtils.createValidName(getNodeName(entity)).equals(name) ) {
				return entity;
			}
		}
		return null;
	}
	
	private String updateNode( Node node, Object obj, Class objClass, NameFilter childNameFilter, int maxDepth, int depth ) throws Exception {
		
		// if name is different, then we move the node
		if ( !node.getName().equals(PathUtils.createValidName(getNodeName(obj))) ) {
			node.getSession().move(node.getPath(), node.getParent().getPath() + "/" + PathUtils.createValidName(getNodeName(obj)));
		}
		
		for ( Field field : ReflectionUtils.getDeclaredAndInheritedFields(objClass) ) {
			field.setAccessible(true);
		
			if ( field.isAnnotationPresent(JcrProperty.class) ) {
				mapFieldToProperty(field, obj, node);
				
			} else if ( field.isAnnotationPresent(JcrChildNode.class) 
					&& ( maxDepth < 0 || depth < maxDepth ) ) {
				JcrChildNode jcrChildNode = field.getAnnotation(JcrChildNode.class);
				String name = field.getName();
				if ( !jcrChildNode.name().equals("fieldName") ) {
					name = jcrChildNode.name();
				}
				
				// make sure that this child is supposed to be updated
				if ( childNameFilter.isIncluded(name) ) {
					if ( ReflectionUtils.implementsInterface(field.getType(), List.class) ) {
						// can expect multiple children
						boolean nullOrEmpty = field.get(obj) == null || ((List)field.get(obj)).isEmpty();
						if ( !nullOrEmpty ) {
							Class paramClass = ReflectionUtils.getParameterizedClass(field);
							List children = (List)field.get(obj);
							if ( node.hasNode(name) ) {
								// children exist, we must update
								Node childContainer = node.getNode(name);
								NodeIterator childNodes = childContainer.getNodes();
								while ( childNodes.hasNext() ) {
									Node child = childNodes.nextNode();
									Object childEntity = findEntityByName(children, child.getName());
									if ( childEntity == null ) {
										// this child was not found, so we remove it
										child.remove();
									} else {
										updateNode(child, childEntity, paramClass, childNameFilter, maxDepth, depth+1);
									}
								}
								// we must add new children, if any
								for ( int i = 0; i < children.size(); i++ ) {
									Object child = children.get(i);
									if ( !childContainer.hasNode(PathUtils.createValidName(getNodeName(child))) ) {
										addNode(childContainer, child, paramClass, null);
									}
								}
							} else {
								// no children exist, we add
								Node childContainer = node.addNode(PathUtils.createValidName(name));
								for ( int i = 0; i < children.size(); i++ ) {
									addNode(childContainer, children.get(i), paramClass, null);
								}
							}
						} else {
							// field list is now null (or empty), so we remove the child nodes
							NodeIterator nodeIterator = node.getNodes(name);
							while ( nodeIterator.hasNext() ) {
								nodeIterator.nextNode().remove();
							}
						}
					} else {
						// single child
						if ( !node.hasNode(name) ) {
							if ( field.get(obj) != null ) {
								// add the node if it does not exist
								Node childContainer = node.addNode(PathUtils.createValidName(name));
								addNode(childContainer, field.get(obj), field.getType(), null);
							}
						} else {
							if ( field.get(obj) != null ) {
								updateNode(node.getNode(name).getNodes().nextNode(), field.get(obj), field.getType(), childNameFilter, maxDepth, depth+1);
							} else {
								// field is now null, so we remove the child node
								NodeIterator nodeIterator = node.getNodes(name);
								while ( nodeIterator.hasNext() ) {
									nodeIterator.nextNode().remove();
								}
							}
						}
					}
				}
				
			} else if ( field.isAnnotationPresent(JcrFileNode.class) 
					&& ( maxDepth < 0 || depth < maxDepth ) ) {
				
				JcrFileNode jcrFileNode = field.getAnnotation(JcrFileNode.class);
				String name = field.getName();
				if ( !jcrFileNode.name().equals("fieldName") ) {
					name = jcrFileNode.name();
				}
				
				// make sure that this child is supposed to be updated
				if ( childNameFilter.isIncluded(name) ) {
					if ( ReflectionUtils.implementsInterface(field.getType(), List.class) ) {
						// can expect multiple children
						boolean nullOrEmpty = field.get(obj) == null || ((List)field.get(obj)).isEmpty();
						if ( !nullOrEmpty ) {
							Class paramClass = ReflectionUtils.getParameterizedClass(field);
							List children = (List)field.get(obj);
							if ( node.hasNode(name) ) {
								// children exist, we must update
								Node childContainer = node.getNode(name);
								NodeIterator childNodes = childContainer.getNodes();
								while ( childNodes.hasNext() ) {
									Node child = childNodes.nextNode();
									JcrFile childEntity = (JcrFile)findEntityByName(children, child.getName());
									if ( childEntity == null ) {
										// this child was not found, so we remove it
										child.remove();
									} else {
										updateFileNode(child, childEntity, childNameFilter, maxDepth, depth);
									}
								}
								// we must add new children, if any
								for ( int i = 0; i < children.size(); i++ ) {
									Object child = children.get(i);
									if ( !childContainer.hasNode(PathUtils.createValidName(getNodeName(child))) ) {
										addNode(childContainer, child, paramClass, null);
									}
								}
							} else {
								// no children exist, we add
								JcrNode jcrNode = getJcrNodeAnnotation(ReflectionUtils.getParameterizedClass(field));
								Node fileContainer = createFileFolderNode(jcrNode, name, node);
								for ( int i = 0; i < children.size(); i++ ) {
									addFileNode(jcrNode, fileContainer, (JcrFile)children.get(i));
								}
							}
						} else {
							// field list is now null (or empty), so we remove the child nodes
							NodeIterator nodeIterator = node.getNodes(name);
							while ( nodeIterator.hasNext() ) {
								nodeIterator.nextNode().remove();
							}
						}
					} else {
						// single child
						if ( !node.hasNode(name) ) {
							if ( field.get(obj) != null ) {
								JcrNode jcrNode = getJcrNodeAnnotation(field.getType());
								Node fileContainer = createFileFolderNode(jcrNode, name, node);
								addFileNode(jcrNode, fileContainer, (JcrFile)field.get(obj));
							}
						} else {
							if ( field.get(obj) != null ) {
								updateFileNode(node.getNode(name).getNodes().nextNode(), (JcrFile)field.get(obj), childNameFilter, maxDepth, depth);
							} else {
								// field is now null, so we remove the child node
								NodeIterator nodeIterator = node.getNodes(name);
								while ( nodeIterator.hasNext() ) {
									nodeIterator.nextNode().remove();
								}
							}
						}
					}
				}
			}
		}
		return node.getName();
	}
	
	private JcrNode getJcrNodeAnnotation( Class c ) throws Exception {
		
		if ( c.isAnnotationPresent(JcrNode.class) ) {
			return (JcrNode) c.getAnnotation(JcrNode.class);
		} else {
			// need to check all superclasses
			Class parent = c.getSuperclass();
			while ( parent != null && parent != Object.class ) {
				if ( parent.isAnnotationPresent(JcrNode.class) ) {
					return (JcrNode) parent.getAnnotation(JcrNode.class);
				}
				parent = parent.getSuperclass();
			}
			
			// ...and all implemented interfaces
			for ( Class interfaceClass : c.getInterfaces() ) {
				if ( interfaceClass.isAnnotationPresent(JcrNode.class) ) {
					return (JcrNode) interfaceClass.getAnnotation(JcrNode.class);
				}
			}
		}
		// no annotation found, use the defaults
		return null;
	}
	
	private Node createFileFolderNode( JcrNode jcrNode, String nodeName, Node parentNode ) throws Exception {
		if ( jcrNode != null && jcrNode.nodeType().equals("nt:unstructured") ) {
			return parentNode.addNode(PathUtils.createValidName(nodeName));
		} else {
			// assume it is an nt:file or extension of that, 
			// so we create an nt:folder
			return parentNode.addNode(PathUtils.createValidName(nodeName), "nt:folder");
		}
	}
	
	/**
	 * 
	 * @param parentNode
	 * @param entity
	 * @throws java.lang.Exception
	 */
	Node addNode( Node parentNode, Object entity, String[] mixinTypes ) throws Exception {
		if ( entity.getClass() != entityClass ) {
			throw new JcrMappingException("This mapper was constructed for [" + entityClass.getName() + "] and cannot handle [" + entity.getClass().getName() + "]");
		}
		return addNode(parentNode, entity, entityClass, mixinTypes);
	}
	
	private Node addNode( Node parentNode, Object entity, Class objClass, String[] mixinTypes ) throws Exception {
		return addNode(parentNode, entity, objClass, mixinTypes, true);
	}
	
	private Node addNode( Node parentNode, Object entity, Class objClass, String[] mixinTypes, boolean createNode ) throws Exception {
		
		// create the child node
		Node node = null;
		if ( createNode ) {
			// check if we should use a specific node type
			JcrNode jcrNode = getJcrNodeAnnotation(objClass);
			if ( jcrNode == null || jcrNode.nodeType().equals("nt:unstructured") ) {
				node = parentNode.addNode(PathUtils.createValidName(getNodeName(entity)));
			} else {
				node = parentNode.addNode(PathUtils.createValidName(getNodeName(entity)), jcrNode.nodeType());
			}
			// add the mixin types
			if ( mixinTypes != null ) {
				for ( String mixinType : mixinTypes ) {
					if ( node.canAddMixin(mixinType) ) {
						node.addMixin(mixinType);
					}
				}
			}
		} else {
			node = parentNode;
		}
		
		for ( Field field : ReflectionUtils.getDeclaredAndInheritedFields(objClass) ) {
			field.setAccessible(true);
			
			if ( field.isAnnotationPresent(JcrProperty.class) ) {
				mapFieldToProperty(field, entity, node);
				
			} else if ( field.isAnnotationPresent(JcrChildNode.class) ) {
				JcrChildNode jcrChildNode = field.getAnnotation(JcrChildNode.class);
				String name = field.getName();
				if ( !jcrChildNode.name().equals("fieldName") ) {
					name = jcrChildNode.name();
				}
				
				// make sure that the field value is not null
				if ( field.get(entity) != null ) {
					if ( ReflectionUtils.implementsInterface(field.getType(), List.class) ) {
						// we can expect more than one child object here
						List children = (List)field.get(entity);
						if ( !children.isEmpty() ) {
							Node childContainer = node.addNode(PathUtils.createValidName(name));
							for ( int i = 0; i < children.size(); i++ ) {
								addNode(childContainer, children.get(i), ReflectionUtils.getParameterizedClass(field), null);
							}
						}
					} else {
						Node childContainer = node.addNode(PathUtils.createValidName(name));
						addNode(childContainer, field.get(entity), field.getType(), null);
					}
				}
				
			} else if ( field.isAnnotationPresent(JcrFileNode.class) ) {
				JcrFileNode jcrFileNode = field.getAnnotation(JcrFileNode.class);
				String name = field.getName();
				if ( !jcrFileNode.name().equals("fieldName") ) {
					name = jcrFileNode.name();
				}
				
				if ( field.get(entity) != null ) {
					if ( ReflectionUtils.implementsInterface(field.getType(), List.class) ) {
						// we can expect more than one child object here
						List children = (List)field.get(entity);
						if ( !children.isEmpty() ) {
							JcrNode jcrNode = getJcrNodeAnnotation(ReflectionUtils.getParameterizedClass(field));
							Node fileContainer = createFileFolderNode(jcrNode, name, node);
							for ( int i = 0; i < children.size(); i++ ) {
								addFileNode(jcrNode, fileContainer, (JcrFile)children.get(i));
							}
						}
					} else {
						JcrNode jcrNode = getJcrNodeAnnotation(field.getType());
						Node fileContainer = createFileFolderNode(jcrNode, name, node);
						addFileNode(jcrNode, fileContainer, (JcrFile)field.get(entity));
					}
				}
			}
		}
		return node;
	}
	
	
	private <T extends JcrFile> void updateFileNode( Node fileNode, T file, NameFilter childNameFilter, int maxDepth, int depth ) throws Exception {
		Node contentNode = fileNode.getNode("jcr:content");
		setFileNodeProperties(contentNode, file);
	
		updateNode(fileNode, file, file.getClass(), childNameFilter, maxDepth, depth+1);
	}
	
	private <T extends JcrFile> void addFileNode( JcrNode jcrNode, Node parentNode, T file ) throws Exception {
		Node fileNode = null;
		if ( jcrNode == null || jcrNode.nodeType().equals("nt:unstructured") ) {
			fileNode = parentNode.addNode(PathUtils.createValidName(file.getName()));
		} else {
			fileNode = parentNode.addNode(PathUtils.createValidName(file.getName()), jcrNode.nodeType());
		}
		Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
		setFileNodeProperties(contentNode, file);
		
		addNode(fileNode, file, file.getClass(), null, false);
	}
	
	private <T extends JcrFile> void setFileNodeProperties( Node contentNode, T file ) throws Exception {
		contentNode.setProperty("jcr:mimeType", file.getMimeType());
		contentNode.setProperty("jcr:lastModified", file.getLastModified());
		if ( file.getEncoding() != null ) {
			contentNode.setProperty("jcr:encoding", file.getEncoding());
		}
		
		// add the file data
		JcrDataProvider dataProvider = file.getDataProvider();
		if ( dataProvider != null ) {
			if ( dataProvider.getType() == JcrDataProvider.TYPE.FILE && dataProvider.getFile() != null ) {
				contentNode.setProperty("jcr:data", new FileInputStream(dataProvider.getFile()));
			} else if ( dataProvider.getType() == JcrDataProvider.TYPE.BYTES && dataProvider.getBytes() != null ) {
				contentNode.setProperty("jcr:data", new ByteArrayInputStream(dataProvider.getBytes()));
			} else if ( dataProvider.getType() == JcrDataProvider.TYPE.STREAM && dataProvider.getInputStream() != null ) {
				contentNode.setProperty("jcr:data", dataProvider.getInputStream());
			}
		}
	}
	
	void mapFieldToProperty( Field field, Object obj, Node node ) throws Exception {
		JcrProperty jcrProperty = field.getAnnotation(JcrProperty.class);
		String name = field.getName();
		if ( !jcrProperty.name().equals("fieldName") ) {
			name = jcrProperty.name();
		}
		
		// make sure that the field value is not null
		if ( field.get(obj) != null ) {
		
			ValueFactory valueFactory = node.getSession().getValueFactory();

			if ( ReflectionUtils.implementsInterface(field.getType(), List.class) ) {
				// multi-value property
				Class paramClass = ReflectionUtils.getParameterizedClass(field);
				List fieldValues = (List)field.get(obj);
				if ( !fieldValues.isEmpty() ) {
					Value[] values = new Value[fieldValues.size()];
					for ( int i = 0; i < fieldValues.size(); i++ ) {
						values[i] = createValue(paramClass, fieldValues.get(i), valueFactory);
					}
					node.setProperty(name, values);
				}

			} else {
				// single-value property
				Value value = createValue(field.getType(), field.get(obj), valueFactory);
				if ( value != null ) {
					node.setProperty(name, value);
				}
			}
		} else {
			// remove the value
			node.setProperty(name, (Value)null);
		}
	}
		
	private void mapNodeToClass( Object obj, Class objClass, Node node, NameFilter nameFilter, int maxDepth, Object parentObject, int depth ) throws Exception {
		
		if ( !ReflectionUtils.extendsClass(objClass, JcrFile.class) ) {
			// this does not apply for JcrFile extensions
			setNodeName(obj, node.getName());
		}
		
		for ( Field field : ReflectionUtils.getDeclaredAndInheritedFields(objClass) ) {
			field.setAccessible(true);
			
			if ( field.isAnnotationPresent(JcrProperty.class) ) {
				mapPropertyToField(obj, field, node);
				
			} else if ( field.isAnnotationPresent(JcrUUID.class) ) {
				if ( node.hasProperty("jcr:uuid") ) {
					field.set(obj, node.getUUID());
				}
				
			} else if ( field.isAnnotationPresent(JcrCreated.class) ) {
				if ( node.hasProperty("jcr:created") ) {
					field.set(obj, getValue(field.getType(), node.getProperty("jcr:created").getValue()));
				}
			
			} else if ( field.isAnnotationPresent(JcrParentNode.class) ) {
				if ( parentObject != null ) {
					field.set(obj, parentObject);
				}
				
			} else if ( field.isAnnotationPresent(JcrChildNode.class) 
					&& ( maxDepth < 0 || depth < maxDepth ) ) {
				JcrChildNode jcrChildNode = field.getAnnotation(JcrChildNode.class);
				String name = field.getName();
				if ( !jcrChildNode.name().equals("fieldName") ) {
					name = jcrChildNode.name();
				}
				
				if ( node.hasNode(name) && nameFilter.isIncluded(name) ) {
					// child nodes are always stored inside a container node
					Node childrenContainer = node.getNode(name);
					if ( ReflectionUtils.implementsInterface(field.getType(), List.class) ) {
						// we can expect more than one child object here
						List children = new ArrayList();
						ParameterizedType ptype = (ParameterizedType) field.getGenericType();
						
						NodeIterator iterator = childrenContainer.getNodes();
						while ( iterator.hasNext() ) {
							Class childObjClass = (Class)ptype.getActualTypeArguments()[0];
							Object childObj = childObjClass.newInstance();
							mapNodeToClass(childObj, childObjClass, iterator.nextNode(), nameFilter, maxDepth, obj, depth+1);
							children.add(childObj);
						}
						field.set(obj, children);
						
					} else {
						// instantiate the field class
						Object childObj = field.getType().newInstance();
						mapNodeToClass(childObj, field.getType(), childrenContainer.getNodes().nextNode(), nameFilter, maxDepth, obj, depth+1);
						field.set(obj, childObj);
					}
				}
				
			} else if ( field.isAnnotationPresent(JcrFileNode.class) 
					&& ( maxDepth < 0 || depth < maxDepth ) ) {
				JcrFileNode jcrFileNode = field.getAnnotation(JcrFileNode.class);
				String name = field.getName();
				if ( !jcrFileNode.name().equals("fieldName") ) {
					name = jcrFileNode.name();
				}
				
				if ( node.hasNode(name) && nameFilter.isIncluded(name) ) {
					// file nodes are always stored inside a folder node
					Node fileContainer = node.getNode(name);
					if ( ReflectionUtils.implementsInterface(field.getType(), List.class) ) {
						// we can expect more than one child object here
						List children = new ArrayList();
						ParameterizedType ptype = (ParameterizedType) field.getGenericType();
						
						NodeIterator iterator = fileContainer.getNodes();
						while ( iterator.hasNext() ) {
							Class childObjClass = (Class)ptype.getActualTypeArguments()[0];
							JcrFile fileObj = (JcrFile)childObjClass.newInstance();
							mapNodeToFileObject(jcrFileNode, fileObj, iterator.nextNode(), nameFilter, maxDepth, obj, depth);
							children.add(fileObj);
						}
						field.set(obj, children);
						
					} else {
						// instantiate the field class
						JcrFile fileObj = (JcrFile)field.getType().newInstance();
						mapNodeToFileObject(jcrFileNode, fileObj, fileContainer.getNodes().nextNode(), nameFilter, maxDepth, obj, depth);
						field.set(obj, fileObj);
					}
				}
			
			} else if ( field.getName().equals("path") ) {
				field.set(obj, node.getPath());
			
			}
		}
	}
	
	private <T extends JcrFile> void mapNodeToFileObject( JcrFileNode jcrFileNode, T fileObj, Node fileNode, NameFilter nameFilter, int maxDepth, Object parentObject, int depth ) throws Exception {
		Node contentNode = fileNode.getNode("jcr:content");
		fileObj.setName(fileNode.getName());
		fileObj.setPath(fileNode.getPath());
		fileObj.setMimeType( contentNode.getProperty("jcr:mimeType").getString() );
		fileObj.setLastModified( contentNode.getProperty("jcr:lastModified").getDate() );
		if ( contentNode.hasProperty("jcr:encoding") ) {
			fileObj.setEncoding(contentNode.getProperty("jcr:encoding").getString());
		}
		
		// file data
		if ( jcrFileNode.loadType() == JcrFileNode.LoadType.BYTES ) {
			JcrDataProviderImpl dataProvider = new JcrDataProviderImpl(JcrDataProvider.TYPE.BYTES, readBytes(contentNode.getProperty("jcr:data").getStream()));
			fileObj.setDataProvider(dataProvider);
		} else if ( jcrFileNode.loadType() == JcrFileNode.LoadType.STREAM ) {
			JcrDataProviderImpl dataProvider = new JcrDataProviderImpl(JcrDataProvider.TYPE.STREAM, contentNode.getProperty("jcr:data").getStream());
			fileObj.setDataProvider(dataProvider);
		}
		
		// if this is a JcrFile subclass, it may contain custom properties and 
		// child nodes that need to be mapped
		mapNodeToClass(fileObj, fileObj.getClass(), fileNode, nameFilter, maxDepth, parentObject, depth+1);
	}
	
	private void mapPropertyToField( Object obj, Field field, Node node ) throws Exception {
		JcrProperty jcrProperty = field.getAnnotation(JcrProperty.class);
		String name = field.getName();
		if ( !jcrProperty.name().equals("fieldName") ) {
			name = jcrProperty.name();
		}
		if ( node.hasProperty(name) ) {
			Property p = node.getProperty(name);
			
			if ( ReflectionUtils.implementsInterface(field.getType(), List.class) ) {
				// multi-value property
				List properties = new ArrayList();
				ParameterizedType ptype = (ParameterizedType) field.getGenericType();
				for ( Value value : p.getValues() ) {
					properties.add(getValue((Class)ptype.getActualTypeArguments()[0], value));
				}
				field.set(obj, properties);
			} else {
				// single-value property
				field.set(obj, getValue(field.getType(), p.getValue()));
			}
		}
	}
	
	Value createValue( Class c, Object fieldValue, ValueFactory valueFactory ) throws Exception {		
		if ( c == String.class ) {
			return valueFactory.createValue((String) fieldValue);
		} else if ( c == Date.class ) {
			Calendar cal = Calendar.getInstance();
			cal.setTime((Date)fieldValue);
			return valueFactory.createValue(cal);
		} else if ( c == Timestamp.class ) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(((Timestamp)fieldValue).getTime());
			return valueFactory.createValue(cal);
		} else if ( c == Calendar.class ) {
			return valueFactory.createValue((Calendar) fieldValue);
		} else if ( c == InputStream.class ) {
			return valueFactory.createValue((InputStream)fieldValue);
		} else if ( c.isArray() && c.getComponentType() == byte.class ) {
			return valueFactory.createValue(new ByteArrayInputStream((byte[])fieldValue));
		} else if ( c == Integer.class || c == int.class ) {
			return valueFactory.createValue((Integer)fieldValue);
		} else if ( c == Long.class || c == long.class ) {
			return valueFactory.createValue((Long)fieldValue);
		} else if ( c == Double.class || c == double.class ) {
			return valueFactory.createValue((Double)fieldValue);
		} else if ( c == Float.class || c == float.class ) {
			return valueFactory.createValue((Float)fieldValue);
		} else if ( c == Boolean.class || c == boolean.class ) {
			return valueFactory.createValue((Boolean)fieldValue);
		}
		return null;
	}
	
	private Object getValue( Class c, Value value ) throws Exception {
		if ( c == String.class ) {
			return value.getString();
		} else if ( c == Date.class ) {
			return value.getDate().getTime();
		} else if ( c == Timestamp.class ) {
			return new Timestamp(value.getDate().getTimeInMillis());
		} else if ( c == Calendar.class ) {
			return value.getDate();
		} else if ( c == InputStream.class ) {
			return value.getStream();
		} else if ( c.isArray() && c.getComponentType() == byte.class ) {
			// byte array...we need to read from the stream
			return readBytes(value.getStream());
		} else if ( c == Integer.class || c == int.class ) {
			return (int) value.getDouble();
		} else if ( c == Long.class || c == long.class ) {
			return value.getLong();
		} else if ( c == Double.class || c == double.class ) {
			return value.getDouble(); 
		} else if ( c == Boolean.class || c == boolean.class ) {
			return value.getBoolean();
		}
		return null;
	}
	
	private byte[] readBytes( InputStream in ) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			in.close();
	        out.close();
		}
		return out.toByteArray();
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !(obj instanceof Mapper) ) {
			return false;
		}
		Mapper other = (Mapper) obj;
		
		return other.entityClass == entityClass;
	}

	@Override
	public int hashCode() {
		return entityClass.hashCode();
	}
	
	
}