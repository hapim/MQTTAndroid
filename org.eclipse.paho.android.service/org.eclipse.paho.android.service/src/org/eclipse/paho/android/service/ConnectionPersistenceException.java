/*
 * Licensed Materials - Property of IBM
 *
 * 5747-SM3
 *
 * (C) Copyright IBM Corp. 1999, 2012 All Rights Reserved.
 *
 * US Government Users Restricted Rights - Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with
 * IBM Corp.
 *
 */
package org.eclipse.paho.android.service;

/**
 * Persistence Exception, defines an error with persisting a
 * {@link ModelConnectionPersistence} fails. Example operations are
 * {@link DatabaseConnectionPersistence#persistConnection(ModelConnectionPersistence)}
 * and
 * {@link DatabaseConnectionPersistence#restoreConnections(android.content.Context)}
 * ; these operations throw this exception to indicate unexpected results
 * occurred when performing actions on the database.
 * 
 */
public class ConnectionPersistenceException extends Exception {

	/**
	 * Creates a persistence exception with the given error message
	 * 
	 * @param message
	 *            The error message to display
	 */
	public ConnectionPersistenceException(String message) {
		super(message);
	}

	/** Serialisation ID **/
	private static final long serialVersionUID = 5326458803268855071L;

}
