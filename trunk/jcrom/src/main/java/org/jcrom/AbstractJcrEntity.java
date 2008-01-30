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

/**
 * Abstract implementation of the JcrEntity. Has protected variables
 * for name and path, and implements the getters and setters.
 * 
 * @author Olafur Gauti Gudmundsson
 */
public abstract class AbstractJcrEntity implements JcrEntity {

	protected String name;
	protected String path;
	
	public AbstractJcrEntity() {
	}
	
	public void setName( String name ) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setPath( String path ) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}
}
