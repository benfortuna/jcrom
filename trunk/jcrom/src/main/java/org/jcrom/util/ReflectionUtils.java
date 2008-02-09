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
package org.jcrom.util;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Various reflection utility methods, used mainly in the Mapper.
 * 
 * @author Olafur Gauti Gudmundsson
 */
public class ReflectionUtils {
	
	/**
	 * Get an array of all fields declared in the supplied class,
	 * and all its superclasses (except java.lang.Object).
	 * 
	 * @param type the class for which we want to retrieve the Fields
	 * @return an array of all declared and inherited fields
	 */
	public static Field[] getDeclaredAndInheritedFields( Class type ) {
		List<Field> allFields = new ArrayList<Field>();
		allFields.addAll(Arrays.asList(type.getDeclaredFields()));
		Class parent = type.getSuperclass();
		while ( parent != null && parent != Object.class ) {
			allFields.addAll(Arrays.asList(parent.getDeclaredFields()));
			parent = parent.getSuperclass();
		}
		return allFields.toArray(new Field[allFields.size()]);
	}

	/**
	 * Check if a class implements a specific interface.
	 * 
	 * @param type the class we want to check
	 * @param interfaceClass the interface class we want to check against
	 * @return true if type implements interfaceClass, else false
	 */
	public static boolean implementsInterface( Class type, Class interfaceClass ) {
		if ( type.isInterface() ) {
			return type == interfaceClass;
		}
		
		for ( Class ifc : type.getInterfaces() ) {
			if ( ifc == interfaceClass ) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check if a class extends a specific class.
	 * 
	 * @param type the class we want to check
	 * @param superClass the super class we want to check against
	 * @return true if type implements superClass, else false
	 */
	public static boolean extendsClass( Class type, Class superClass ) {
		if ( type == superClass ) {
			return true;
		}
		
		Class c = type.getSuperclass();
		while ( c != null && c != Object.class ) {
			if ( c == superClass ) {
				return true;
			}
			c = c.getSuperclass();
		}
		return false;
	}
	
	/**
	 * Check if the class supplied represents a valid JCR property type.
	 * 
	 * @param type the class we want to check
	 * @return true if the class represents a valid JCR property type
	 */
	public static boolean isPropertyType( Class type ) {
		return type == String.class
				|| type == Date.class || type == Calendar.class || type == Timestamp.class
				|| type == InputStream.class || (type.isArray() && type.getComponentType() == byte.class)
				|| type == Integer.class || type == int.class
				|| type == Long.class || type == long.class
				|| type == Double.class || type == double.class
				|| type == Boolean.class || type == boolean.class
				;
	}
	
	public static boolean isDateType( Class type ) {
		return type == Date.class || type == Calendar.class || type == Timestamp.class;
	}
	
	/**
	 * Get the (first) class that parameterizes the Field supplied.
	 * 
	 * @param field the field
	 * @return the class that parameterizes the field, or null if field is
	 * not parameterized
	 */
	public static Class getParameterizedClass( Field field ) {
		if ( field.getGenericType() instanceof ParameterizedType ) {
			ParameterizedType ptype = (ParameterizedType) field.getGenericType();
			return (Class) ptype.getActualTypeArguments()[0];
		}
		return null;
	}
	
	/**
	 * Check if a field is parameterized with a specific class.
	 * 
	 * @param field the field
	 * @param c the class to check against
	 * @return true if the field is parameterized and c is the class that 
	 * parameterizes the field, or is an interface that the parameterized class
	 * implements, else false
	 */
	public static boolean isFieldParameterizedWithClass( Field field, Class c ) {
		if ( field.getGenericType() instanceof ParameterizedType ) {
			ParameterizedType ptype = (ParameterizedType) field.getGenericType();
			for ( Type type : ptype.getActualTypeArguments() ) {				
				if ( type == c ) {
					return true;
				}
				if ( c.isInterface() && implementsInterface((Class)type, c) ) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Check if the field supplied is parameterized with a valid JCR
	 * property type.
	 * 
	 * @param field the field
	 * @return true if the field is parameterized with a valid JCR property
	 * type, else false
	 */
	public static boolean isFieldParameterizedWithPropertyType( Field field ) {
		if ( field.getGenericType() instanceof ParameterizedType ) {
			ParameterizedType ptype = (ParameterizedType) field.getGenericType();
			for ( Type type : ptype.getActualTypeArguments() ) {
				if ( isPropertyType((Class)type) ) {
					return true;
				}
			}
		}
		return false;
	}
}
