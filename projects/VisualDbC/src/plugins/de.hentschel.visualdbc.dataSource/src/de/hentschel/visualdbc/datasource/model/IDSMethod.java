/*******************************************************************************
 * Copyright (c) 2011 Martin Hentschel.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Martin Hentschel - initial API and implementation
 *******************************************************************************/

package de.hentschel.visualdbc.datasource.model;

import de.hentschel.visualdbc.datasource.model.exception.DSException;

/**
 * Represents a method on the data source.
 * @author Martin Hentschel
 */
public interface IDSMethod extends IDSOperation {
   /**
    * Returns the return type.
    * @return The return type.
    * @throws DSException Occurred Exception
    */
   public String getReturnType() throws DSException;
   
   /**
    * Checks if it is abstract.
    * @return Is abstract?
    * @throws DSException Occurred Exception
    */
   public boolean isAbstract() throws DSException;
   
   /**
    * Checks if it is final.
    * @return Is final?
    * @throws DSException Occurred Exception
    */
   public boolean isFinal() throws DSException;
}