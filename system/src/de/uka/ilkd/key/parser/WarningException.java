// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.parser;

public class WarningException extends antlr.ANTLRException {

  private String errorStr="";
  
  public WarningException(String errorStr) {
    this.errorStr=errorStr;
  }
  
  public String getMessage() {
    return errorStr;
  }
  
  
  public String toString() {
    return errorStr;
  }
  
}