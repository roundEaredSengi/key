// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.speclang.jml.pretranslation;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.speclang.PositionedString;

/**
 * A JML model method declaration in textual form.
 */
public final class TextualJMLMethodDecl extends TextualJMLConstruct {
    
    private final PositionedString decl;
    private final String methodName;
    
    
    public TextualJMLMethodDecl(ImmutableList<String> mods, 
                                PositionedString decl, 
                                String methodName) {
        super(mods);
        assert decl != null;
        this.decl = decl;
        this.methodName = methodName;
    }
    
    
    public PositionedString getDecl() {
        return decl;
    }
    
    
    public String getMethodName() {
        return methodName;
    }
    
    
    @Override
    public String toString() {
        return decl.toString();
    }
    
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof TextualJMLMethodDecl)) {
            return false;
        }
        TextualJMLMethodDecl md = (TextualJMLMethodDecl) o;
        return mods.equals(md.mods) 
               && decl.equals(md.decl) 
               && methodName.equals(md.methodName);
    }
    
    
    @Override
    public int hashCode() {
        return mods.hashCode() + decl.hashCode() + methodName.hashCode();
    }
}