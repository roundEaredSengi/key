// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.taclettranslation;

public class IllegalTacletException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public IllegalTacletException(String msg) {
	super(msg);
    }
}