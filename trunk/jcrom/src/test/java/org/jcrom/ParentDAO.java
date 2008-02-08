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

import java.util.List;
import javax.jcr.Session;
import org.jcrom.dao.AbstractJcrDAO;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public class ParentDAO extends AbstractJcrDAO<Parent> {

	private static final String[] MIXIN_TYPES = {"mix:referenceable"};
	
	public ParentDAO( Session session, Jcrom jcrom ) {
		super(Parent.class, "content/parents", session, jcrom, true, MIXIN_TYPES);
	}
	
	public List<Parent> findByLicense() throws Exception {
		return super.findByXPath("/jcr:root/content/parents/*[@drivingLicense = 'true']", "*", -1);
	}
}
